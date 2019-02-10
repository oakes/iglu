(ns iglu.parse
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defrecord Attribute [name type])

(defrecord Uniform [name type])

(defrecord Varying [name type])

(defrecord Output [name type location])

(defrecord Function [name arg-types return-type])

(def ^:dynamic *attributes-used* nil)

(def ^:dynamic *uniforms-used* nil)

(def ^:dynamic *varyings-used* nil)

(def ^:dynamic *functions-used* nil)

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

(defn function? [x]
  (when (instance? Function x)
    (some-> *functions-used* (swap! conj x))
    true))

(s/def ::version string?)
(s/def ::shader-opts (s/keys :opt-un [::version]))

(s/def ::fn-expression (s/cat
                         :fn (s/or :fn fn? :fn-obj function?)
                         :args (s/* ::subexpression)))
(defn fn-expression? [x]
  (let [res (s/conform ::fn-expression x)]
    (when-not (= ::s/invalid res)
      (some-> *functions-used* (swap! conj res)))
    res))
(s/def ::expression (s/cat
                      :fn keyword?
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :attribute attribute?
                         :uniform uniform?
                         :varying varying?
                         :fn-expression fn-expression?
                         :expression ::expression))
(s/def ::shader (s/map-of any? ::subexpression))

(s/def ::vertex-out (s/or
                      :output output?
                      :function function?
                      :varying varying?))
(s/def ::fragment-out (s/or
                        :output output?
                        :function function?))

(defn throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(defn parse [content shader-type]
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
        *functions (atom #{})
        opts-res (s/conform ::shader-opts opts)
        outs-res (binding [*attributes-used* *attributes
                           *uniforms-used* *uniforms
                           *varyings-used* *varyings
                           *functions-used* *functions]
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
       :varyings @*varyings
       :functions @*functions})))

