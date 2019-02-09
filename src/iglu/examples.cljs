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
  (let [m1 (mapv vec (partition size m1))
        m2 (mapv vec (partition size m2))
        result (for [i (range size)
                     j (range size)]
                 (reduce
                   (fn [sum k]
                     (+ sum (* (get-in m1 [i k])
                               (get-in m2 [k j]))))
                   0
                   (range size)))]
    (clj->js result)))

(defn inverse-matrix [size m]
  (let [mc (mapv vec (partition size m))
        mi (mapv vec (for [i (range size)]
                       (for [j (range size)]
                         (if (= i j) 1 0))))
        mc (clj->js mc)
        mi (clj->js mi)]
    (dotimes [i size]
      (when (= 0 (aget mc i i))
        (loop [r (range (+ i 1) size)]
          (when-let [ii (first r)]
            (if (not= 0 (aget mc ii i))
              (dotimes [j size]
                (let [e (aget mc i j)]
                  (aset mc i j (aget mc ii j))
                  (aset mc ii j e))
                (let [e (aget mi i j)]
                  (aset mi i j (aget mi ii j))
                  (aset mi ii j e)))
              (recur (rest r))))))
      (let [e (aget mc i i)]
        (when (= 0 e)
          (throw (js/Error. "Not invertable")))
        (dotimes [j size]
          (aset mc i j (/ (aget mc i j) e))
          (aset mi i j (/ (aget mi i j) e))))
      (dotimes [ii size]
        (when (not= i ii)
          (let [e (aget mc ii i)]
            (dotimes [j size]
              (aset mc ii j
                (- (aget mc ii j)
                   (* e (aget mc i j))))
              (aset mi ii j
                (- (aget mi ii j)
                   (* e (aget mi i j)))))))))
    (->> mi seq (map seq) flatten clj->js)))

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

(defn transform-vector [m v]
  (let [dst (array)]
    (dotimes [i 4]
      (aset dst i 0.0)
      (dotimes [j 4]
        (aset dst i
          (+ (aget dst i)
             (* (aget v j)
                (aget m (-> j (* 4) (+ i))))))))
    dst))

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

(defn translation-render [canvas
                          {:keys [gl program vao matrix-location color-location]}
                          {:keys [x y]}]
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
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn translation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
               :matrix-location matrix-location}
        *state (atom {:x 0 :y 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-render canvas props (swap! *state assoc :x x :y y)))))
    (translation-render canvas props @*state)))

(defexample iglu.core/translation
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/translation-init)))

;; rotation

(defn rotation-render [canvas
                       {:keys [gl program vao matrix-location color-location]}
                       {:keys [tx ty r]}]
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
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn rotation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
               :matrix-location matrix-location}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-render canvas props @*state)))

(defexample iglu.core/rotation
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rotation-init)))

;; scale

(defn scale-render [canvas
                    {:keys [gl program vao matrix-location color-location]}
                    {:keys [tx ty sx sy]}]
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
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn scale-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
               :matrix-location matrix-location}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :sx 1 :sy 1})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              sx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              sy (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (scale-render canvas props (swap! *state assoc :sx sx :sy sy)))))
    (scale-render canvas props @*state)))

(defexample iglu.core/scale
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/scale-init)))

;; rotation-multi

(defn rotation-multi-render [canvas
                             {:keys [gl program vao matrix-location color-location]}
                             {:keys [tx ty r]}]
  (let []
    
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
  (let [gl (.getContext canvas "webgl2")
        program (create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (create-buffer gl program "a_position")
        color-location (.getUniformLocation gl program "u_color")
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :color-location color-location
               :matrix-location matrix-location}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-2d) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-multi-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-multi-render canvas props @*state)))

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

(defn translation-3d-render [canvas
                             {:keys [gl program vao matrix-location]}
                             {:keys [x y]}]
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
  (.drawArrays gl gl.TRIANGLES 0 (* 16 6)))

(defn translation-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        *state (atom {:x 0 :y 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (translation-3d-render canvas props (swap! *state assoc :x x :y y)))))
    (translation-3d-render canvas props @*state)))

(defexample iglu.core/translation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [canvas
                          {:keys [gl program vao matrix-location]}
                          {:keys [tx ty r]}]
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
  (.drawArrays gl gl.TRIANGLES 0 (* 16 6)))

