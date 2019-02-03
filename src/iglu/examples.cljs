(ns iglu.examples
  (:require [goog.events :as events])
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

(def rand-rects-vertex-shader-source
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

(def rand-rects-fragment-shader-source
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

(defn rand-rects-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  rand-rects-vertex-shader-source
                  rand-rects-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (let [pos-attrib-location (.getAttribLocation gl program "a_position")
                         pos-buffer (.createBuffer gl)
                         _ (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
                         _ (.enableVertexAttribArray gl pos-attrib-location)
                         size 2, type gl.FLOAT, normalize false, stride 0, offset 0
                         _ (.vertexAttribPointer gl pos-attrib-location size type normalize stride offset)]
                     pos-buffer)
        resolution-location (.getUniformLocation gl program "u_resolution")
        color-location (.getUniformLocation gl program "u_color")]
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform2f gl resolution-location gl.canvas.width gl.canvas.height)
    (dotimes [_ 50]
      (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
      (set-rectangle gl (rand-int 300) (rand-int 300) (rand-int 300) (rand-int 300))
      (.uniform4f gl color-location (rand) (rand) (rand) 1)
      (.drawArrays gl gl.TRIANGLES 0 6))))

(defexample iglu.core/rand-rects
  {:with-card card}
  (defonce rand-rects-canvas (iglu.examples/create-canvas card))
  (iglu.examples/rand-rects-init rand-rects-canvas))

(def image-vertex-shader-source
  "#version 300 es
  
  // an attribute is an input (in) to a vertex shader.
  // It will receive data from a buffer
  in vec2 a_position;
  
  in vec2 a_texCoord;
  
  uniform vec2 u_resolution;
  
  out vec2 v_texCoord;
  
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
  
    // pass the texCoord to the fragment shader
    // The GPU will interpolate this value between points
    v_texCoord = a_texCoord;
  }")

(def image-fragment-shader-source
  "#version 300 es
  
  precision mediump float;
   
  // our texture
  uniform sampler2D u_image;
   
  // the texCoords passed in from the vertex shader.
  in vec2 v_texCoord;
   
  // we need to declare an output for the fragment shader
  out vec4 outColor;
   
  void main() {
     // Look up a color from the texture.
     outColor = texture(u_image, v_texCoord).bgra;
  }")

(defn image-render [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  image-vertex-shader-source
                  image-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (let [pos-attrib-location (.getAttribLocation gl program "a_position")
                         pos-buffer (.createBuffer gl)
                         _ (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
                         _ (.enableVertexAttribArray gl pos-attrib-location)
                         size 2, type gl.FLOAT, normalize false, stride 0, offset 0
                         _ (.vertexAttribPointer gl pos-attrib-location size type normalize stride offset)]
                     pos-buffer)
        tex-coord-buffer (let [tex-coord-attrib-location (.getAttribLocation gl program "a_texCoord")
                               tex-coord-buffer (.createBuffer gl)
                               _ (.bindBuffer gl gl.ARRAY_BUFFER tex-coord-buffer)
                               _ (.bufferData gl gl.ARRAY_BUFFER
                                   (js/Float32Array. (array
                                                       0.0 0.0, 1.0 0.0, 0.0 1.0
                                                       0.0 1.0, 1.0 0.0, 1.0 1.0))
                                   gl.STATIC_DRAW)
                               _ (.enableVertexAttribArray gl tex-coord-attrib-location)
                               size 2, type gl.FLOAT, normalize false, stride 0, offset 0
                               _ (.vertexAttribPointer gl tex-coord-attrib-location
                                   size type normalize stride offset)]
                           tex-coord-buffer)
        resolution-location (.getUniformLocation gl program "u_resolution")
        image-location (.getUniformLocation gl program "u_image")
        texture-unit 0]
    (let [texture (.createTexture gl)]
      (.activeTexture gl (+ gl.TEXTURE0 texture-unit))
      (.bindTexture gl gl.TEXTURE_2D texture)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_S gl.CLAMP_TO_EDGE)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_WRAP_T gl.CLAMP_TO_EDGE)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MIN_FILTER gl.NEAREST)
      (.texParameteri gl gl.TEXTURE_2D gl.TEXTURE_MAG_FILTER gl.NEAREST))
    (let [mip-level 0, internal-fmt gl.RGBA, src-fmt gl.RGBA, src-type gl.UNSIGNED_BYTE]
      (.texImage2D gl gl.TEXTURE_2D mip-level internal-fmt src-fmt src-type image))
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform2f gl resolution-location gl.canvas.width gl.canvas.height)
    (.uniform1i gl image-location texture-unit)
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (set-rectangle gl 0 0 image.width image.height)
    (.drawArrays gl gl.TRIANGLES 0 6)))

(defn image-init [canvas]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "leaves.jpg"))
      (-> .-onload (set! (fn []
                           (image-render canvas image)))))))

(defexample iglu.core/image
  {:with-card card}
  (defonce image-canvas (iglu.examples/create-canvas card))
  (iglu.examples/image-init image-canvas))

(def translation-vertex-shader-source
  "#version 300 es
  
  // an attribute is an input (in) to a vertex shader.
  // It will receive data from a buffer
  in vec2 a_position;
  
  uniform vec2 u_resolution;
  
  // translation to add to position
  uniform vec2 u_translation;
  
  // all shaders have a main function
  void main() {
    // Add in the translation  
    vec2 position = a_position + u_translation;
  
    // convert the position from pixels to 0.0 to 1.0
    vec2 zeroToOne = position / u_resolution;
 
    // convert from 0->1 to 0->2
    vec2 zeroToTwo = zeroToOne * 2.0;
 
    // convert from 0->2 to -1->+1 (clipspace)
    vec2 clipSpace = zeroToTwo - 1.0;
  
    // gl_Position is a special variable a vertex shader
    // is responsible for setting
    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
  }")

(def translation-fragment-shader-source
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

(defn translation-render [canvas {:keys [x y]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  translation-vertex-shader-source
                  translation-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (let [pos-attrib-location (.getAttribLocation gl program "a_position")
                         pos-buffer (.createBuffer gl)
                         _ (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
                         _ (.enableVertexAttribArray gl pos-attrib-location)
                         size 2, type gl.FLOAT, normalize false, stride 0, offset 0
                         _ (.vertexAttribPointer gl pos-attrib-location size type normalize stride offset)]
                     pos-buffer)
        resolution-location (.getUniformLocation gl program "u_resolution")
        color-location (.getUniformLocation gl program "u_color")
        translation-location (.getUniformLocation gl program "u_translation")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER
      (js/Float32Array. (array
                          ;; left column
                          0 0, 30 0, 0 150, 0 150, 30 0, 30 150
                          ;; top rung
                          30 0, 100 0, 30 30, 30 30, 100 0, 100 30
                          ;; middle rung
                          30 60, 67 60, 30 90, 30 90, 67 60, 67 90))
      gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform2f gl resolution-location gl.canvas.width gl.canvas.height)
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniform2fv gl translation-location (array x y))
    (.drawArrays gl gl.TRIANGLES 0 18)))

(defn translation-init [canvas]
  (let [*state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-render canvas (swap! *state assoc :x x :y y)))))
    (translation-render canvas @*state)))

(defexample iglu.core/translation
  {:with-card card}
  (defonce translation-canvas (iglu.examples/create-canvas card))
  (iglu.examples/translation-init translation-canvas))


