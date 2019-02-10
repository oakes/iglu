(ns iglu.glsl
  (:require [clojure.string :as str]
            [iglu.parse :as parse]))

;; multimethods

(defmulti ->function
  (fn [fn-name args]
    (if (#{:+ :- :* :/} fn-name)
      ::operator
      fn-name)))

(defmulti ->subexpression
  (fn [val] (first val)))

;; ->function

(defmethod ->function ::operator [fn-name args]
  (str/join (str " " (name fn-name) " ") (mapv ->subexpression args)))

(defmethod ->function :default [fn-name args]
  (str (name fn-name) "(" (str/join ", " (mapv ->subexpression args)) ")"))

;; ->expression

(defmethod ->subexpression :expression [[_ expression]]
  (let [{:keys [fn-name args]} expression]
    (->function fn-name args)))

(defmethod ->subexpression :attribute [[_ attribute]]
  (str (:name attribute)))

(defmethod ->subexpression :uniform [[_ uniform]]
  (str (:name uniform)))

(defmethod ->subexpression :varying [[_ varying]]
  (str (:name varying)))

;; var definitions

(defn ->in [{:keys [name type]}]
  (str "in " type " " name ";"))

(defn ->uniform [{:keys [name type]}]
  (str "uniform " type " " name ";"))

(defn ->out [{:keys [name type location]}]
  (when type
    (str "out " type " " name ";")))

;; compiler fn

(defn ->glsl [{:keys [version attributes uniforms varyings] :as shader}]
  (let [defined-varyings (filterv parse/varying? (keys shader))
        defined-outputs (filterv parse/output? (keys shader))
        outputs (select-keys shader defined-outputs)]
    (->> [(some->> version (str "#version "))
          (mapv ->in attributes)
          (mapv ->in varyings)
          (mapv ->uniform uniforms)
          (mapv ->out defined-varyings)
          (mapv ->out defined-outputs)
          "void main() {"
          (mapv (fn [[{:keys [name]} subexpression]]
                  (str "  " name " = " (->subexpression subexpression) ";"))
            outputs)
          "}"]
         flatten
         (remove nil?)
         (str/join \newline))))
     