(defn rotation-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              rx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              ry (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (rotation-3d-render canvas props (swap! *state assoc :r (Math/atan2 rx ry))))))
    (rotation-3d-render canvas props @*state)))

(defexample iglu.core/rotation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [canvas
                       {:keys [gl program vao matrix-location]}
                       {:keys [tx ty sx sy]}]
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
  (.drawArrays gl gl.TRIANGLES 0 (* 16 6)))

(defn scale-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :sx 1 :sy 1})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              sx (/ (- (.-clientX event) (.-left bounds) tx)
                    (.-width bounds))
              sy (/ (- (.-clientY event) (.-top bounds) ty)
                    (.-height bounds))]
          (scale-3d-render canvas props (swap! *state assoc :sx sx :sy sy)))))
    (scale-3d-render canvas props @*state)))

(defexample iglu.core/scale-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [canvas
                             {:keys [gl program vao matrix-location]}
                             {:keys [tx ty]}]
  (resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (perspective-matrix-3d {:field-of-view (deg->rad 60)
                                 :aspect (/ gl.canvas.clientWidth
                                            gl.canvas.clientHeight)
                                 :near 1
                                 :far 2000})
         (multiply-matrices 4 (translation-matrix-3d tx ty -150))
         (multiply-matrices 4 (x-rotation-matrix-3d (deg->rad 180)))
         (multiply-matrices 4 (y-rotation-matrix-3d 0))
         (multiply-matrices 4 (z-rotation-matrix-3d 0))))
  (.drawArrays gl gl.TRIANGLES 0 (* 16 6)))

(defn perspective-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        *state (atom {:tx 0 :ty 0})]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              x (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
              y (- (.-height bounds)
                   (- (.-clientY event) (.-top bounds)))]
          (perspective-3d-render canvas props (swap! *state assoc :tx x :ty y)))))
    (perspective-3d-render canvas props @*state)))

(defexample iglu.core/perspective-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/perspective-3d-init)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [canvas
                                    {:keys [gl program vao matrix-location]}
                                    {:keys [r]}]
  (resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [radius 200
        num-fs 5
        projection-matrix (perspective-matrix-3d {:field-of-view (deg->rad 60)
                                                  :aspect (/ gl.canvas.clientWidth
                                                             gl.canvas.clientHeight)
                                                  :near 1
                                                  :far 2000})
        camera-matrix (->> (y-rotation-matrix-3d r)
                           (multiply-matrices 4 (translation-matrix-3d 0 0 (* radius 1.5))))
        view-matrix (inverse-matrix 4 camera-matrix)
        view-projection-matrix (multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (multiply-matrices 4 (translation-matrix-3d x 0 z) view-projection-matrix)]
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))))

(defn perspective-camera-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        *state (atom {:r 0})
        positions (js/Float32Array. data/f-3d)
        matrix (multiply-matrices 4
                 (translation-matrix-3d -50 -75 -15)
                 (x-rotation-matrix-3d js/Math.PI))]
    (doseq [i (range 0 (.-length positions) 3)]
      (let [v (transform-vector matrix
                (array
                  (aget positions (+ i 0))
                  (aget positions (+ i 1))
                  (aget positions (+ i 2))
                  1))]
        (aset positions (+ i 0) (aget v 0))
        (aset positions (+ i 1) (aget v 1))
        (aset positions (+ i 2) (aget v 2))))
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER positions gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-3d-render canvas props (swap! *state assoc :r (-> r (* 360) deg->rad))))))
    (perspective-camera-3d-render canvas props @*state)))

(defexample iglu.core/perspective-camera-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/perspective-camera-3d-init)))

;; perspective-camera-target-3d

(defn cross [a b]
  (array
    (- (* (aget a 1) (aget b 2))
       (* (aget a 2) (aget b 1)))
    (- (* (aget a 2) (aget b 0))
       (* (aget a 0) (aget b 2)))
    (- (* (aget a 0) (aget b 1))
       (* (aget a 1) (aget b 0)))))

(defn subtract-vectors [a b]
  (array
    (- (aget a 0) (aget b 0))
    (- (aget a 1) (aget b 1))
    (- (aget a 2) (aget b 2))))

(defn normalize [v]
  (let [length (js/Math.sqrt
                 (+ (* (aget v 0) (aget v 0))
                    (* (aget v 1) (aget v 1))
                    (* (aget v 2) (aget v 2))))]
    (if (> length 0.00001)
      (array
        (/ (aget v 0) length)
        (/ (aget v 1) length)
        (/ (aget v 2) length))
      (array 0 0 0))))

