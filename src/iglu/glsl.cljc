(ns iglu.glsl
  (:require [clojure.string :as str]
            [iglu.parse :as parse]))

;; multimethods

(defmulti ->function-call
  (fn [fn-name args]
    (cond
      (number? fn-name) ::number
      (= 'if fn-name) ::conditional
      (= '? fn-name) ::inline-conditional
      ('#{+ - * / < > <= >= == !=} fn-name) ::operator
      (= '= fn-name) ::assignment
      (-> fn-name str (str/starts-with? "=")) ::local-assignment
      (-> fn-name str (str/starts-with? ".")) ::property
      :else fn-name)))

(defmulti ->subexpression
  (fn [val] (first val)))

;; ->function-call

(defmethod ->function-call ::assignment [fn-name args]
  (when-not (= 2 (count args))
    (throw (ex-info ":= requires 2 args" {})))
  (let [[sym val] args]
    (str (->subexpression sym) " = " (->subexpression val))))

(defmethod ->function-call ::local-assignment [fn-name args]
  (when-not (= 2 (count args))
    (throw (ex-info (str fn-name " requires 2 args") {})))
  (let [[sym val] args]
    (str
      (-> fn-name str (subs 1))
      " "
      (->subexpression sym)
      " = "
      (->subexpression val))))

(defmethod ->function-call ::conditional [fn-name args]
  (when-not (< 1 (count args) 4)
    (throw (ex-info (str fn-name " requires 2 or 3 args") {})))
  (let [[condition true-case false-case] args]
    [[(str "if " (->subexpression condition))
      (->subexpression true-case)]
     (when false-case
       ["else"
        (->subexpression false-case)])]))

(defmethod ->function-call ::inline-conditional [fn-name args]
  (when-not (= 3 (count args))
    (throw (ex-info (str fn-name " requires 3 args") {})))
  (let [[condition true-case false-case] args]
    (str
      (->subexpression condition)
      " ? " (->subexpression true-case)
      " : " (->subexpression false-case))))

(defmethod ->function-call ::operator [fn-name args]
  (str/join (str " " fn-name " ") (mapv ->subexpression args)))

(defmethod ->function-call ::property [fn-name args]
  (when (not= (count args) 1)
    (throw (ex-info (str fn-name " requires exactly one arg") {})))
  (str (-> args first ->subexpression) "." (-> fn-name str (subs 1))))

(defmethod ->function-call ::number [fn-name args]
  (when (not= (count args) 1)
    (throw (ex-info (str fn-name " requires exactly one arg") {})))
  (str (->subexpression (first args)) "[" fn-name "]"))

(defmethod ->function-call :default [fn-name args]
  (str fn-name "(" (str/join ", " (mapv ->subexpression args)) ")"))

;; ->expression

(defmethod ->subexpression :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression
        s (->function-call fn-name args)]
    (when-not (string? s)
      (throw (ex-info (str fn-name " can't be used as an expression") {})))
    (str "(" s ")")))

(defmethod ->subexpression :number [[_ number]]
  (str number))

(defmethod ->subexpression :symbol [[_ symbol]]
  (str symbol))

(defmethod ->subexpression :string [[_ string]]
  string)

;; var definitions

(defn ->in [[name type]]
  (str "in " type " " name))

(defn ->uniform [[name type]]
  (str "uniform " type " " name))

(defn ->out [[name type]]
  (when type
    (str "out " type " " name)))

(defn ->function [signatures [name {:keys [args body]}]]
  (if-let [{:keys [in out]} (get signatures name)]
    (let [_ (when (not= (count in) (count args))
              (throw (ex-info (str "The function " name " has args signature "
                                in " of a different length than its args definition "
                                args)
                       {})))
          args-list (str/join ", "
                      (mapv (fn [type name]
                              (str type " " name))
                        in args))
          body-lines (->> body
                          (mapv (fn [{:keys [fn-name args]}]
                                  (->function-call fn-name args))))]
      (into [(str out " " name "(" args-list ")")]
            (if (= 'void out)
              body-lines
              (conj
                (vec (butlast body-lines))
                (str "return " (last body-lines))))))
    (throw (ex-info (str "Nothing found in :signatures for function " name) {}))))

;; compiler fn

(defn indent [level line]
  (str (str/join (repeat (* level 2) " "))
       line))

(defn stringify [level lines line]
  (cond
    (string? line)
    (conj lines
          (if (str/starts-with? line "#")
            line
            (str (indent level line) ";")))
    (string? (first line))
    (-> lines
        (conj (indent level (first line)))
        (conj (indent level "{"))
        (into (reduce (partial stringify (inc level)) [] (rest line)))
        (conj (indent level "}")))
    :else
    (into lines (reduce (partial stringify level) [] line))))

(defn sort-fns [functions fn-deps]
  (->> functions
       seq
       (sort-by first
         (fn [a b]
           (cond
             (contains? (fn-deps a) b) 1
             (contains? (fn-deps b) a) -1
             :else 0)))))

(defn iglu->glsl [shader-type
                  {:keys [version precision
                          attributes uniforms varyings
                          outputs signatures functions fn-deps]
                   :as shader}]
  (->> (cond-> []
               version (conj (str "#version " version))
               precision (conj (str "precision " precision))
               (= shader-type :vertex) (into (mapv ->in attributes))
               uniforms (into (mapv ->uniform uniforms))
               varyings (into (case shader-type
                                :vertex (mapv ->out varyings)
                                :fragment (mapv ->in varyings)))
               outputs (into (mapv ->out outputs))
               functions (into (mapv (partial ->function signatures)
                                 (sort-fns functions fn-deps))))
       (reduce (partial stringify 0) [])
       (str/join \newline)))

