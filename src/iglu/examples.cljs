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
  ["Strings are passed through without modification. This is generally what you
   want for GLSL keywords and floating point numbers."
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
                 (= outColor (vec4 "0.0" "0.0" "0.0" "1.0"))))}})]
  ["A vertex shader that loops over a uniform array."
   (iglu->glsl
     '{:version "300 es"
       :uniforms
       {u_matrix mat3
        u_char_counts [int 10]}
       :inputs
       {a_position vec2
        a_color vec4}
       :outputs
       {v_color vec4}
       :signatures
       {main ([] void)}
       :functions
       {main ([]
              (=int total_char_count 0)
              (=int current_line 0)
              ("for" "(int i=0; i<1024; ++i)"
                (+= total_char_count "u_char_counts[i]")
                ("if" (> total_char_count gl_InstanceID) "break")
                ("else" (+= current_line 1)))
              (=mat3 matrix u_matrix)
              (*= "matrix[2][1]" current_line)
              (= gl_Position
                (vec4
                  (.xy (* matrix (vec3 a_position 1)))
                  0 1))
              (= v_color a_color))}})])

