(ns iglu.glsl
  (:require [clojure.string :as str]
            [iglu.parse :as parse]))

;; multimethods

(defmulti ->function
  (fn [fn-name args]
    (cond
      (#{:+ :- :* :/} fn-name) ::operator
      (-> fn-name name (str/starts-with? "-")) ::property
      :else fn-name)))

(defmulti ->subexpression
  (fn [val] (first val)))

;; ->function

(defmethod ->function ::operator [fn-name args]
  (str/join (str " " (name fn-name) " ") (mapv ->subexpression args)))

(defmethod ->function ::property [fn-name args]
  (when (> (count args) 1)
    (parse/throw-error (str "Too many arguments given to " fn-name)))
  (str (-> args first ->subexpression) "." (-> fn-name name (subs 1))))

(defmethod ->function :default [fn-name args]
  (str (name fn-name) "(" (str/join ", " (mapv ->subexpression args)) ")"))

;; ->expression

(defmethod ->subexpression :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression]
    (str "(" (->function fn-name args) ")")))

(defmethod ->subexpression :fn-expression [[_ fn-expression]]
  (let [[func & args] fn-expression]
    (str (:name func) "(" (str/join ", " args) ")")))

(defmethod ->subexpression :attribute [[_ attribute]]
  (str (:name attribute)))

(defmethod ->subexpression :uniform [[_ uniform]]
  (str (:name uniform)))

(defmethod ->subexpression :varying [[_ varying]]
  (str (:name varying)))

(defmethod ->subexpression :number [[_ number]]
  (str number))

;; var definitions

(defn ->in [{:keys [name type]}]
  (str "in " type " " name ";"))

(defn ->uniform [{:keys [name type]}]
  (str "uniform " type " " name ";"))

(defn ->out [{:keys [name type location]}]
  (when type
    (str "out " type " " name ";")))

;; compiler fn

(defn ->glsl [{:keys [version precision attributes uniforms varyings] :as shader}]
  (let [defined-varyings (filterv parse/varying? (keys shader))
        defined-outputs (filterv parse/output? (keys shader))
        varyings-kv (select-keys shader defined-varyings)
        outputs-kv (select-keys shader defined-outputs)]
    (->> [(when version (str "#version " version))
          (when precision (str "precision " precision ";"))
          (mapv ->in attributes)
          (mapv ->in varyings)
          (mapv ->uniform uniforms)
          (mapv ->out defined-varyings)
          (mapv ->out defined-outputs)
          "void main() {"
          (mapv (fn [[{:keys [name]} subexpression]]
                  (str "  " name " = " (->subexpression subexpression) ";"))
            (merge outputs-kv varyings-kv))
          "}"]
         flatten
         (remove nil?)
         (str/join \newline))))
     

