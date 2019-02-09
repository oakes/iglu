(ns iglu.data)

(def image-vertex-shader-source
  "#version 300 es
  
  in vec2 a_position;
  
  uniform mat3 u_matrix;
  
  out vec2 v_texCoord;
  
  void main() {
    gl_Position = vec4((u_matrix * vec3(a_position, 1)).xy, 0, 1);
  
    // pass the texCoord to the fragment shader
    // The GPU will interpolate this value between points
    v_texCoord = a_position;
  }")

(def image-fragment-shader-source
  "#version 300 es
  
  precision mediump float;
  
  uniform sampler2D u_image;
  
  in vec2 v_texCoord;
  
  out vec4 outColor;
   
  void main() {
     // Look up a color from the texture.
     outColor = texture(u_image, v_texCoord).bgra;
  }")

(def two-d-vertex-shader-source
  "#version 300 es
  
  in vec2 a_position;
  
  uniform mat3 u_matrix;
  
  void main() {
    gl_Position = vec4((u_matrix * vec3(a_position, 1)).xy, 0, 1);
  }")

(def two-d-fragment-shader-source
  "#version 300 es
  
  // fragment shaders don't have a default precision so we need
  // to pick one. mediump is a good default. It means 'medium precision'
  precision mediump float;
  
  uniform vec4 u_color;
  
  out vec4 outColor;
  
  void main() {
    outColor = u_color;
  }")

(def three-d-vertex-shader-source
  "#version 300 es
  
  in vec4 a_position;
  in vec4 a_color;
  uniform mat4 u_matrix;
  out vec4 v_color;
  
  void main() {
    gl_Position = u_matrix * a_position;
    v_color = a_color;
  }")

(def three-d-fragment-shader-source
  "#version 300 es
  
  // fragment shaders don't have a default precision so we need
  // to pick one. mediump is a good default. It means 'medium precision'
  precision mediump float;
  
  in vec4 v_color;
  
  out vec4 outColor;
  
  void main() {
    outColor = v_color;
  }")

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
