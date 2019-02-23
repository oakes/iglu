(ns iglu.primitives
  (:require [clojure.spec.alpha :as s]))

(s/fdef sphere
  :args (s/cat :radius number? :axis pos? :height pos?))

(defn sphere [radius subdivisions-axis subdivisions-height]
  (let [start-latitude 0
        end-latitude js/Math.PI
        start-longitude 0
        end-longitude (* 2 js/Math.PI)
        lat-range (- end-latitude start-latitude)
        long-range (- end-longitude start-longitude)
        num-vertices (* (inc subdivisions-axis) (inc subdivisions-height))
        num-verts-around (inc subdivisions-axis)]
    (-> (fn [m y]
          (reduce
            (fn [m x]
              (let [u (/ x subdivisions-axis)
                    v (/ y subdivisions-height)
                    theta (+ (* long-range u) start-longitude)
                    phi (+ (* lat-range v) start-latitude)
                    sin-theta (js/Math.sin theta)
                    cos-theta (js/Math.cos theta)
                    sin-phi (js/Math.sin phi)
                    cos-phi (js/Math.cos phi)
                    ux (* cos-theta sin-phi)
                    uy cos-phi
                    uz (* sin-theta sin-phi)]
                (-> m
                    (update :positions (fn [positions]
                                         (-> positions
                                             (conj! (* radius ux))
                                             (conj! (* radius uy))
                                             (conj! (* radius uz)))))
                    (update :normals (fn [normals]
                                       (-> normals
                                           (conj! ux)
                                           (conj! uy)
                                           (conj! uz))))
                    (update :texcoords (fn [texcoords]
                                         (-> texcoords
                                             (conj! (- 1 u))
                                             (conj! v)))))))
            m
            (range (inc subdivisions-axis))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range (inc subdivisions-height)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (assoc :indices
          (persistent!
            (reduce
              (fn [indices y]
                (reduce
                  (fn [indices x]
                    (-> indices
                        ;; triangle 1
                        (conj! (-> y (* num-verts-around) (+ x)))
                        (conj! (-> y (* num-verts-around) (+ (inc x))))
                        (conj! (-> y inc (* num-verts-around) (+ x)))
                        ;; triangle 2
                        (conj! (-> y inc (* num-verts-around) (+ x)))
                        (conj! (-> y (* num-verts-around) (+ (inc x))))
                        (conj! (-> y inc (* num-verts-around) (+ (inc x))))))
                  indices
                  (range subdivisions-axis)))
              (transient [])
              (range subdivisions-height)))))))

