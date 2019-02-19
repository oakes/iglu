(ns iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(s/def ::declarations (s/map-of symbol? symbol?))

(s/def ::type keyword?)
(s/def ::version string?)
(s/def ::precision string?)
(s/def ::uniforms ::declarations)
(s/def ::attributes ::declarations)
(s/def ::varyings ::declarations)
(s/def ::outputs ::declarations)

(s/def ::expression (s/cat
                      :fn-name #(or (keyword? %) (symbol? %))
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :symbol symbol?
                         :expression ::expression))

(s/def ::ret symbol?)
(s/def ::args (s/* (s/cat :type symbol? :name symbol?)))
(s/def ::body ::subexpression)
(s/def ::function (s/keys :req-un [::ret ::args ::body]))
(s/def ::functions (s/map-of symbol? ::function))

(s/def ::main (s/map-of symbol? ::subexpression))

(s/def ::shader (s/keys :opt-un [::type
                                 ::version
                                 ::precision
                                 ::uniforms
                                 ::attributes
                                 ::varyings
                                 ::outputs
                                 ::functions
                                 ::main]))

(defn parse-subexpression [content]
  (let [res (s/conform ::subexpression content)]
    (when (= res ::s/invalid)
      (throw-error (expound/expound-str ::subexpression content)))
    res))

(defn parse [content]
  (let [parsed-content (s/conform ::shader content)]
    (if (= parsed-content ::s/invalid)
      (throw-error (expound/expound-str ::shader content))
      parsed-content)))

