(ns iglu.examples-2d
  (:require [iglu.examples :as ex]
            [goog.events :as events]
            [iglu.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; rand-rects

(defn rand-rects-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/rand-rects-vertex-shader-source
                  data/rand-rects-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
        resolution-location (.getUniformLocation gl program "u_resolution")
        color-location (.getUniformLocation gl program "u_color")]
    (ex/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform2f gl resolution-location gl.canvas.width gl.canvas.height)
    (dotimes [_ 50]
      (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
      (ex/set-rectangle gl (rand-int 300) (rand-int 300) (rand-int 300) (rand-int 300))
      (.uniform4f gl color-location (rand) (rand) (rand) 1)
      (.drawArrays gl gl.TRIANGLES 0 6))))

(defexample iglu.core/rand-rects
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-2d/rand-rects-init)))

;; image

(defn image-render [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/image-vertex-shader-source
                  data/image-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
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
    (ex/resize-canvas canvas)
    (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
    (.clearColor gl 0 0 0 0)
    (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
    (.useProgram gl program)
    (.bindVertexArray gl vao)
    (.uniform2f gl resolution-location gl.canvas.width gl.canvas.height)
    (.uniform1i gl image-location texture-unit)
    (.bindBuffer gl gl.ARRAY_BUFFER pos-buffer)
    (ex/set-rectangle gl 0 0 image.width image.height)
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
       (iglu.examples-2d/image-init)))

;; translation

(defn translation-render [canvas
                          {:keys [gl program vao matrix-location color-location]}
                          {:keys [x y]}]
  (ex/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (ex/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (ex/multiply-matrices 3 (ex/translation-matrix x y))))
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn translation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
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
       (iglu.examples-2d/translation-init)))

;; rotation

(defn rotation-render [canvas
                       {:keys [gl program vao matrix-location color-location]}
                       {:keys [tx ty r]}]
  (ex/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (ex/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (ex/multiply-matrices 3 (ex/translation-matrix tx ty))
         (ex/multiply-matrices 3 (ex/rotation-matrix r))
         ;; make it rotate around its center
         (ex/multiply-matrices 3 (ex/translation-matrix -50 -75))))
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn rotation-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
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
       (iglu.examples-2d/rotation-init)))

;; scale

(defn scale-render [canvas
                    {:keys [gl program vao matrix-location color-location]}
                    {:keys [tx ty sx sy]}]
  (ex/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (.uniformMatrix3fv gl matrix-location false
    (->> (ex/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)
         (ex/multiply-matrices 3 (ex/translation-matrix tx ty))
         (ex/multiply-matrices 3 (ex/rotation-matrix 0))
         (ex/multiply-matrices 3 (ex/scaling-matrix sx sy))))
  (.drawArrays gl gl.TRIANGLES 0 18))

(defn scale-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
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
       (iglu.examples-2d/scale-init)))

;; rotation-multi

(defn rotation-multi-render [canvas
                             {:keys [gl program vao matrix-location color-location]}
                             {:keys [tx ty r]}]
  (ex/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniform4f gl color-location 1 0 0.5 1)
  (loop [i 0
         matrix (ex/projection-matrix gl.canvas.clientWidth gl.canvas.clientHeight)]
    (when (< i 5)
      (let [matrix (->> matrix
                        (ex/multiply-matrices 3 (ex/translation-matrix tx ty))
                        (ex/multiply-matrices 3 (ex/rotation-matrix r)))]
        (.uniformMatrix3fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 18)
        (recur (inc i) matrix)))))

(defn rotation-multi-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/two-d-vertex-shader-source
                  data/two-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        pos-buffer (ex/create-buffer gl program "a_position")
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
       (iglu.examples-2d/rotation-multi-init)))

