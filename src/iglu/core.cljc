(ns iglu.core
  (:require [iglu.glsl :as glsl]
            [iglu.parse :as parse]
            [clojure.set :as set]))

(defn iglu->glsl
  "Converts an iglu map into a GLSL string. The second arity is only for backwards
  compatibility and should not be used."
  ([shader]
   (-> shader parse/parse glsl/iglu->glsl))
  ([shader-type shader]
   (-> shader
       (set/rename-keys (case shader-type
                          :vertex {:attributes :inputs
                                   :varyings :outputs}
                          :fragment {:varyings :inputs}))
       parse/parse
       glsl/iglu->glsl)))

