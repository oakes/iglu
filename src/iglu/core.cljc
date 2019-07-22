(ns iglu.core
  (:require [iglu.glsl :as glsl]
            [iglu.parse :as parse]))

(defn iglu->glsl
  "Converts an iglu map into a GLSL string. The second arity is only for backwards
  compatibility and should not be used."
  ([shader]
   (-> shader parse/parse glsl/iglu->glsl))
  ([shader-type shader]
   (-> (reduce-kv
         (fn [shader old-key new-key]
           (let [combined-vals (merge (new-key shader) (old-key shader))]
             (cond-> (dissoc shader old-key)
                     (seq combined-vals)
                     (assoc new-key combined-vals))))
         shader
         (case shader-type
           :vertex {:attributes :inputs
                    :varyings :outputs}
           :fragment {:varyings :inputs}))
       parse/parse
       glsl/iglu->glsl)))

