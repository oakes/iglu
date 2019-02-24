(ns iglu.examples-advanced
  (:require [iglu.examples :as ex]
            [goog.events :as events]
            [iglu.data :as data]
            [iglu.primitives :as primitives])
  (:require-macros [dynadoc.example :refer [defexample]]))

(defn advanced-render [canvas
                       {:keys [gl program vao cnt objects]
                        {:keys [light-world-pos
                                view-inverse
                                light-color
                                world-view-projection
                                world
                                world-inverse-transpose
                                color
                                specular
                                shininess
                                specular-factor]}
                        :uniforms
                        :as props}
                       {:keys [then now] :as state}]
  (ex/resize-canvas canvas)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.useProgram gl program)
  (.bindVertexArray gl vao)
  (let [projection-matrix (ex/perspective-matrix-3d {:field-of-view (ex/deg->rad 60)
                                                     :aspect (/ gl.canvas.clientWidth
                                                                gl.canvas.clientHeight)
                                                     :near 1
                                                     :far 2000})
        camera-pos (array 0 0 100)
        target (array 0 0 0)
        up (array 0 1 0)
        camera-matrix (ex/look-at camera-pos target up)
        view-matrix (ex/inverse-matrix 4 camera-matrix)
        view-projection-matrix (ex/multiply-matrices 4 view-matrix projection-matrix)]
    (.uniform3fv gl light-world-pos (array -50 30 100))
    (.uniformMatrix4fv gl view-inverse false camera-matrix)
    (.uniform4fv gl light-color (array 1 1 1 1))
    (doseq [{:keys [rx ry tz mat-uniforms]}
            objects]
      (let [world-matrix (->> (ex/identity-matrix-3d)
                              (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d (* rx now)))
                              (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d (* ry now)))
                              (ex/multiply-matrices 4 (ex/translation-matrix-3d 0 0 tz)))]
        (.uniformMatrix4fv gl world false world-matrix)
        (.uniformMatrix4fv gl world-view-projection false
          (->> view-projection-matrix
               (ex/multiply-matrices 4 world-matrix)))
        (.uniformMatrix4fv gl world-inverse-transpose false
          (->> world-matrix
               (ex/inverse-matrix 4)
               (ex/transpose-matrix-3d)))
        (.uniform4fv gl color (:u_color mat-uniforms))
        (.uniform4fv gl specular (:u_specular mat-uniforms))
        (.uniform1f gl shininess (:u_shininess mat-uniforms))
        (.uniform1f gl specular-factor (:u_specularFactor mat-uniforms))
        (.drawElements gl gl.TRIANGLES cnt gl.UNSIGNED_SHORT 0))))
  (js/requestAnimationFrame #(advanced-render canvas props
                               (assoc state :then now :now (* % 0.0001)))))

;; balls-3d

(defn balls-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        {:keys [positions normals texcoords indices]}
        (primitives/sphere {:radius 10 :subdivisions-axis 48 :subdivisions-height 24})
        program (ex/create-program gl
                  data/balls-3d-vertex-shader-source
                  data/balls-3d-fragment-shader-source)
        *buffers (delay
                   (ex/create-buffer gl program "a_position"
                     (-> positions clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_normal"
                     (-> normals clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_texCoord"
                     (-> texcoords clj->js js/Float32Array.) {:size 2})
                   (ex/create-index-buffer gl
                     (-> indices clj->js js/Uint16Array.)))
        vao (ex/create-vao gl *buffers)
        props {:gl gl
               :program program
               :vao vao
               :cnt @*buffers
               :uniforms {:light-world-pos (.getUniformLocation gl program "u_lightWorldPos")
                          :view-inverse (.getUniformLocation gl program "u_viewInverse")
                          :light-color (.getUniformLocation gl program "u_lightColor")
                          :world-view-projection (.getUniformLocation gl program "u_worldViewProjection")
                          :world (.getUniformLocation gl program "u_world")
                          :world-inverse-transpose (.getUniformLocation gl program "u_worldInverseTranspose")
                          :color (.getUniformLocation gl program "u_color")
                          :specular (.getUniformLocation gl program "u_specular")
                          :shininess (.getUniformLocation gl program "u_shininess")
                          :specular-factor (.getUniformLocation gl program "u_specularFactor")}
               :objects (vec
                          (for [i (range 100)]
                            {:tz (rand 150)
                             :rx (rand (* 2 js/Math.PI))
                             :ry (rand js/Math.PI)
                             :mat-uniforms {:u_color (array (rand) (rand) (rand) 1)
                                            :u_specular        (array 1, 1, 1, 1)
                                            :u_shininess       (rand 500)
                                            :u_specularFactor  (rand 1)}}))}
        state {:then 0
               :now 0}]
    (advanced-render canvas props state)))

