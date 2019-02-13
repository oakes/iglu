(ns iglu.examples-3d
  (:require [iglu.examples :as ex]
            [goog.events :as events]
            [iglu.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; translation-3d

(defn translation-3d-render [canvas
                             {:keys [gl program vao matrix-location cnt]}
                             {:keys [x y]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/ortho-matrix-3d {:left 0
                              :right gl.canvas.clientWidth
                              :bottom gl.canvas.clientHeight
                              :top 0
                              :near 400
                              :far -400})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d x y 0))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d (ex/deg->rad 40)))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d (ex/deg->rad 25)))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d (ex/deg->rad 325)))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn translation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        *state (atom {:x 0 :y 0})]
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
       (iglu.examples-3d/translation-3d-init)))

;; rotation-3d

(defn rotation-3d-render [canvas
                          {:keys [gl program vao matrix-location cnt]}
                          {:keys [tx ty r]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/ortho-matrix-3d {:left 0
                              :right gl.canvas.clientWidth
                              :bottom gl.canvas.clientHeight
                              :top 0
                              :near 400
                              :far -400})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d tx ty 0))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d r))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d r))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d r))
         ;; make it rotate around its center
         (ex/multiply-matrices 4 (ex/translation-matrix-3d -50 -75 0))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn rotation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :r 0})]
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
       (iglu.examples-3d/rotation-3d-init)))

;; scale-3d

(defn scale-3d-render [canvas
                       {:keys [gl program vao matrix-location cnt]}
                       {:keys [tx ty sx sy]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/ortho-matrix-3d {:left 0
                              :right gl.canvas.clientWidth
                              :bottom gl.canvas.clientHeight
                              :top 0
                              :near 400
                              :far -400})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d tx ty 0))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d (ex/deg->rad 40)))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d (ex/deg->rad 25)))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d (ex/deg->rad 325)))
         (ex/multiply-matrices 4 (ex/scaling-matrix-3d sx sy 1))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn scale-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        tx 100
        ty 100
        *state (atom {:tx tx :ty ty :sx 1 :sy 1})]
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
       (iglu.examples-3d/scale-3d-init)))

;; perspective-3d

(defn perspective-3d-render [canvas
                             {:keys [gl program vao matrix-location cnt]}
                             {:keys [tx ty]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                    :aspect (/ gl.canvas.clientWidth
                                               gl.canvas.clientHeight)
                                    :near 1
                                    :far 2000})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d tx ty -150))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d (ex/deg->rad 180)))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d 0))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d 0))))
  (.drawArrays gl gl.TRIANGLES 0 cnt))

(defn perspective-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        *state (atom {:tx 0 :ty 0})]
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
       (iglu.examples-3d/perspective-3d-init)))

;; perspective-camera-3d

(defn perspective-camera-3d-render [canvas
                                    {:keys [gl program vao matrix-location cnt]}
                                    {:keys [r]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [radius 200
        num-fs 5
        projection-matrix (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-matrix (->> (ex/y-rotation-matrix-3d r)
                           (ex/multiply-matrices 4
                             (ex/translation-matrix-3d 0 0 (* radius 1.5))))
        view-matrix (ex/inverse-matrix 4 camera-matrix)
        view-projection-matrix (ex/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (ex/multiply-matrices 4
                     (ex/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 cnt)))))

(defn perspective-camera-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        positions (js/Float32Array. data/f-3d)
        matrix (ex/multiply-matrices 4
                 (ex/translation-matrix-3d -50 -75 -15)
                 (ex/x-rotation-matrix-3d js/Math.PI))
        _ (doseq [i (range 0 (.-length positions) 3)]
            (let [v (ex/transform-vector matrix
                      (array
                        (aget positions (+ i 0))
                        (aget positions (+ i 1))
                        (aget positions (+ i 2))
                        1))]
              (aset positions (+ i 0) (aget v 0))
              (aset positions (+ i 1) (aget v 1))
              (aset positions (+ i 2) (aget v 2))))
        cnt (ex/create-buffer gl program "a_position" positions {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-3d-render canvas props
            (swap! *state assoc :r (-> r (* 360) ex/deg->rad))))))
    (perspective-camera-3d-render canvas props @*state)))

(defexample iglu.core/perspective-camera-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-3d/perspective-camera-3d-init)))

;; perspective-camera-target-3d

