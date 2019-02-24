(ns iglu.primitives
  (:require [clojure.spec.alpha :as s]
            [iglu.examples :as ex]))

(defn plane [{:keys [width depth subdivisions-width subdivisions-depth]
              :or {width 1 depth 1
                   subdivisions-width 1 subdivisions-depth 1}}]
  (let [num-verts-across (inc subdivisions-width)]
    (-> (fn [m z]
          (reduce
            (fn [m x]
              (let [u (/ x subdivisions-width)
                    v (/ z subdivisions-depth)]
                (-> m
                    (update :positions (fn [positions]
                                         (-> positions
                                             (conj! (- (* width u) (* width 0.5)))
                                             (conj! 0)
                                             (conj! (- (* depth v) (* depth 0.5))))))
                    (update :normals (fn [normals]
                                       (-> normals
                                           (conj! 0)
                                           (conj! 1)
                                           (conj! 0))))
                    (update :texcoords (fn [texcoords]
                                         (-> texcoords
                                             (conj! u)
                                             (conj! v)))))))
            m
            (range (inc subdivisions-width))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range (inc subdivisions-depth)))
        (assoc :indices
          (reduce
            (fn [indices z]
              (reduce
                (fn [indices x]
                  (-> indices
                      ;; triangle 1
                      (conj! (-> z (* num-verts-across) (+ x)))
                      (conj! (-> z inc (* num-verts-across) (+ x)))
                      (conj! (-> z (* num-verts-across) (+ (inc x))))
                      ;; triangle 2
                      (conj! (-> z inc (* num-verts-across) (+ x)))
                      (conj! (-> z inc (* num-verts-across) (+ (inc x))))
                      (conj! (-> z (* num-verts-across) (+ (inc x))))))
                indices
                (range subdivisions-width)))
            (transient [])
            (range subdivisions-depth)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/def ::radius number?)
(s/def ::subdivisions-axis pos?)
(s/def ::subdivisions-height pos?)
(s/fdef sphere
  :args (s/cat :props (s/keys :req-un [::radius ::subdivisions-axis ::subdivisions-height])))

(defn sphere [{:keys [radius subdivisions-axis subdivisions-height]}]
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
        (assoc :indices
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
            (range subdivisions-height)))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(def cube-face-indices
  [; right
   [3 7 5 1]
   ; left
   [6 2 0 4]
   [6 7 3 2]
   [0 1 5 4]
   ; front
   [7 6 4 5]
   ; back
   [2 3 1 0]])

(defn cube [{:keys [size] :or {size 1}}]
  (let [k (/ size 2)
        corner-vertices [[(- k) (- k) (- k)]
                         [k (- k) (- k)]
                         [(- k) k (- k)]
                         [k k (- k)]
                         [(- k) (- k) k]
                         [k (- k) k]
                         [(- k) k k]
                         [k k k]]
        face-normals [[1 0 0]
                      [-1 0 0]
                      [0 1 0]
                      [0 -1 0]
                      [0 0 1]
                      [0 0 -1]]
        uv-coords [[1 0]
                   [0 0]
                   [0 1]
                   [1 1]]
        num-vertices (* 6 4)]
    (-> (fn [m f]
          (let [face-indices (nth cube-face-indices f)
                offset (* 4 f)]
            (-> (fn [m v]
                  (-> m
                      (update :positions (fn [positions]
                                           (let [[x y z] (nth corner-vertices (nth face-indices v))]
                                             (-> positions
                                                 (conj! x)
                                                 (conj! y)
                                                 (conj! z)))))
                      (update :normals (fn [normals]
                                         (let [[x y z] (nth face-normals f)]
                                           (-> normals
                                               (conj! x)
                                               (conj! y)
                                               (conj! z)))))
                      (update :texcoords (fn [texcoords]
                                           (let [[u v] (nth uv-coords v)]
                                             (-> texcoords
                                                 (conj! u)
                                                 (conj! v)))))))
                (reduce m (range 4))
                (update :indices (fn [indices]
                                   (-> indices
                                       (conj! (+ offset 0))
                                       (conj! (+ offset 1))
                                       (conj! (+ offset 2))
                                       (conj! (+ offset 0))
                                       (conj! (+ offset 2))
                                       (conj! (+ offset 3))))))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])
           :indices (transient [])}
          (range 6))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

(s/def ::bottom-radius number?)
(s/def ::top-radius number?)
(s/def ::radial-subdivisions #(>= % 3))
(s/def ::vertical-subdivisions #(>= % 1))
(s/def ::top-cap? boolean?)
(s/def ::bottom-cap? boolean?)
(s/fdef truncated-cone
  :args (s/cat :props (s/keys
                        :req-un [::bottom-radius ::top-radius
                                 ::radial-subdivisions ::vertical-subdivisions]
                        :opt-un [::top-cap? ::bottom-cap?])))

(defn truncated-cone [{:keys [bottom-radius top-radius height
                              radial-subdivisions vertical-subdivisions
                              top-cap? bottom-cap?]
                       :or {top-cap? true bottom-cap? true}}]
  (let [extra (+ (if top-cap? 2 0) (if bottom-cap? 2 0))
        num-vertices (* (inc radial-subdivisions)
                        (+ vertical-subdivisions 1 extra))
        verts-around-edge (inc radial-subdivisions)
        slant (js/Math.atan2 (- bottom-radius top-radius) height)
        cos-slant (js/Math.cos slant)
        sin-slant (js/Math.sin slant)
        start (if top-cap? -2 0)
        end (+ vertical-subdivisions (if bottom-cap? 2 0))]
    (-> (fn [m yy]
          (let [v (/ yy vertical-subdivisions)
                y (* height v)
                [y v ring-radius]
                (cond
                  (< yy 0)
                  [0
                   1
                   bottom-radius]
                  (> yy vertical-subdivisions)
                  [height
                   1
                   top-radius]
                  :else
                  [y
                   v
                   (+ bottom-radius
                      (* (- top-radius bottom-radius)
                         (/ yy vertical-subdivisions)))])
                [y v ring-radius]
                (if (or (= yy -2)
                        (= yy (+ vertical-subdivisions 2)))
                  [y 0 0]
                  [y v ring-radius])
                y (- y (/ height 2))]
            (reduce
              (fn [m ii]
                (let [sin (js/Math.sin (-> ii (* js/Math.PI) (* 2) (/ radial-subdivisions)))
                      cos (js/Math.cos (-> ii (* js/Math.PI) (* 2) (/ radial-subdivisions)))]
                  (-> m
                      (update :positions (fn [positions]
                                           (-> positions
                                               (conj! (* sin ring-radius))
                                               (conj! y)
                                               (conj! (* cos ring-radius)))))
                      (update :normals (fn [normals]
                                         (-> normals
                                             (conj! (if (or (< yy 0)
                                                            (> yy vertical-subdivisions))
                                                      0
                                                      (* sin cos-slant)))
                                             (conj! (if (< yy 0)
                                                      -1
                                                      (if (> yy vertical-subdivisions)
                                                        1
                                                        sin-slant)))
                                             (conj! (if (or (< yy 0)
                                                            (> yy vertical-subdivisions))
                                                      0
                                                      (* cos cos-slant))))))
                      (update :texcoords (fn [texcoords]
                                           (-> texcoords
                                               (conj! (/ ii radial-subdivisions))
                                               (conj! (- 1 v))))))))
              
              m
              (range verts-around-edge))))
        (reduce
          {:positions (transient [])
           :normals (transient [])
           :texcoords (transient [])}
          (range start (inc end)))
        (assoc :indices
          (reduce
            (fn [indices yy]
              (reduce
                (fn [indices ii]
                  (-> indices
                      ;; triangle 1
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 0) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 1) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 1) (+ ii)))
                      ;; triangle 2
                      (conj! (-> verts-around-edge (* (+ yy 0)) (+ 0) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 1) (+ ii)))
                      (conj! (-> verts-around-edge (* (+ yy 1)) (+ 0) (+ ii)))))
                indices
                (range radial-subdivisions)))
            (transient [])
            (range (+ vertical-subdivisions extra))))
        (update :positions persistent!)
        (update :normals persistent!)
        (update :texcoords persistent!)
        (update :indices persistent!))))

