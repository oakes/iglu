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

(defn ->function [[name {:keys [ret args clj-fn]}]]
  (let [arg-syms (mapv #(symbol (str "arg" %))
                   (range (count args)))
        fn-body (parse/parse-subexpression (apply clj-fn arg-syms))]
    [(str ret " " name
       "("
       (str/join ", "
         (mapv (fn [arg-type arg-sym]
                 (str arg-type " " arg-sym))
           args arg-syms))
       ")")
     "{"
     (str "  return " (->subexpression fn-body) ";")
     "}"]))

;; compiler fn

(defn ->glsl [{:keys [type version precision attributes uniforms varyings outputs functions main] :as shader}]
  (->> [(when version (str "#version " version))
        (when precision (str "precision " precision ";"))
        (case type
          :vertex
          [(mapv ->in attributes)
           (mapv ->uniform uniforms)
           (mapv ->out varyings)]
          :fragment
          [(mapv ->in varyings)
           (mapv ->uniform uniforms)])
        (mapv ->out outputs)
        (mapv ->function functions)
        "void main() {"
        (mapv (fn [[name subexpression]]
                (str "  " name " = " (->subexpression subexpression) ";"))
          main)
        "}"]
       flatten
       (remove nil?)
       (str/join \newline)))

