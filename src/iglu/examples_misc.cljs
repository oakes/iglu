(ns iglu.examples-misc
  (:require [iglu.examples :as ex]
            [goog.events :as events]
            [iglu.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; balls-3d

(defn balls-3d-render [canvas
                       {:keys [gl program vao cnt objects]
                        {:keys [light-world-pos
                                view-inverse
                                light-color
                                world-view-projection
                                world
                                world-inverse-transpose
                                specular
                                shininess
                                specular-factor]}
                        :uniforms
                        :as props}
                       {:keys [then now] :as state}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
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
    (doseq [{:keys [radius rx ry mat-uniforms]} objects]
      (let [world-matrix (->> (ex/identity-matrix-3d)
                              (ex/multiply-matrices 4 (ex/x-rotation-matrix-3d (* rx now)))
                              (ex/multiply-matrices 4 (ex/y-rotation-matrix-3d (* ry now)))
                              (ex/multiply-matrices 4 (ex/translation-matrix-3d 0 0 radius)))]
        (.uniformMatrix4fv gl world false world-matrix)
        (.uniformMatrix4fv gl world-view-projection false
          (->> world-matrix
               (ex/multiply-matrices 4 view-projection-matrix)))
        (.uniformMatrix4fv gl world-inverse-transpose false
          (->> world-matrix
               (ex/inverse-matrix 4)))
        (.uniform4fv gl specular (:specular mat-uniforms))
        (.uniform1f gl shininess (:shininess mat-uniforms))
        (.uniform1f gl specular-factor (:specular-factor mat-uniforms))
        (.drawElements gl gl.TRIANGLES 6912 gl.UNSIGNED_SHORT 0))))
  #_
  (js/requestAnimationFrame #(balls-3d-render canvas props
                               (-> state
                                   (assoc :then now :now (* % 0.001))))))

(defn balls-3d-init [canvas]
  (let [gl (.getContext canvas "webgl2")
        vertices (js/twgl.primitives.createSphereVertices 10 48 24)
        program (ex/create-program gl
                  data/balls-3d-vertex-shader-source
                  data/balls-3d-fragment-shader-source)
        vao (let [vao (.createVertexArray gl)]
              (.bindVertexArray gl vao)
              vao)
        cnt (ex/create-buffer gl program "a_position" vertices.position {:size 3})
        _ (ex/create-buffer gl program "a_normal" vertices.normal {:size 3})
        ;_ (ex/create-buffer gl program "a_texcoord" vertices.texcoord {:size 2})
        props {:gl gl
               :program program
               :vao vao
               :cnt cnt
               :uniforms {:light-world-pos (.getUniformLocation gl program "u_lightWorldPos")
                          :view-inverse (.getUniformLocation gl program "u_viewInverse")
                          :light-color (.getUniformLocation gl program "u_lightColor")
                          :world-view-projection (.getUniformLocation gl program "u_worldViewProjection")
                          :world (.getUniformLocation gl program "u_world")
                          :world-inverse-transpose (.getUniformLocation gl program "u_worldInverseTranspose")
                          ;:color-mult (.getUniformLocation gl program "u_colorMult")
                          ;:diffuse (.getUniformLocation gl program "u_diffuse")
                          :specular (.getUniformLocation gl program "u_specular")
                          :shininess (.getUniformLocation gl program "u_shininess")
                          :specular-factor (.getUniformLocation gl program "u_specularFactor")}
               :objects (vec
                          (for [i (range 100)]
                            {:radius (rand 150)
                             :rx (rand (* 2 js/Math.PI))
                             :ry (rand js/Math.PI)
                             :mat-uniforms {:specular        (array 1, 1, 1, 1)
                                            :shininess       (rand 500)
                                            :specular-factor (rand 1)}}))}
        state {:then 0
               :now 0}]
    (balls-3d-render canvas props state)))

(defexample iglu.core/balls-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-misc/balls-3d-init)))

