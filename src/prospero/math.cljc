;; - Copyright 2020 - dkropfuntucht
(ns prospero.math
  "Some useful math functions - more to come around 1.0.0")

(def pi Math/PI)

(defn degrees-to-radians
  [theta]
  (/ (* pi theta)
     180.0))

(defn radians-to-degrees
  [theta]
  (* (/ 180.0 pi)
     theta))

(defn quadrant-number
  [theta]
  (let [deg (radians-to-degrees theta)
        deg (if (neg? deg)
              (+ 360 deg)
              deg)
        deg (loop [deg deg]
              (if (< deg 361)
                deg
                (recur (- deg 360))))]
    (cond (> deg 270)
          2
          (> deg 180)
          3
          (> deg 90)
          4
          :or
          1)))

(defn square [n]
  (* n n))
