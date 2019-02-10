(ns iglu.core
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [expound.alpha :as expound]))

(defrecord Attribute [name type])
(def attribute ->Attribute)

(defrecord Uniform [name type])
(def uniform ->Uniform)

(defrecord Varying [name type])
(def varying ->Varying)

(defrecord Output [name type location])
(defn output [name & [type location]]
  (->Output name type location))

(def ^:private ^:dynamic *attributes-used* nil)

(def ^:private ^:dynamic *uniforms-used* nil)

(def ^:private ^:dynamic *varyings-used* nil)

(defn attribute? [x]
  (when (instance? Attribute x)
    (some-> *attributes-used* (swap! conj x))
    true))

(defn uniform? [x]
  (when (instance? Uniform x)
    (some-> *uniforms-used* (swap! conj x))
    true))

(defn varying? [x]
  (when (instance? Varying x)
    (some-> *varyings-used* (swap! conj x))
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
                         :varying varying?
                         :expression ::expression))
(s/def ::shader (s/map-of any? ::subexpression))

(s/def ::vertex-out (s/or
                      :output output?
                      :varying varying?))
(s/def ::fragment-out output?)

(defn ^:private throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(defn ^:private parse [content shader-type]
  (let [{:keys [opts outs]} (reduce-kv
                              (fn [m k v]
                                (if (keyword? k)
                                  (update m :opts assoc k v)
                                  (let [spec (case shader-type
                                               :vertex ::vertex-out
                                               :fragment ::fragment-out)]
                                    (if (= ::s/invalid (s/conform spec k))
                                      (throw-error (expound/expound-str spec k))
                                      (update m :outs assoc k v)))))
                              {:opts {}
                               :outs {}}
                              content)
        *attributes (atom #{})
        *uniforms (atom #{})
        *varyings (atom #{})
        opts-res (s/conform ::shader-opts opts)
        outs-res (binding [*attributes-used* *attributes
                           *uniforms-used* *uniforms
                           *varyings-used* *varyings]
                   (s/conform ::shader outs))]
    (case shader-type
      :vertex (some->> @*varyings first
                       (str "You may not use a varying in a vertex shader: ")
                       throw-error)
      :fragment (some->> @*attributes first
                         (str "You may not use an attribute in a fragment shader: ")
                         throw-error))
    (cond
      (= opts-res ::s/invalid)
      (throw-error (expound/expound-str ::shader-opts opts))
      (= outs-res ::s/invalid)
      (throw-error (expound/expound-str ::shader outs)))
    (merge opts-res outs-res
      {:attributes @*attributes
       :uniforms @*uniforms
       :varyings @*varyings})))

(defn ^:private glsl [content]
  content)

(defn iglu->glsl [vertex-shader fragment-shader]
  (let [vertex-shader (parse vertex-shader :vertex)
        fragment-shader (parse fragment-shader :fragment)
        varyings-not-passed (set/difference
                              (:varyings fragment-shader)
                              (-> vertex-shader keys set))]
    (when (seq varyings-not-passed)
      (throw-error (str "The following varyings must be set in the vertex shader: "
                     (str/join ", " (map :name varyings-not-passed)))))
    (mapv glsl [vertex-shader fragment-shader])))
