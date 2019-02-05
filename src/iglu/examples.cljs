(ns iglu.examples
  (:require [goog.events :as events]
            [iglu.data :as data])
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

(defn multiply-matrices [size m1 m2]
  (let [m1 (clj->js (partition size m1))
        m2 (clj->js (partition size m2))
        result (for [i (range size)
                     j (range size)]
                 (reduce
                   (fn [sum k]
                     (+ sum (* (aget m1 i k) (aget m2 k j))))
                   0
                   (range size)))]
    (clj->js result)))

(defn create-buffer
  ([gl program attrib-name]
   (create-buffer gl program attrib-name {}))
  ([gl program attrib-name
    {:keys [size type normalize stride offset]
     :or {size 2
          type gl.FLOAT
          normalize false
          stride 0
          offset 0}}]
   (let [attrib-location (.getAttribLocation gl program attrib-name)
         buffer (.createBuffer gl)
         _ (.bindBuffer gl gl.ARRAY_BUFFER buffer)
         _ (.enableVertexAttribArray gl attrib-location)
         _ (.vertexAttribPointer gl attrib-location size type normalize stride offset)]
     buffer)))

(defn deg->rad [d]
  (-> d (* js/Math.PI) (/ 180)))

;; rand-rects

(defn rand-rects-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/rand-rects-vertex-shader-source
                  data/rand-rects-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
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

(defn image-render [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/image-vertex-shader-source
                  data/image-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
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

;; two-d

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

;; translation

(defn translation-render [canvas {:keys [x y]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
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
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
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
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform4f gl color-location 1 0 0.5 1)
    (.uniformMatrix3fv gl matrix-location false
      (->> (projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
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
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform4f gl color-location 1 0 0.5 1)
    (loop [i 0
           matrix (projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)]
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

;; three-d

(defn translation-matrix-3d [tx ty tz]
  (array
    1,  0,  0,  0,
    0,  1,  0,  0,
    0,  0,  1,  0,
    tx, ty, tz, 1,))

(defn x-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      1, 0, 0, 0,
      0, c, s, 0,
      0, (- s), c, 0,
      0, 0, 0, 1)))

(defn y-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c, 0, (- s), 0,
      0, 1, 0, 0,
      s, 0, c, 0,
      0, 0, 0, 1,)))

(defn z-rotation-matrix-3d [angle-in-radians]
  (let [c (js/Math.cos angle-in-radians)
        s (js/Math.sin angle-in-radians)]
    (array
      c, s, 0, 0,
      (- s), c, 0, 0,
      0, 0, 1, 0,
      0, 0, 0, 1,)))

(defn scaling-matrix-3d [sx sy sz]
  (array
    sx, 0,  0,  0,
    0, sy,  0,  0,
    0,  0, sz,  0,
    0,  0,  0,  1,))

(defn ortho-matrix-3d [{:keys [left right bottom top near far]}]
  (let [width (- right left)
        height (- top bottom)
        depth (- near far)]
    (array
      (/ 2 width) 0 0 0
      0 (/ 2 height) 0 0
      0 0 (/ 2 depth) 0
      
      (/ (+ left right)
         (- left right))
      (/ (+ bottom top)
         (- bottom top))
      (/ (+ near far)
         (- near far))
      1)))

(defn perspective-matrix-3d [{:keys [field-of-view aspect near far]}]
  (let [f (js/Math.tan (- (* js/Math.PI 0.5)
                          (* field-of-view 0.5)))
        range-inv (/ 1 (- near far))]
    (array
      (/ f aspect) 0 0 0
      0 f 0 0
      0 0 (* (+ near far) range-inv) -1
      0 0 (* near far range-inv 2) 0)))

;; translation-3d

(defn translation-3d-render [canvas {:keys [x y]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position" {:size 3})
        color-buffer (create-buffer gl program "a_color" {:size 3
                                                          :type gl.UNSIGNED_BYTE
                                                          :normalize true})
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.enable gl gl.CULL_FACE)
    (.enable gl gl.DEPTH_TEST)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniformMatrix4fv gl matrix-location false
      (->> (ortho-matrix-3d {:left 0
                             :right gl.canvas.clientWidth
                             :bottom gl.canvas.clientHeight
                             :top 0
                             :near 400
                             :far -400})
           (multiply-matrices 4 (translation-matrix-3d x y 0))
           (multiply-matrices 4 (x-rotation-matrix-3d (deg->rad 40)))
           (multiply-matrices 4 (y-rotation-matrix-3d (deg->rad 25)))
           (multiply-matrices 4 (z-rotation-matrix-3d (deg->rad 325)))))
    (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))

(defn translation-3d-init [canvas]
  (let [*state (atom {:x 0 :y 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-3d-render canvas (swap! *state assoc :x x :y y)))))
    (translation-3d-render canvas @*state)))

(defexample iglu.core/translation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [canvas {:keys [tx ty r]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position" {:size 3})
        color-buffer (create-buffer gl program "a_color" {:size 3
                                                          :type gl.UNSIGNED_BYTE
                                                          :normalize true})
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.enable gl gl.CULL_FACE)
    (.enable gl gl.DEPTH_TEST)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniformMatrix4fv gl matrix-location false
      (->> (ortho-matrix-3d {:left 0
                             :right gl.canvas.clientWidth
                             :bottom gl.canvas.clientHeight
                             :top 0
                             :near 400
                             :far -400})
           (multiply-matrices 4 (translation-matrix-3d tx ty 0))
           (multiply-matrices 4 (x-rotation-matrix-3d r))
           (multiply-matrices 4 (y-rotation-matrix-3d r))
           (multiply-matrices 4 (z-rotation-matrix-3d r))
           ;; make it rotate around its center
           (multiply-matrices 4 (translation-matrix-3d -50 -75 0))))
    (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))

(defn rotation-3d-init [canvas]
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
          (rotation-3d-render canvas (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-3d-render canvas @*state)))

(defexample iglu.core/rotation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [canvas {:keys [tx ty sx sy]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position" {:size 3})
        color-buffer (create-buffer gl program "a_color" {:size 3
                                                          :type gl.UNSIGNED_BYTE
                                                          :normalize true})
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.enable gl gl.CULL_FACE)
    (.enable gl gl.DEPTH_TEST)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniformMatrix4fv gl matrix-location false
      (->> (ortho-matrix-3d {:left 0
                             :right gl.canvas.clientWidth
                             :bottom gl.canvas.clientHeight
                             :top 0
                             :near 400
                             :far -400})
           (multiply-matrices 4 (translation-matrix-3d tx ty 0))
           (multiply-matrices 4 (x-rotation-matrix-3d (deg->rad 40)))
           (multiply-matrices 4 (y-rotation-matrix-3d (deg->rad 25)))
           (multiply-matrices 4 (z-rotation-matrix-3d (deg->rad 325)))
           (multiply-matrices 4 (scaling-matrix-3d sx sy 1))))
    (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))

(defn scale-3d-init [canvas]
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
          (scale-3d-render canvas (swap! *state assoc :sx sx :sy sy)))))
    (scale-3d-render canvas @*state)))

