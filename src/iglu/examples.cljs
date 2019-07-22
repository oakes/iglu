(ns iglu.examples
  (:require [iglu.core])
  (:require-macros [dynadoc.example :refer [defexamples]]))

(defexamples iglu.core/iglu->glsl
  ["A simple vertex shader."
   (iglu->glsl
     '{:version "300 es"
       :uniforms
       {u_matrix mat4}
       :inputs
       {a_position vec4
        a_color vec4}
       :outputs
       {v_color vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([]
              (= gl_Position (* a_position u_matrix))
              (= v_color a_color))}})]
  ["A simple fragment shader."
   (iglu->glsl
     '{:version "300 es"
       :precision "mediump float"
       :inputs
       {v_color vec4}
       :outputs
       {outColor vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([] (= outColor v_color))}})]
  ["A vertex shader with a user-defined function."
   (iglu->glsl
     '{:version "300 es"
       :uniforms
       {u_matrix mat4}
       :inputs
       {a_position vec4
        a_color vec4}
       :outputs
       {v_color vec4}
       :signatures
       {multiply ([mat4 vec4] vec4)
        main ([] void)}
       :functions
       {multiply ([x y] (* x y))
        main ([]
              (= gl_Position (multiply u_matrix a_position))
              (= v_color a_color))}})]
  ["You can specify function bodies as a string if you want to write them entirely in GLSL."
   (iglu->glsl
     '{:version "300 es"
       :uniforms
       {u_matrix mat4}
       :inputs
       {a_position vec4
        a_color vec4}
       :outputs
       {v_color vec4}
       :signatures
       {multiply ([mat4 vec4] vec4)
        main ([] void)}
       :functions
       {multiply ([x y] "return x * y;")
        main ([]
              "gl_Position = multiply(u_matrix, a_position);
v_color = a_color;")}})]
  ["You can also specify specific values as strings, making iglu pass them through
   without modification. This is generally what you want for GLSL keywords and
   floating point numbers."
   (iglu->glsl
     '{:version "300 es"
       :precision "mediump float"
       :uniforms
       {u_image sampler2D}
       :inputs
       {v_texCoord vec2}
       :outputs
       {outColor vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([]
               (= outColor (texture u_image v_texCoord))
               ("if" (== (.rgb outColor) (vec3 "0.0" "0.0" "0.0"))
                 "discard")
               ("else"
                 (= outColor (vec4 "0.0" "0.0" "0.0" "1.0"))))}})])

