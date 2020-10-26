;; - Copyright 2020 - dkropfuntucht
(ns prospero.collisions
  "This is all internal routines for the collision detection system.")

(declare collect-colliders)
(defn collect-colliders
  [accl objects]
  (filter
   #(contains? % :collisions)
   (reduce
    (fn [accl o]
      (if (contains? o :children)
        (collect-colliders (conj accl o) (:children  o))
        (conj accl o)))
    accl
    objects)))

(defn collision-intersects?
  [col-a col-b]
  (let [abounds       (get-in col-a [:collisions :bounds-box])
        [abx aby abz] abounds
        bbounds       (get-in col-b [:collisions :bounds-box])
        [bbx bby bbz] bbounds
        aoffs         (get-in col-a [:collisions :offset])
        [oax oay oaz] aoffs
        boffs         (get-in col-b [:collisions :offset])
        [obx oby obz] boffs
        aloc          (get-in col-a [:absolute-translation])
        [ax ay az]    aloc
        bloc          (get-in col-b [:absolute-translation])
        [bx by bz]    bloc
        ax            (when (number? ax) (+ ax oax))
        ay            (when (number? ay) (+ ay oay))
        az            (when (number? az) (+ az oaz))
        bx            (when (number? bx) (+ bx obx))
        by            (when (number? by) (+ by oby))
        bz            (when (number? bz) (+ bz obz))]
    (if (not= col-a col-b)
      (if (or (nil? ax) (nil? ay) (nil? bx) (nil? by)
              (nil? abx) (nil? aby) (nil? bbx) (nil? bby))
        false
        (if (or (and (>= ax bx) (<= ax (+ bx bbx)))
                (and (>= (+ ax abx) bx) (<= ax (+ bx bbx))))
          (if (or (and (>= ay by) (<= ay (+ by bby)))
                  (and (>= (+ ay aby) by) (<= ay (+ by bby))))
            true
            false)
          false))
      false)))

(defn collision-detection
  [colliders]
  (map
   (fn [c]
     (let [{:keys [bounds-box level-set match-set on-hit-fn]} (:collisions c)
           to-test (filter
                    (fn [dc]
                      (some #(contains? match-set %) (get-in dc [:collisions :level-set])))
                    colliders)
           col-res (map
                    (fn [oc]
                      (if (collision-intersects? c oc)
                        [c oc]
                        nil))
                    to-test)]
       col-res))
   colliders))