(defn perspective-camera-target-3d-render [canvas
                                           {:keys [gl program vao matrix-location cnt]}
                                           {:keys [r]}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [radius 200
        num-fs 5
        projection-matrix (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-matrix (->> (ex/y-rotation-matrix-3d r)
                           (ex/multiply-matrices 4
                             (ex/translation-matrix-3d 0 50 (* radius 1.5))))
        camera-pos (array
                     (aget camera-matrix 12)
                     (aget camera-matrix 13)
                     (aget camera-matrix 14))
        f-pos (array radius 0 0)
        up (array 0 1 0)
        camera-matrix (ex/look-at camera-pos f-pos up)
        view-matrix (ex/inverse-matrix 4 camera-matrix)
        view-projection-matrix (ex/multiply-matrices 4 view-matrix projection-matrix)]
    (dotimes [i num-fs]
      (let [angle (/ (* i js/Math.PI 2) num-fs)
            x (* (js/Math.cos angle) radius)
            z (* (js/Math.sin angle) radius)
            matrix (ex/multiply-matrices 4
                     (ex/translation-matrix-3d x 0 z)
                     view-projection-matrix)]
        (.uniformMatrix4fv gl matrix-location false matrix)
        (.drawArrays gl gl.TRIANGLES 0 cnt)))))

(defn perspective-camera-target-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        positions (js/Float32Array. data/f-3d)
        matrix (ex/multiply-matrices 4
                 (ex/translation-matrix-3d -50 -75 -15)
                 (ex/x-rotation-matrix-3d js/Math.PI))
        _ (doseq [i (range 0 (.-length positions) 3)]
            (let [v (ex/transform-vector matrix
                      (array
                        (aget positions (+ i 0))
                        (aget positions (+ i 1))
                        (aget positions (+ i 2))
                        1))]
              (aset positions (+ i 0) (aget v 0))
              (aset positions (+ i 1) (aget v 1))
              (aset positions (+ i 2) (aget v 2))))
        cnt (ex/create-buffer gl program "a_position" positions {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        *state (atom {:r 0})]
    (events/listen js/window "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect canvas)
              r (/ (- (.-clientX event) (.-left bounds) (/ (.-width bounds) 2))
                   (.-width bounds))]
          (perspective-camera-target-3d-render canvas props
            (swap! *state assoc :r (-> r (* 360) ex/deg->rad))))))
    (perspective-camera-target-3d-render canvas props @*state)))

(defexample iglu.core/perspective-camera-target-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-3d/perspective-camera-target-3d-init)))

;; perspective-animation-3d

(defn perspective-animation-3d-render [canvas
                                       {:keys [gl program vao matrix-location cnt]
                                        :as props}
                                       {:keys [rx ry rz then now] :as state}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                    :aspect (/ gl.canvas.clientWidth
                                               gl.canvas.clientHeight)
                                    :near 1
                                    :far 2000})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d 0 0 -360))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d rx))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d ry))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d rz))))
  (.drawArrays gl gl.TRIANGLES 0 cnt)
  (js/requestAnimationFrame #(perspective-animation-3d-render canvas props
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-animation-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/three-d-vertex-shader-source
                  data/three-d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_color" (js/Uint8Array. data/f-3d-colors)
            {:size 3 :type gl.UNSIGNED_BYTE :normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        state {:rx (ex/deg->rad 190)
               :ry (ex/deg->rad 40)
               :rz (ex/deg->rad 320)
               :then 0
               :now 0}]
    (perspective-animation-3d-render canvas props state)))

(defexample iglu.core/perspective-animation-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-3d/perspective-animation-3d-init)))

;; perspective-texture-3d

(defn perspective-texture-3d-render [canvas
                                     {:keys [gl program vao matrix-location cnt]
                                      :as props}
                                     {:keys [rx ry rz then now] :as state}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (.uniformMatrix4fv gl matrix-location false
    (->> (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                    :aspect (/ gl.canvas.clientWidth
                                               gl.canvas.clientHeight)
                                    :near 1
                                    :far 2000})
         (ex/multiply-matrices 4 (ex/translation-matrix-3d 0 0 -360))
         (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d rx))
         (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d ry))
         (ex/multiply-matrices 4 (ex/z-rotation-matrix-3d rz))))
  (.drawArrays gl gl.TRIANGLES 0 cnt)
  (js/requestAnimationFrame #(perspective-texture-3d-render canvas props
                               (-> state
                                   (update :ry + (* 1.2 (- now then)))
                                   (assoc :then now :now (* % 0.001))))))

(defn perspective-texture-3d-init [canvas image]
  (let [gl (.getContext canvas "webgl2")
        program (ex/create-program gl
                  data/texture-vertex-shader-source
                  data/texture-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        matrix-location (.getUniformLocation gl program "u_matrix")
        cnt (ex/create-buffer gl program "a_position"
              (js/Float32Array. data/f-3d) {:size 3})
        _ (ex/create-buffer gl program "a_texcoord"
            (js/Float32Array. data/texcoords) {:normalize true})
        props {:gl gl
               :program program
               :vao vao
               :matrix-location matrix-location
               :cnt cnt}
        state {:rx (ex/deg->rad 190)
               :ry (ex/deg->rad 40)
               :rz (ex/deg->rad 320)
               :then 0
               :now 0}
        texture (.createTexture gl)]
    (.activeTexture gl (+ gl.TEXTURE0 0))
    (.bindTexture gl gl.TEXTURE_2D texture)
    (.texImage2D gl gl.TEXTURE_2D 0 gl.RGBA gl.RGBA gl.UNSIGNED_BYTE image)
    (.generateMipmap gl gl.TEXTURE_2D)
    (perspective-texture-3d-render canvas props state)))

(defn perspective-texture-3d-load [canvas]
  (let [image (js/Image.)]
    (doto image
      (-> .-src (set! "f-texture.png"))
      (-> .-onload (set! (fn []
                           (perspective-texture-3d-init canvas image)))))))

(defexample iglu.core/perspective-texture-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-3d/perspective-texture-3d-load)))

