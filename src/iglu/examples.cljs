(ns iglu.examples
  (:require-macros [dynadoc.example :refer [defexample]]))

(defn create-canvas [card]
  (let [canvas (doto (js/document.createElement "canvas")
                 (-> .-style .-width (set! "100%"))
                 (-> .-style .-height (set! "100%")))]
    (.appendChild card canvas)
    canvas))

(defn resize-canvas [canvas]
  (let [display-width canvas.clientWidth
        display-height canvas.clientHeight]
    (when (or (not= canvas.width display-width)
              (not= canvas.height display-height))
      (set! canvas.width display-width)
      (set! canvas.height display-height))))

(def vertex-shader-source
  "#version 300 es
  
  // an attribute is an input (in) to a vertex shader.
  // It will receive data from a buffer
  in vec2 a_position;
  
  uniform vec2 u_resolution;
  
  // all shaders have a main function
  void main() {
    // convert the position from pixels to 0.0 to 1.0
    vec2 zeroToOne = a_position / u_resolution;
 
    // convert from 0->1 to 0->2
    vec2 zeroToTwo = zeroToOne * 2.0;
 
    // convert from 0->2 to -1->+1 (clipspace)
    vec2 clipSpace = zeroToTwo - 1.0;
  
    // gl_Position is a special variable a vertex shader
    // is responsible for setting
    gl_Position = vec4(clipSpace, 0, 1);
  }")

(def fragment-shader-source
  "#version 300 es
  
  // fragment shaders don't have a default precision so we need
  // to pick one. mediump is a good default. It means 'medium precision'
  precision mediump float;
  
  uniform vec4 u_color;
  
  // we need to declare an output for the fragment shader
  out vec4 outColor;
  
  void main() {
    // Just set the output to a constant redish-purple
    outColor = u_color;
  }")

(defn create-shader [gl type source]
  (let [shader (.createShader gl type)]
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (if (.getShaderParameter gl shader gl.COMPILE_STATUS)
      shader
      (do
        (js/console.log (.getShaderInfoLog gl shader))
        (.deleteShader gl shader)))))

(defn create-program [gl v-source f-source]
  (let [vertex-shader (create-shader gl gl.VERTEX_SHADER v-source)
        fragment-shader (create-shader gl gl.FRAGMENT_SHADER f-source)
        program (.createProgram gl)]
    (.attachShader gl program vertex-shader)
    (.attachShader gl program fragment-shader)
    (.linkProgram gl program)
    (if (.getProgramParameter gl program gl.LINK_STATUS)
      program
      (do
        (js/console.log (.getProgramInfoLog gl program))
        (.deleteProgram gl program)))))

(defn set-rectangle [gl x y width height]
  (let [x1 x, x2 (+ x width), y1 y, y2 (+ y height)]
    (.bufferData gl gl.ARRAY_BUFFER
      (js/Float32Array. (array x1 y1, x2 y1, x1 y2, x1 y2, x2 y1, x2 y2))
      gl.STATIC_DRAW)))

(defn run-fundamentals [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl vertex-shader-source fragment-shader-source)
        pos-attrib-location (.getAttribLocation gl program "a_position")
        pos-buffer (.createBuffer gl)
        _ (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
        _ (.bufferData gl gl.ARRAY_BUFFER
            (js/Float32Array. (array 0 0, 0 0.5, 0.7 0))
            gl.STATIC_DRAW)
        vao (.createVertexArray gl)
        _ (.bindVertexArray gl vao)
        _ (.enableVertexAttribArray gl pos-attrib-location)
        size 2, type gl.FLOAT, normalize false, stride 0, offset 0
        _ (.vertexAttribPointer gl pos-attrib-location size type normalize stride offset)
        resolution-uniform-location (.getUniformLocation gl program "u_resolution")
        _ (.bufferData gl gl.ARRAY_BUFFER
            (js/Float32Array. (array 10 20, 80 20, 10 30, 10 30, 80 20, 80 30))
            gl.STATIC_DRAW)
        color-location (.getUniformLocation gl program "u_color")]
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl gl.COLOR_BUFFER_BIT)
    (.useProgram gl program)
    (.uniform2f gl resolution-uniform-location gl.canvas.width gl.canvas.height)
    (.bindVertexArray gl vao)
    (dotimes [_ 50]
      (set-rectangle gl (rand-int 300) (rand-int 300) (rand-int 300) (rand-int 300))
      (.uniform4f gl color-location (rand) (rand) (rand) 1)
      (.drawArrays gl gl.TRIANGLES 0 6))))

(defexample iglu.core/fundamentals
  {:with-card card}
  (defonce fundamentals-canvas (iglu.examples/create-canvas card))
  (iglu.examples/run-fundamentals fundamentals-canvas))

