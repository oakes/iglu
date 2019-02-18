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

(def ^:dynamic *dependencies* nil)

(def ^:dynamic *current-out* nil)

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
    (when-let [out *current-out*]
      (some-> *dependencies*
              (swap! update out (fn [out-deps]
                                  (conj (set out-deps) x)))))
    true))

(defn output? [x]
  (instance? Output x))

(defn function? [x]
  (when (instance? Function x)
    (some-> *functions-used* (swap! conj x))
    true))

(s/def ::version string?)
(s/def ::precision string?)
(s/def ::shader-opts (s/keys :opt-un [::version ::precision]))
(s/def ::shader-fns (s/map-of function? fn?))

(s/def ::fn-expression (s/cat
                         :fn-record function?
                         :args (s/* ::subexpression)))
(s/def ::expression (s/cat
                      :fn-name keyword?
                      :args (s/* ::subexpression)))
(s/def ::subexpression (s/or
                         :number number?
                         :symbol symbol?
                         :attribute attribute?
                         :uniform uniform?
                         :varying varying?
                         :expression ::expression
                         :fn-expression ::fn-expression))

(s/def ::vertex-out (s/or
                      :output output?
                      :varying varying?))
(s/def ::fragment-out output?)

(defn throw-error [msg]
  (throw (#?(:clj Exception. :cljs js/Error.) msg)))

(defn parse-subexpression [content]
  (let [res (s/conform ::subexpression content)]
    (when (= res ::s/invalid)
      (throw-error (expound/expound-str ::subexpression content)))
    res))

(defn parse [shader-type content]
  (let [{:keys [opts fns outs]}
        (reduce-kv
          (fn [m k v]
            (cond
              (keyword? k)
              (update m :opts assoc k v)
              (function? k)
              (update m :fns assoc k v)
              :else
              (let [spec (case shader-type
                           :vertex ::vertex-out
                           :fragment ::fragment-out)]
                (if (= ::s/invalid (s/conform spec k))
                  (throw-error (expound/expound-str spec k))
                  (update m :outs assoc k v)))))
          {:opts {}
           :fns {}
           :outs {}}
          content)
        *attributes (atom #{})
        *uniforms (atom #{})
        *varyings (atom #{})
        *functions (atom #{})
        *dependencies (atom {})
        opts-res (s/conform ::shader-opts opts)
        fns-res (s/conform ::shader-fns fns)
        outs-res (binding [*attributes-used* *attributes
                           *uniforms-used* *uniforms
                           *varyings-used* *varyings
                           *functions-used* *functions
                           *dependencies* *dependencies]
                   (reduce-kv
                     (fn [m k v]
                       (binding [*current-out* k]
                         (assoc m k (parse-subexpression v))))
                     {}
                     outs))]
    (when (= :fragment shader-type)
      (some->> @*attributes first
               (str "You may not use an attribute in a fragment shader: ")
               throw-error))
    (cond
      (= opts-res ::s/invalid)
      (throw-error (expound/expound-str ::shader-opts opts))
      (= fns-res ::s/invalid)
      (throw-error (expound/expound-str ::shader-fns fns)))
    (merge opts-res fns-res outs-res
      {:attributes @*attributes
       :uniforms @*uniforms
       :varyings @*varyings
       :functions @*functions
       :dependencies @*dependencies})))