(defexample iglu.examples-advanced/balls-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-advanced/balls-3d-init)))

;; planes-3d

(defn planes-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        {:keys [positions normals texcoords indices]}
        (primitives/plane {:width 20 :depth 20})
        program (ex/create-program gl
                  data/balls-3d-vertex-shader-source
                  data/balls-3d-fragment-shader-source)
        *buffers (delay
                   (ex/create-buffer gl program "a_position"
                     (-> positions clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_normal"
                     (-> normals clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_texCoord"
                     (-> texcoords clj->js js/Float32Array.) {:size 2})
                   (ex/create-index-buffer gl
                     (-> indices clj->js js/Uint16Array.)))
        vao (ex/create-vao gl *buffers)
        props {:gl gl
               :program program
               :vao vao
               :cnt @*buffers
               :uniforms {:light-world-pos (.getUniformLocation gl program "u_lightWorldPos")
                          :view-inverse (.getUniformLocation gl program "u_viewInverse")
                          :light-color (.getUniformLocation gl program "u_lightColor")
                          :world-view-projection (.getUniformLocation gl program "u_worldViewProjection")
                          :world (.getUniformLocation gl program "u_world")
                          :world-inverse-transpose (.getUniformLocation gl program "u_worldInverseTranspose")
                          :color (.getUniformLocation gl program "u_color")
                          :specular (.getUniformLocation gl program "u_specular")
                          :shininess (.getUniformLocation gl program "u_shininess")
                          :specular-factor (.getUniformLocation gl program "u_specularFactor")}
               :objects (vec
                          (for [i (range 100)]
                            {:tz (rand 150)
                             :rx (rand (* 2 js/Math.PI))
                             :ry (rand js/Math.PI)
                             :mat-uniforms {:u_color (array (rand) (rand) (rand) 1)
                                            :u_specular        (array 1, 1, 1, 1)
                                            :u_shininess       (rand 500)
                                            :u_specularFactor  (rand 1)}}))}
        state {:then 0
               :now 0}]
    (advanced-render canvas props state)))

(defexample iglu.examples-advanced/planes-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-advanced/planes-3d-init)))

;; cubes-3d

(defn cubes-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        {:keys [positions normals texcoords indices]}
        (primitives/cube {:size 20})
        program (ex/create-program gl
                  data/balls-3d-vertex-shader-source
                  data/balls-3d-fragment-shader-source)
        *buffers (delay
                   (ex/create-buffer gl program "a_position"
                     (-> positions clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_normal"
                     (-> normals clj->js js/Float32Array.) {:size 3})
                   (ex/create-buffer gl program "a_texCoord"
                     (-> texcoords clj->js js/Float32Array.) {:size 2})
                   (ex/create-index-buffer gl
                     (-> indices clj->js js/Uint16Array.)))
        vao (ex/create-vao gl *buffers)
        props {:gl gl
               :program program
               :vao vao
               :cnt @*buffers
               :uniforms {:light-world-pos (.getUniformLocation gl program "u_lightWorldPos")
                          :view-inverse (.getUniformLocation gl program "u_viewInverse")
                          :light-color (.getUniformLocation gl program "u_lightColor")
                          :world-view-projection (.getUniformLocation gl program "u_worldViewProjection")
                          :world (.getUniformLocation gl program "u_world")
                          :world-inverse-transpose (.getUniformLocation gl program "u_worldInverseTranspose")
                          :color (.getUniformLocation gl program "u_color")
                          :specular (.getUniformLocation gl program "u_specular")
                          :shininess (.getUniformLocation gl program "u_shininess")
                          :specular-factor (.getUniformLocation gl program "u_specularFactor")}
               :objects (vec
                          (for [i (range 100)]
                            {:tz (rand 150)
                             :rx (rand (* 2 js/Math.PI))
                             :ry (rand js/Math.PI)
                             :mat-uniforms {:u_color (array (rand) (rand) (rand) 1)
                                            :u_specular        (array 1, 1, 1, 1)
                                            :u_shininess       (rand 500)
                                            :u_specularFactor  (rand 1)}}))}
        state {:then 0
               :now 0}]
    (advanced-render canvas props state)))

(defexample iglu.examples-advanced/cubes-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-advanced/cubes-3d-init)))

