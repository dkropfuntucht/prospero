;; - Copyright 2020 - dkropfuntucht
(ns prospero.constraints
  "Constraints provide a set of common state updating functions
  that can be used to control object behaviour.
  ```
  ::pospero.constraints/keep-on-screen ; - can keep the player object on screen
  ```"
  (:require [prospero.events :as proevent]))

(defmulti get-constraint-fn
  "This can be extended to add constrain functions."
  (fn [the-keyword object] the-keyword))

(defmethod get-constraint-fn ::keep-on-screen
  [_ {:keys [game-system width height] :as object}]
  (let [{:keys [display-width display-height]} game-system]
    (fn [obj current-time frame-time _]
      (let [[x y _] (:translation obj)]
        (cond-> obj
          (< x 0)
          (assoc-in [:translation 0] 0)

          (< y 0)
          (assoc-in [:translation 1] 0)

          (> (+ x width) display-width)
          (assoc-in [:translation 0] (- display-width width))

          (> (+ y height) display-height)
          (assoc-in [:translation 1] (- display-height height)))))))
