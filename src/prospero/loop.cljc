;; - Copyright 2020 - dkropfuntucht
(ns prospero.loop
  (:require #?(:cljs [cljs.core.async :as async]
               :clj  [clojure.core.async :as async])
            [prospero.collisions :as procoll]
            [prospero.compatibility :as procompat]
            [prospero.events :as proevent]
            [prospero.mutable :as promut]
            [prospero.pluggable-game-system :as progame]))

(defn- render-pass [game-system  objects arguments]
  (progame/clear game-system objects {})
  (doseq [obj objects]
    (progame/render-object game-system obj {})))

;;TODO: swap col-map with col-results, because col-results is what we need to use
(declare update-state)
(defn- update-state
  [mutable-snapshot event-state objects colliders no-colliders col-results [apx apy apz] current-time frame-time root-state]
  (let [col-map     (if (map? colliders)
                      colliders
                      (group-by #(:object-id (first %)) colliders))
        col-results (if (nil? col-results)
                      (reduce
                       (fn [col-results [primary secondary]]
                         (let [primary-hit       (get-in primary   [:collisions :on-hit-fn])
                               secondary-hit     (get-in secondary [:collisions :on-hit-fn])
                               current-primary   (col-results (:object-id primary) primary)
                               current-secondary (col-results (:object-id secondary) secondary)
                               mod-primary       (primary-hit current-primary current-secondary root-state)
                               mod-secondary     (secondary-hit current-secondary current-primary root-state)]
                           (if (and (contains? col-results (:object-id current-primary))
                                    (contains? col-results (:object-id current-secondary)))
                             col-results
                             (assoc col-results
                                    (:object-id current-primary) mod-primary
                                    (:object-id current-secondary) mod-secondary))))
                       {}
                       colliders)
                      col-results)

        update-fn (->> (fn [o]
                         (if (contains? o :watch-mutable-state)
                           (assoc o :watch-mutable-state mutable-snapshot)
                           o))

                       (comp (fn [o]
                               (if (and (contains? o :children)
                                        (nat-int? (count (:children o))))
                                 (assoc o :children
                                        (update-state mutable-snapshot
                                                      event-state
                                                      (:children o)
                                                      col-map
                                                      no-colliders
                                                      col-results
                                                      [(+ apx (get-in o [:translation 0]))
                                                       (+ apy (get-in o [:translation 1]))
                                                       (+ apz (get-in o [:translation 2]))]
                                                      current-time
                                                      frame-time
                                                      root-state))
                                 o)))


                       (comp (fn [o]
                               (if (contains? col-map (:object-id o))
                                 (col-results (:object-id o))
                                 o)))

                       (comp (fn [o]
                               (if (contains? no-colliders o)
                                 ((get-in o [:collisions :reset-fn]) o)
                                 o)))


                       (comp #(assoc-in % [:absolute-translation]
                                        [(+ apx (get-in % [:translation 0]))
                                         (+ apy (get-in % [:translation 1]))
                                         (+ apz (get-in % [:translation 2]))]))

                       (comp (fn [o]
                               (if (contains? o :process-event)
                                 (proevent/process-event o event-state root-state)
                                 o)))

                       (comp (fn [o]
                               (if (contains? o :update-state)
                                 ((:update-state o)
                                  o
                                  current-time
                                  frame-time
                                  (when (contains? o :watch-root-state) root-state))
                                 o))))]
    (mapv
     (fn [object]
       (update-fn object))

     objects)))

(defn start-game
  "This starts the game loop.  As a rule, use `prospero.core/start-game` rather than this
   fn directly."
  [{:keys [::frame-delay]
    :or {frame-delay 25}
    :as game-system}
   objects]

  (let [running-id  (gensym)
        update-chan (async/chan 2)
        time-chan   (async/chan 2)
        first-run   (update-state (promut/current-state)
                                  {:keyboard-held-state #{}
                                   :signal-state   #{}}
                                  objects
                                  []
                                  #{}
                                  nil
                                  [0 0 0]
                                  (procompat/get-time)
                                  frame-delay
                                  objects)]
    (async/go
      (async/>! update-chan first-run)
      (async/>! time-chan (procompat/get-time)))
    (promut/set-state! [:running-loop-id] running-id)
    (async/go-loop []
      (let [obj          (async/<! update-chan)
            last-time    (async/<! time-chan)
            current-time (procompat/get-time)
            key-state    (progame/sample-keyboard-state game-system {})
            mouse-state  (progame/sample-mouse-state game-system {})

            ;;TODO: should collision detection happen after the render?
            to-collide   (procoll/collect-colliders #{} obj)
            colliders    (->> to-collide
                              (procoll/collision-detection)
                              (mapcat identity)
                              (remove nil?)
                              distinct)
            who-collide  (->> colliders
                              (map flatten)
                              flatten
                              set)
            no-collide   (->> to-collide
                              (filter #(some? (get-in % [:collisions :reset-fn])))
                              (remove #(contains? who-collide %))
                              set)
            frame-time   (- current-time last-time)
            sleep-time   (max (- frame-delay frame-time)
                              0)
            _            (async/<! (async/timeout sleep-time))]

        (render-pass game-system obj {})

        (async/>! update-chan (update-state (promut/current-state)
                                            (-> key-state
                                                (assoc :mouse-state mouse-state)
                                                (merge {:signal-state (proevent/gather-signals!)}))
                                            obj
                                            colliders
                                            no-collide
                                            nil
                                            [0 0 0]
                                            current-time
                                            (max frame-time frame-delay)
                                            obj))
        (async/>! time-chan current-time)

        ;;TODO: wtf? wtf is this for?
        ;; - exit game loop if captured name is different from running name
        (when (= running-id  (promut/current-state-in [:running-loop-id]))
          (recur))))))
