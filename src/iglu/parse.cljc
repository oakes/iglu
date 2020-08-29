(ns iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(s/def ::type (s/or
                :type-name symbol?
                :array (s/cat :type-name symbol? :size int?)))
(s/def ::declarations (s/map-of symbol? ::type))

(s/def ::version string?)
(s/def ::precision string?)
(s/def ::uniforms ::declarations)
(s/def ::attributes ::declarations)
(s/def ::varyings ::declarations)
(s/def ::inputs ::declarations)
(s/def ::outputs ::declarations)

(def ^:dynamic *fn-dependencies* nil)

(def ^:dynamic *current-fn* nil)

(defn fn-name? [x]
  (when (and (symbol? x) *current-fn*)
    (some-> *fn-dependencies*
            (swap! (fn [deps]
                     (when (contains? (deps x) *current-fn*)
                       (throw (ex-info "Cyclic dependency detected between functions"
                                {:first-fn *current-fn*
                                 :second-fn x})))
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
                         :accessor (s/and vector? ::expression)
                         :expression ::expression))

(s/def ::signature (s/cat :in (s/coll-of symbol?) :out symbol?))
(s/def ::signatures (s/map-of symbol? ::signature))

(s/def ::body (s/+ (s/spec ::subexpression)))
(s/def ::function (s/cat :args (s/coll-of symbol?) :body ::body))
(s/def ::functions (s/or
                     :iglu (s/map-of symbol? ::function)
                     :glsl string?))

(s/def ::shader (s/keys :opt-un [::version
                                 ::precision
                                 ::uniforms
                                 ::attributes
                                 ::varyings
                                 ::inputs
                                 ::outputs
                                 ::signatures]
                        :req-un [::functions]))

(defn parse [content]
  (let [parsed-content (s/conform ::shader content)]
    (if (= parsed-content ::s/invalid)
      (throw (ex-info (expound/expound-str ::shader content) {}))
      (let [*fn-deps (atom {})]
        (when (map? (:functions content))
          (doseq [[fn-sym body] (:functions content)]
            (binding [*fn-dependencies* *fn-deps
                      *current-fn* fn-sym]
              (s/conform ::function body))))
        (assoc parsed-content
          :fn-deps @*fn-deps)))))

