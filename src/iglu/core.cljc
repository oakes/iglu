(ns iglu.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [iglu.glsl :as glsl]
            [iglu.parse :as parse]))

(def attribute parse/->Attribute)

(def uniform parse/->Uniform)

(def varying parse/->Varying)

(defn output [name & [type location]]
  (parse/->Output name type location))

(def function parse/->Function)

(defn iglu->glsl [vertex-shader fragment-shader]
  (let [vertex-shader (parse/parse vertex-shader :vertex)
        fragment-shader (parse/parse fragment-shader :fragment)
        varyings-not-set (set/difference
                           (:varyings fragment-shader)
                           (-> vertex-shader keys set))
        fns-not-set-v (set/difference
                        (:functions vertex-shader)
                        (-> vertex-shader keys set))
        fns-not-set-f (set/difference
                        (:functions fragment-shader)
                        (-> fragment-shader keys set))]
    (cond
      (seq varyings-not-set)
      (parse/throw-error
        (str "The following varyings must be set in the vertex shader: "
          (str/join ", " (map :name varyings-not-set))))
      (seq fns-not-set-v)
      (parse/throw-error
        (str "The following functions must be set in the vertex shader: "
          (str/join ", " (map :name fns-not-set-v))))
      (seq fns-not-set-f)
      (parse/throw-error
        (str "The following functions must be set in the fragment shader: "
          (str/join ", " (map :name fns-not-set-f)))))
    (mapv glsl/->glsl [vertex-shader fragment-shader])))

