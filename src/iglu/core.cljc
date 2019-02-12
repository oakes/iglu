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

(defn iglu->glsl [shader-type shader]
  (let [shader (parse/parse shader-type shader)
        fns-not-set (set/difference
                      (:functions shader)
                      (-> shader keys set))]
    (when (seq fns-not-set)
      (parse/throw-error
        (str "The following functions must be set in the shader: "
          (str/join ", " (map :name fns-not-set)))))
    (glsl/->glsl shader)))

