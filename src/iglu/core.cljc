(ns iglu.core
  (:require [iglu.glsl :as glsl]
            [iglu.parse :as parse]))

(defn iglu->glsl [shader]
  (->> shader
       parse/parse
       glsl/->glsl))

