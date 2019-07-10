(ns iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(s/def ::declarations (s/map-of symbol? symbol?))

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
                       (throw (ex-info (str "Cyclic dependency detected between functions "
                                         *current-fn* " and " x)
                                {})))
                     (update deps *current-fn* #(conj (set %) x))))))
  (or (symbol? x)
      (number? x)
      (string? x)))

(s/def ::expression (s/cat
                      :fn-name fn-name?
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :symbol symbol?
                         :string string?
                         :expression ::expression))

(s/def ::signature (s/cat :in (s/coll-of symbol?) :out symbol?))
(s/def ::signatures (s/map-of symbol? ::signature))

(s/def ::body (s/alt
                :string string?
                :data (s/+ (s/spec ::expression))))
(s/def ::function (s/cat :args (s/coll-of symbol?) :body ::body))
(s/def ::functions (s/map-of symbol? ::function))

(s/def ::shader (s/keys :opt-un [::version
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
      (throw (ex-info (expound/expound-str ::shader content) {}))
      (let [*fn-deps (atom {})]
        (doseq [[fn-sym body] (:functions content)]
          (binding [*fn-dependencies* *fn-deps
                    *current-fn* fn-sym]
            (s/conform ::function body)))
        (assoc parsed-content
          :fn-deps @*fn-deps)))))

