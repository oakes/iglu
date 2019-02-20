(ns iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(s/def ::declarations (s/map-of symbol? symbol?))

(s/def ::type #{:vertex :fragment})
(s/def ::version string?)
(s/def ::precision string?)
(s/def ::uniforms ::declarations)
(s/def ::attributes ::declarations)
(s/def ::varyings ::declarations)
(s/def ::outputs ::declarations)

(def ^:dynamic *fn-dependencies* nil)

(def ^:dynamic *current-fn* nil)

(defn fn-name? [x]
  (when (and (symbol? x) *current-fn*)
    (some-> *fn-dependencies*
            (swap! (fn [deps]
                     (when (contains? (deps x) *current-fn*)
                       (throw-error (str "Cyclic dependency detected between functions "
                                      *current-fn* " and " x)))
                     (update deps *current-fn* #(conj (set %) x))))))
  (or (keyword? x)
      (symbol? x)))

(s/def ::expression (s/cat
                      :fn-name fn-name?
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :symbol symbol?
                         :expression ::expression))

(s/def ::signature (s/cat :in (s/coll-of symbol?) :out symbol?))
(s/def ::signatures (s/map-of symbol? ::signature))

(s/def ::body (s/+ (s/spec ::expression)))
(s/def ::function (s/cat :args (s/coll-of symbol?) :body ::body))
(s/def ::functions (s/map-of symbol? ::function))

(s/def ::shader (s/keys :opt-un [::type
                                 ::version
                                 ::precision
                                 ::uniforms
                                 ::attributes
                                 ::varyings
                                 ::outputs
                                 ::signatures
                                 ::functions]))

(defn parse [content]
  (let [parsed-content (s/conform ::shader content)]
    (if (= parsed-content ::s/invalid)
      (throw-error (expound/expound-str ::shader content))
      (let [*fn-deps (atom {})]
        (doseq [[fn-sym {:keys [body]}] (:functions content)]
          (binding [*fn-dependencies* *fn-deps
                    *current-fn* fn-sym]
            (s/conform ::body body)))
        (assoc parsed-content
          :fn-deps @*fn-deps)))))

