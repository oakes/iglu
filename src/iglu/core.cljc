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
        varyings-not-passed (set/difference
                              (:varyings fragment-shader)
                              (-> vertex-shader keys set))]
    (when (seq varyings-not-passed)
      (parse/throw-error (str "The following varyings must be set in the vertex shader: "
                           (str/join ", " (map :name varyings-not-passed)))))
    (mapv glsl/->glsl [vertex-shader fragment-shader])))