(defexample iglu.core/scale-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [canvas {:keys [tx ty]}]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position" {:size 3})
        color-buffer (create-buffer gl program "a_color" {:size 3
                                                          :type gl.UNSIGNED_BYTE
                                                          :normalize true})
        matrix-location (.getUniformLocation gl program "u_matrix")]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (resize-canvas canvas)
    (.enable gl gl.CULL_FACE)
    (.enable gl gl.DEPTH_TEST)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniformMatrix4fv gl matrix-location false
      (->> (perspective-matrix-3d {:field-of-view 90
                                   :aspect (/ gl.canvas.clientWidth
                                              gl.canvas.clientHeight)
                                   :near 1
                                   :far 2000})
           (multiply-matrices 4 (translation-matrix-3d tx ty -150))
           (multiply-matrices 4 (x-rotation-matrix-3d (deg->rad 180)))
           (multiply-matrices 4 (y-rotation-matrix-3d 0))
           (multiply-matrices 4 (z-rotation-matrix-3d 0))))
    (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))

(defn perspective-3d-init [canvas]
  (let [tx 0
        ty 0
        *state (atom {:tx tx :ty ty})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
              y (- (.-height bounds)
                   (- (.-clientY event) (.-top bounds)))]
          (perspective-3d-render canvas (swap! *state assoc :tx x :ty y)))))
    (perspective-3d-render canvas @*state)))

(defexample iglu.core/perspective-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/perspective-3d-init)))

