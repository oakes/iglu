(ns iglu.core
  (:require [iglu.glsl :as glsl]
            [iglu.parse :as parse]))

(defn iglu->glsl [shader-type shader]
  (->> shader
       parse/parse
       (glsl/iglu->glsl shader-type)))

