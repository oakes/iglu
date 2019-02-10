(ns iglu.core
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defrecord Attribute [name type])
(def attribute ->Attribute)

(defrecord Uniform [name type])
(def uniform ->Uniform)

(defrecord Output [name type location])
(defn output [name & [type location]]
  (->Output name type location))

(def ^:private ^:dynamic *attributes-used* nil)

(def ^:private ^:dynamic *uniforms-used* nil)

(defn attribute? [x]
  (when (instance? Attribute x)
    (some-> *attributes-used* (swap! conj x))
    true))

(defn uniform? [x]
  (when (instance? Uniform x)
    (some-> *uniforms-used* (swap! conj x))
    true))

(defn output? [x]
  (instance? Output x))

(s/def ::version string?)
(s/def ::shader-opts (s/keys :opt-un [::version]))

(s/def ::expression (s/cat
                      :fn keyword?
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :attribute attribute?
                         :uniform uniform?
                         :expression ::expression))
(s/def ::shader (s/map-of output? ::subexpression))

(defn ^:private throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(defn ^:private parse [content]
  (let [{:keys [opts syms]} (reduce-kv
                              (fn [m k v]
                                (cond
                                  (keyword? k) (update m :opts assoc k v)
                                  (output? k) (update m :syms assoc k v)
                                  :else (throw-error (str "Invalid key: " k))))
                              {:opts {}
                               :syms {}}
                              content)
        *attributes (atom #{})
        *uniforms (atom #{})
        opts-res (s/conform ::shader-opts opts)
        syms-res (binding [*attributes-used* *attributes
                           *uniforms-used* *uniforms]
                   (s/conform ::shader syms))]
    (cond
      (= opts-res ::s/invalid)
      (throw-error (expound/expound-str ::shader-opts opts))
      (= syms-res ::s/invalid)
      (throw-error (expound/expound-str ::shader syms)))
    (merge opts-res syms-res
      {:attributes @*attributes
       :uniforms @*uniforms})))

(defn ^:private glsl [content]
  content)

(defn iglu->glsl [contents]
  (->> contents
       ;; use spec to parse the maps
       (mapv parse)
       ;; pass attributes as varyings if necessary
       reverse
       (reduce
         (fn [{:keys [contents varyings]} content]
           {:contents (conj contents (assoc content :varyings varyings))
            :varyings (into varyings (:attributes content))})
         {:contents []
          :varyings #{}})
       :contents
       reverse
       ;; output to GLSL strings
       (mapv glsl)))

