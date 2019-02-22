(ns iglu.examples-misc
  (:require [iglu.examples :as ex]
            [goog.events :as events]
            [iglu.data :as data])
  (:require-macros [dynadoc.example :refer [defexample]]))

;; balls-3d

(defn balls-3d-render [canvas
                       {:keys [gl program vao cnt]
                        :as props}
                       {:keys [then now] :as state}]
  (ex/resize-canvas canvas)
  (.enable gl gl.CULL_FACE)
  (.enable gl gl.DEPTH_TEST)
  (.viewport gl 0 0 gl.canvas.width gl.canvas.height)
  (.clearColor gl 0 0 0 0)
  (.clear gl (bit-or gl.COLOR_BUFFER_BIT gl.DEPTH_BUFFER_BIT))
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
        base-color (rand 240)
        props {:gl gl
               :program program
               :vao vao
               :cnt cnt
               :uniforms-same-for-all {"u_lightWorldPos" (array -50 30 100)
                                       "u_viewInverse" (ex/identity-matrix-3d)
                                       "u_lightColor" (array 1 1 1 1)}
               :uniforms-computed-for-each {"u_worldViewProjection" (ex/identity-matrix-3d)
                                            "u_world" (ex/identity-matrix-3d)
                                            "u_worldInverseTranspose" (ex/identity-matrix-3d)}
               :objects (vec
                          (for [i (range 300)]
                            {:radius (rand 150)
                             :x-rotation (rand (* 2 js/Math.PI))
                             :y-rotation (rand js/Math.PI)
                             :mat-uniforms {;"u_colorMult"      chroma.hsv(rand(baseColor, baseColor + 120), 0.5, 1).gl(),
                                            ;"u_diffuse"        textures[randInt(textures.length)]
                                            "u_specular"       (array 1, 1, 1, 1)
                                            "u_shininess"      (rand 500)
                                            "u_specularFactor" (rand 1)}}))}
        state {:then 0
               :now 0}]
    (balls-3d-render canvas props state)))

(defexample iglu.core/balls-3d
  {:with-card card}
  (->> (iglu.examples/create-canvas card)
       (iglu.examples-misc/balls-3d-init)))

