(ns iglu.glsl
  (:require [clojure.string :as str]
            [iglu.parse :as parse]))

;; multimethods

(defmulti ->function-call
  (fn [fn-name args]
    (cond
      (#{:+ :- :* :/ :=} fn-name) ::operator
      (-> fn-name name (str/starts-with? "-")) ::property
      :else fn-name)))

(defmulti ->subexpression
  (fn [val] (first val)))

;; ->function-call

(defmethod ->function-call ::operator [fn-name args]
  (str/join (str " " (name fn-name) " ") (mapv ->subexpression args)))

(defmethod ->function-call ::property [fn-name args]
  (when (> (count args) 1)
    (parse/throw-error (str "Too many arguments given to " fn-name)))
  (str (-> args first ->subexpression) "." (-> fn-name name (subs 1))))

(defmethod ->function-call :default [fn-name args]
  (str (name fn-name) "(" (str/join ", " (mapv ->subexpression args)) ")"))

;; ->expression

(defmethod ->subexpression :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression]
    (str "(" (->function-call fn-name args) ")")))

(defmethod ->subexpression :one-line [[_ line]]
  (let [{:keys [fn-name args]} line]
    [(->function-call fn-name args)]))

(defmethod ->subexpression :multi-line [[_ lines]]
  (mapv (fn [{:keys [fn-name args]}]
          (->function-call fn-name args))
    lines))

(defmethod ->subexpression :number [[_ number]]
  (str number))

(defmethod ->subexpression :symbol [[_ symbol]]
  (str symbol))

;; var definitions

(defn ->in [[name type]]
  (str "in " type " " name ";"))

(defn ->uniform [[name type]]
  (str "uniform " type " " name ";"))

(defn ->out [[name type]]
  (when type
    (str "out " type " " name ";")))

(defn ->function [[name {:keys [ret args body]}]]
  (let [args-list (str/join ", "
                    (mapv (fn [{:keys [type name]}]
                            (str type " " name))
                      args))
        body-lines (mapv #(str % ";") (->subexpression body))]
    [(str ret " " name "(" args-list ")")
     "{"
     (if (= 'void ret)
       body-lines
       (conj
         (vec (butlast body-lines))
         (str "return " (last body-lines))))
     "}"]))

;; compiler fn

(defn indent [level content]
  (reduce
    (fn [v line]
      (conj v
        (if (string? line)
          (str (str/join (repeat (* level 2) " "))
            line)
          (indent (inc level) line))))
    []
    content))

(defn ->glsl [{:keys [type version precision attributes uniforms varyings outputs functions] :as shader}]
  (->> (cond-> []
               version (conj (str "#version " version))
               precision (conj (str "precision " precision ";"))
               (= type :vertex) (into (mapv ->in attributes))
               true (into (mapv ->uniform uniforms))
               true (into (case type
                            :vertex (mapv ->out varyings)
                            :fragment (mapv ->in varyings)))
               true (into (mapv ->out outputs))
               true (into (mapcat ->function functions)))
       (indent 0)
       flatten
       (str/join \newline)))

