(ns iglu.examples
  (:require [iglu.core])
  (:require-macros [dynadoc.example :refer [defexamples]]))

(defexamples iglu.core/iglu->glsl
  [(iglu->glsl :vertex
     '{:version "300 es"
       :attributes
       {a_position vec4
        a_color vec4}
       :uniforms
       {u_matrix mat4}
       :varyings
       {v_color vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([]
              (= gl_Position (* a_position u_matrix))
              (= v_color a_color))}})]
  [(iglu->glsl :fragment
     '{:version "300 es"
       :precision "mediump float"
       :varyings
       {v_color vec4}
       :outputs
       {outColor vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([] (= outColor v_color))}})]
  [(iglu->glsl :vertex
     '{:version "300 es"
       :attributes
       {a_position vec4
        a_color vec4}
       :uniforms
       {u_matrix mat4}
       :varyings
       {v_color vec4}
       :signatures
       {multiply ([mat4 vec4] vec4)
        main ([] void)}
       :functions
       {multiply ([x y] (* x y))
        main ([]
              (= gl_Position (multiply u_matrix a_position))
              (= v_color a_color))}})]
  [(iglu->glsl :fragment
     '{:version "300 es"
       :precision "mediump float"
       :varyings
       {v_color vec4}
       :outputs
       {outColor vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([] (= outColor v_color))}})])

