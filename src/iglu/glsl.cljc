(ns iglu.glsl
  (:require [clojure.string :as str]
            [iglu.parse :as parse]))

;; multimethods

(defmulti ->function-call
  (fn [fn-name args]
    (cond
      (#{:+ :- :* :/} fn-name) ::operator
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

(defmethod ->subexpression :fn-expression [[_ fn-expression]]
  (let [{:keys [fn-record args]} fn-expression]
    (str (:name fn-record) "(" (str/join ", " (mapv ->subexpression args)) ")")))

(defmethod ->subexpression :attribute [[_ attribute]]
  (str (:name attribute)))

(defmethod ->subexpression :uniform [[_ uniform]]
  (str (:name uniform)))

(defmethod ->subexpression :varying [[_ varying]]
  (str (:name varying)))

(defmethod ->subexpression :number [[_ number]]
  (str number))

(defmethod ->subexpression :symbol [[_ symbol]]
  (str symbol))

;; var definitions

(defn ->in [{:keys [name type]}]
  (str "in " type " " name ";"))

(defn ->uniform [{:keys [name type]}]
  (str "uniform " type " " name ";"))

(defn ->out [{:keys [name type location]}]
  (when type
    (str "out " type " " name ";")))

(defn ->function [[{:keys [name arg-types return-type]} clj-fn]]
  (let [arg-syms (mapv #(symbol (str "arg" %))
                   (range (count arg-types)))
        fn-body (parse/parse-subexpression (apply clj-fn arg-syms))]
    [(str return-type " " name
       "("
       (str/join ", "
         (mapv (fn [arg-type arg-sym]
                 (str arg-type " " arg-sym))
           arg-types arg-syms))
       ")")
     "{"
     (str "  return " (->subexpression fn-body) ";")
     "}"]))

;; compiler fn

(defn ->glsl [{:keys [version precision attributes uniforms varyings dependencies] :as shader}]
  (let [defined-varyings (filterv parse/varying? (keys shader))
        defined-outputs (filterv parse/output? (keys shader))
        defined-functions (filterv parse/function? (keys shader))
        varyings-kv (select-keys shader defined-varyings)
        outputs-kv (select-keys shader defined-outputs)
        functions-kv (select-keys shader defined-functions)
        sorted-outputs (->> (merge outputs-kv varyings-kv)
                            seq
                            (sort-by first
                              (fn [a b]
                                (cond
                                  (contains? (dependencies a) b) 1
                                  (contains? (dependencies b) a) -1
                                  :else 0))))]
    (->> [(when version (str "#version " version))
          (when precision (str "precision " precision ";"))
          (mapv ->in attributes)
          (mapv ->in varyings)
          (mapv ->uniform uniforms)
          (mapv ->out defined-varyings)
          (mapv ->out defined-outputs)
          (mapv ->function functions-kv)
          "void main() {"
          (mapv (fn [[{:keys [name]} subexpression]]
                  (str "  " name " = " (->subexpression subexpression) ";"))
            sorted-outputs)
          "}"]
         flatten
         (remove nil?)
         (str/join \newline))))

