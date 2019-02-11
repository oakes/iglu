(ns iglu.data
  (:require [iglu.core :as c]))

(let [a-position (c/attribute 'a_position 'vec2)
      u-matrix (c/uniform 'u_matrix 'mat3)
      u-image (c/uniform 'u_image 'sampler2D)
      v-texcoord (c/varying 'v_texCoord 'vec2)
      [v f] (c/iglu->glsl
              {:version "300 es"
               (c/output 'gl_Position)
               [:vec4
                [:-xy [:* u-matrix [:vec3 a-position 1]]]
                0 1]
               v-texcoord a-position}
              {:version "300 es"
               :precision "mediump float"
               (c/output 'outColor 'vec4)
               [:-bgra [:texture u-image v-texcoord]]})]
  (def image-vertex-shader-source v)
  (def image-fragment-shader-source f))

(let [a-position (c/attribute 'a_position 'vec2)
      u-matrix (c/uniform 'u_matrix 'mat3)
      u-color (c/uniform 'u_color 'vec4)
      [v f] (c/iglu->glsl
              {:version "300 es"
               (c/output 'gl_Position)
               [:vec4
                [:-xy [:* u-matrix [:vec3 a-position 1]]]
                0 1]}
              {:version "300 es"
               :precision "mediump float"
               (c/output 'outColor 'vec4) u-color})]
  (def two-d-vertex-shader-source v)
  (def two-d-fragment-shader-source f))

(let [a-position (c/attribute 'a_position 'vec4)
      a-color (c/attribute 'a_color 'vec4)
      u-matrix (c/uniform 'u_matrix 'mat4)
      v-color (c/varying 'v_color 'vec4)
      [v f] (c/iglu->glsl
              {:version "300 es"
               (c/output 'gl_Position) [:* u-matrix a-position]
               v-color a-color}
              {:version "300 es"
               :precision "mediump float"
               (c/output 'outColor 'vec4) v-color})]
  (def three-d-vertex-shader-source v)
  (def three-d-fragment-shader-source f))

(def rect
  ;; x1 y1, x2 y1, x1 y2, x1 y2, x2 y1, x2 y2
  (array 0 0, 1 0, 0 1, 0 1, 1 0, 1 1))

(def f-2d
  (array
    ;; left column
    0 0, 30 0, 0 150, 0 150, 30 0, 30 150
    ;; top rung
    30 0, 100 0, 30 30, 30 30, 100 0, 100 30
    ;; middle rung
    30 60, 67 60, 30 90, 30 90, 67 60, 67 90))

(def f-3d
  (array
    ;; left column front
    0,   0,  0,
    0, 150,  0,
    30,   0,  0,
    0, 150,  0,
    30, 150,  0,
    30,   0,  0,

    ;; top rung front
    30,   0,  0,
    30,  30,  0,
    100,   0,  0,
    30,  30,  0,
    100,  30,  0,
    100,   0,  0,

    ;; middle rung front
    30,  60,  0,
    30,  90,  0,
    67,  60,  0,
    30,  90,  0,
    67,  90,  0,
    67,  60,  0,

    ;; left column back
      0,   0,  30,
     30,   0,  30,
      0, 150,  30,
      0, 150,  30,
     30,   0,  30,
     30, 150,  30,

    ;; top rung back
     30,   0,  30,
    100,   0,  30,
     30,  30,  30,
     30,  30,  30,
    100,   0,  30,
    100,  30,  30,

    ;; middle rung back
     30,  60,  30,
     67,  60,  30,
     30,  90,  30,
     30,  90,  30,
     67,  60,  30,
     67,  90,  30,

    ;; top
      0,   0,   0,
    100,   0,   0,
    100,   0,  30,
      0,   0,   0,
    100,   0,  30,
      0,   0,  30,

    ;; top rung right
    100,   0,   0,
    100,  30,   0,
    100,  30,  30,
    100,   0,   0,
    100,  30,  30,
    100,   0,  30,

    ;; under top rung
    30,   30,   0,
    30,   30,  30,
    100,  30,  30,
    30,   30,   0,
    100,  30,  30,
    100,  30,   0,

    ;; between top rung and middle
    30,   30,   0,
    30,   60,  30,
    30,   30,  30,
    30,   30,   0,
    30,   60,   0,
    30,   60,  30,

    ;; top of middle rung
    30,   60,   0,
    67,   60,  30,
    30,   60,  30,
    30,   60,   0,
    67,   60,   0,
    67,   60,  30,

    ;; right of middle rung
    67,   60,   0,
    67,   90,  30,
    67,   60,  30,
    67,   60,   0,
    67,   90,   0,
    67,   90,  30,

    ;; bottom of middle rung.
    30,   90,   0,
    30,   90,  30,
    67,   90,  30,
    30,   90,   0,
    67,   90,  30,
    67,   90,   0,

    ;; right of bottom
    30,   90,   0,
    30,  150,  30,
    30,   90,  30,
    30,   90,   0,
    30,  150,   0,
    30,  150,  30,

    ;; bottom
    0,   150,   0,
    0,   150,  30,
    30,  150,  30,
    0,   150,   0,
    30,  150,  30,
    30,  150,   0,

    ;; left side
    0,   0,   0,
    0,   0,  30,
    0, 150,  30,
    0,   0,   0,
    0, 150,  30,
    0, 150,   0,))

(def f-3d-colors
  (array
    ;; left column front
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    
      ;; top rung front
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    
      ;; middle rung front
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    200,  70, 120,
    
      ;; left column back
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    
      ;; top rung back
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    
      ;; middle rung back
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    80, 70, 200,
    
      ;; top
    70, 200, 210,
    70, 200, 210,
    70, 200, 210,
    70, 200, 210,
    70, 200, 210,
    70, 200, 210,
    
      ;; top rung right
    200, 200, 70,
    200, 200, 70,
    200, 200, 70,
    200, 200, 70,
    200, 200, 70,
    200, 200, 70,
    
      ;; under top rung
    210, 100, 70,
    210, 100, 70,
    210, 100, 70,
    210, 100, 70,
    210, 100, 70,
    210, 100, 70,
    
      ;; between top rung and middle
    210, 160, 70,
    210, 160, 70,
    210, 160, 70,
    210, 160, 70,
    210, 160, 70,
    210, 160, 70,
    
      ;; top of middle rung
    70, 180, 210,
    70, 180, 210,
    70, 180, 210,
    70, 180, 210,
    70, 180, 210,
    70, 180, 210,
    
      ;; right of middle rung
    100, 70, 210,
    100, 70, 210,
    100, 70, 210,
    100, 70, 210,
    100, 70, 210,
    100, 70, 210,
    
      ;; bottom of middle rung.
    76, 210, 100,
    76, 210, 100,
    76, 210, 100,
    76, 210, 100,
    76, 210, 100,
    76, 210, 100,
    
      ;; right of bottom
    140, 210, 80,
    140, 210, 80,
    140, 210, 80,
    140, 210, 80,
    140, 210, 80,
    140, 210, 80,
    
      ;; bottom
    90, 130, 110,
    90, 130, 110,
    90, 130, 110,
    90, 130, 110,
    90, 130, 110,
    90, 130, 110,
    
      ;; left side
    160, 160, 220,
    160, 160, 220,
    160, 160, 220,
    160, 160, 220,
    160, 160, 220,
    160, 160, 220,))
