(ns iglu.core
  (:require [iglu.glsl :as glsl]
            [iglu.parse :as parse]))

(def attribute parse/->Attribute)

(def uniform parse/->Uniform)

(def varying parse/->Varying)

(defn output [name & [type location]]
  (parse/->Output name type location))

(def function parse/->Function)

(defn iglu->glsl [shader-type shader]
  (->> shader
       (parse/parse shader-type)
       glsl/->glsl))