(defn look-at [camera-pos target up]
  (let [z-axis (normalize (subtract-vectors camera-pos target))
        x-axis (normalize (cross up z-axis))
        y-axis (normalize (cross z-axis x-axis))]
    (array
      (aget x-axis 0) (aget x-axis 1) (aget x-axis 2) 0
      (aget y-axis 0) (aget y-axis 1) (aget y-axis 2) 0
      (aget z-axis 0) (aget z-axis 1) (aget z-axis 2) 0
      (aget camera-pos 0) (aget camera-pos 1) (aget camera-pos 2) 1)))

(defn perspective-camera-target-3d-render [canvas
                                           {:keys [gl program vao matrix-location]}
                                           {:keys [r]}]
  (resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [radius 200
        num-fs 5
        projection-matrix (perspective-matrix-3d {:field-of-view (deg->rad 60)
                                                  :aspect (/ gl.canvas.clientWidth
                                                             gl.canvas.clientHeight)
                                                  :near 1
                                                  :far 2000})
        camera-matrix (->> (y-rotation-matrix-3d r)
                           (multiply-matrices 4 (translation-matrix-3d 0 50 (* radius 1.5))))
        camera-pos (array
                     (aget camera-matrix 12)
                     (aget camera-matrix 13)
                     (aget camera-matrix 14))
        f-pos (array radius 0 0)
        up (array 0 1 0)
        camera-matrix (look-at camera-pos f-pos up)
        view-matrix (inverse-matrix 4 camera-matrix)
        view-projection-matrix (multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (multiply-matrices 4 (translation-matrix-3d x 0 z) view-projection-matrix)]
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 (* 16 6))))))

(defn perspective-camera-target-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        *state (atom {:r 0})
        positions (js/Float32Array. data/f-3d)
        matrix (multiply-matrices 4
                 (translation-matrix-3d -50 -75 -15)
                 (x-rotation-matrix-3d js/Math.PI))]
    (doseq [i (range 0 (.-length positions) 3)]
      (let [v (transform-vector matrix
                (array
                  (aget positions (+ i 0))
                  (aget positions (+ i 1))
                  (aget positions (+ i 2))
                  1))]
        (aset positions (+ i 0) (aget v 0))
        (aset positions (+ i 1) (aget v 1))
        (aset positions (+ i 2) (aget v 2))))
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER positions gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-target-3d-render canvas props (swap! *state assoc :r (-> r (* 360) deg->rad))))))
    (perspective-camera-target-3d-render canvas props @*state)))

(defexample iglu.core/perspective-camera-target-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/perspective-camera-target-3d-init)))

;; perspective-animation-3d-render

(defn perspective-animation-3d-render [canvas
                                       {:keys [gl program vao matrix-location] :as props}
                                       {:keys [rx ry rz then now] :as state}]
  (resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (perspective-matrix-3d {:field-of-view (deg->rad 60)
                                 :aspect (/ gl.canvas.clientWidth
                                            gl.canvas.clientHeight)
                                 :near 1
                                 :far 2000})
         (multiply-matrices 4 (translation-matrix-3d 0 0 -360))
         (multiply-matrices 4 (x-rotation-matrix-3d rx))
         (multiply-matrices 4 (y-rotation-matrix-3d ry))
         (multiply-matrices 4 (z-rotation-matrix-3d rz))))
  (.drawArrays gl gl.TRIANGLES 0 (* 16 6))
  (js/requestAnimationFrame #(perspective-animation-3d-render canvas props
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-animation-3d-init [canvas]
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
        matrix-location (.getUniformLocation gl program "u_matrix")
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location}
        state {:rx (deg->rad 190)
               :ry (deg->rad 40)
               :rz (deg->rad 320)
               :then 0
               :now 0}]
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Float32Array. data/f-3d) gl.STATIC_DRAW)
    (.bindBuffer gl gl.ARRAY_BUFFER color-buffer)
    (.bufferData gl gl.ARRAY_BUFFER (js/Uint8Array. data/f-3d-colors) gl.STATIC_DRAW)
    (perspective-animation-3d-render canvas props state)))

(defexample iglu.core/perspective-animation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples/perspective-animation-3d-init)))

