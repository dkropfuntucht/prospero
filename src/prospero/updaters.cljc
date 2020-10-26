;; - Copyright 2020 - dkropfuntucht
(ns prospero.updaters
  "Llkely to be entirely replaced by the animation system,
  or mostly moved into math."
  (:require [prospero.math :as promath]))

;; - common functions to update the state of game objects

(defn update-position-from-rotation-2d
  [object speed]
  (let [rot       (:rotation object)
        quad      (promath/quadrant-number rot)
        dy        (Math/cos rot)
        dy-sqr    (promath/square dy)
        dx        (Math/sqrt (- 1 dy-sqr))
        x-sign    ({1  1
                    2 -1
                    3 -1
                    4  1} quad)
        y-sign    -1]
    (-> object
        (update-in [:translation 0] + (* speed dx x-sign))
        (update-in [:translation 1] + (* speed dy y-sign)))))

(defn update-position-2d
  ([{:keys [translation] :as object} [tx ty] step]
   (let [[x y] translation]
     (cond-> object
       (> x tx)
       (update-in [:translation 0] - step)

       (< x tx)
       (update-in [:translation 0] + step)

       (> y ty)
       (update-in [:translation 1] - step)

       (< y ty)
       (update-in [:translation 1] + step))))
  ([object target]
   (update-position-2d object target 1)))
