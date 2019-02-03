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

;; rand-rects

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
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rand-rects-init)))

;; image

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
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/image-init)))

;; transformations

(defn translation-matrix [tx ty]
  (array
    1 0 0
    0 1 0
    tx ty 1))

(defn rotation-matrix [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c (- s) 0
      s c 0
      0 0 1)))

(defn scaling-matrix [sx sy]
  (array
    sx 0 0
    0 sy 0
    0 0 1))

(defn projection-matrix [width height]
  (array
    (/ 2 width) 0 0
    0 (/ -2 height) 0
    -1 1 1))

(defn multiply-matrices [dim m1 m2]
  (let [m1 (clj->js (partition dim m1))
        m2 (clj->js (partition dim m2))
        result (for [i (range dim)
                     j (range dim)]
                 (reduce
                   (fn [sum k]
                     (+ sum (* (aget m1 i k) (aget m2 k j))))
                   0
                   (range dim)))]
    (clj->js result)))

(def transformation-vertex-shader-source
  "#version 300 es
  
  in vec2 a_position;
  
  uniform mat3 u_matrix;
  
  void main() {
    gl_Position = vec4((u_matrix * vec3(a_position, 1)).xy, 0, 1);
  }")

(def transformation-fragment-shader-source
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

;; translation

(defn translation-render [canvas {:keys [x y]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  transformation-vertex-shader-source
                  transformation-fragment-shader-source)
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
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
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
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.width gl.canvas.height)
           (multiply-matrices 3 (translation-matrix x y))))
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
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/translation-init)))

;; rotation

(defn rotation-render [canvas {:keys [tx ty r]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  transformation-vertex-shader-source
                  transformation-fragment-shader-source)
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
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
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
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.width gl.canvas.height)
           (multiply-matrices 3 (translation-matrix tx ty))
           (multiply-matrices 3 (rotation-matrix r))
           ;; make it rotate around its center
           (multiply-matrices 3 (translation-matrix -50 -75))))
    (.drawArrays gl gl.TRIANGLES 0 18)))

(defn rotation-init [canvas]
  (let [tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-render canvas (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-render canvas @*state)))

(defexample iglu.core/rotation
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rotation-init)))

;; scale

(defn scale-render [canvas {:keys [tx ty sx sy]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  transformation-vertex-shader-source
                  transformation-fragment-shader-source)
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
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
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
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.width gl.canvas.height)
           (multiply-matrices 3 (translation-matrix tx ty))
           (multiply-matrices 3 (rotation-matrix 0))
           (multiply-matrices 3 (scaling-matrix sx sy))))
    (.drawArrays gl gl.TRIANGLES 0 18)))

(defn scale-init [canvas]
  (let [tx 100
        ty 100
        *state (atom {:tx tx :ty ty :sx 1 :sy 1})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              sx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              sy (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (scale-render canvas (swap! *state assoc :sx sx :sy sy)))))
    (scale-render canvas @*state)))

(defexample iglu.core/scale
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/scale-init)))

;; rotation-multi

(defn rotation-multi-render [canvas {:keys [tx ty r]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  transformation-vertex-shader-source
                  transformation-fragment-shader-source)
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
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
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
    (.uniform4f gl color-location 1 0 0.5 1)
    (loop [i 0
           matrix (projection-matrix gl.canvas.width gl.canvas.height)]
      (when (< i 5)
        (let [matrix (->> matrix
                          (multiply-matrices 3 (translation-matrix tx ty))
                          (multiply-matrices 3 (rotation-matrix r)))]
          (.uniformMatrix3fv gl matrix-location false matrix)
          (.drawArrays gl gl.TRIANGLES 0 18)
          (recur (inc i) matrix))))))

(defn rotation-multi-init [canvas]
  (let [tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-multi-render canvas (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-multi-render canvas @*state)))

(defexample iglu.core/rotation-multi
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rotation-multi-init)))


