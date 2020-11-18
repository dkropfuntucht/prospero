;; - Copyright 2020 - dkropfuntucht
(ns prospero.animation
  "This namespace provides the core of the animation system.
   These can be added to game-objects with `prospero.game-objects/add-animators`.
  Animations can be specified like this:
  ```
  [path-vector change-desription tag options]
  ```
  `path-vector` is simply a vector of keys pointing to where the value will
  be changed.  The change-description is used to control the change value.
  In the future, there will be a number of other ways to declare animations
  and provide custom functions to specify the animation curve. Tag is a spare
  parameter that is likely going to be removed.  Options are optional, but
  can add extra behaviours to the animation.

  A value can be changed or set immediately with a description in this format:
  ```
  [[:path :to :target] [[number unit]] :set opts]
  ```
  For example to set the column in a sprite animation:
  ```
  [[:texture :col] [[3 :col-index]] :set]
  ```
  A constant rate of change can be specified this way:
  ```
  [[:translation 0] [[20 :pixels] [1 :second]] :constant]
  ```
  Keys for options include:
  ```
  :animator-id    ; - this name can be used to control animation state via events
  :intitial-state ; - :running or :stopped
  :limit          ; - :constant animation will end here - will deprcate in future
  :on-limit       ; - :signal or :reset
  :limit-signal   ; - will send this signal when limit is hit
  ```
  NB: tag is mostly a free parameter right now, and might
  be removed in a future release. "
  (:require [prospero.events :as proevent]))

(comment
  "TODO/Future direction
  Ways to specify animations:
  `[[target-value unit]] ; - immediate set to a specific value (usually on a trigger)
  [[target-value unit] [duration time-unit]] ; - move to target value in time
   [[target-value unit] [change-value unit] [:duration time-unit]] ; - move to target value by change-value
   [[target-value unit] :curve-specifier [:duration time-unit]] ; - move with a curve
   [nil [change-value unit] [:duration time-unit]] ; - move by change-value by duration as a rate
  [[lower-bound unit] [change-value unit] [upper-bound unit] [duration time-unit]] ; - move between values`")

(defmulti calculate-change
  "Internal multi-method for handling time units in the animator and determing
   what the rate of change should be.
   Don't extend this, as it will likely change a lot in future versions.  "
  (fn [change unit duration time-unit frame-time] time-unit))

(defmethod calculate-change :second
  [change unit duration time-unit frame-time]
  (/ change (/ (* duration 1000) frame-time)))

(defmethod calculate-change :seconds
  [change unit duration time-unit frame-time]
  (/ change (/ (* duration 1000) frame-time)))

(defmethod calculate-change :ms
  [change unit duration time-unit frame-time]
  (/ change (/ duration frame-time)))

(defmethod calculate-change :frame
  [change unit duration time-unit frame-time]
  (/ change duration))

(defmethod calculate-change :frames
  [change unit duration time-unit frame-time]
  (/ change duration))

(defmulti animator
  "This can be extended to provide new animation controls."
  (fn [data-path specification control opts]
    (let [typing (cond (vector? specification)
                       :complex

                       (number? specification)
                       :number

                       :or
                       :unknown)]
      [typing
       (if (= :complex typing)
         (count specification)
         1)])))

(defmethod animator [:complex 1]
  [data-path
   [[change unit]]
   curve
   {:keys [animator-id
           initial-state
           initial-value
           active-global-modes] :as extended-args
    :or {animator-id   (gensym "animator")
         initial-state :running}}]
  (fn [obj current-time frame-time root-state global-mode]
    (let [state (get-in obj [:animators animator-id :state])]
      (if (and (not= initial-state :running) (nil? state))
        (assoc-in obj [:animators animator-id :state] initial-state)
        (let [state     (get-in obj [:animators animator-id :state])
              cur-value (get-in obj data-path initial-value)]
          (cond-> obj
            (nil? state)
            (assoc-in [:animators animator-id :state] initial-state)

            (and (= state :running) (not= change cur-value))
            (assoc-in data-path change)

            (and (= state :running) (not= change cur-value))
            (assoc-in [:animators animator-id :state] :stopped)))))))

(defmethod animator [:complex 2]
  ;;TODO: this doc specifies the way forward, but not the final version
  [data-path
   [[change unit] [duration time-unit]]
   curve
   {:keys [animator-id
           custom-fn
           domain
           initial-state
           initial-value
           limit
           limit-signal
           on-limit
           active-global-modes] :as extended-args
    :or {animator-id   (gensym "animator")
         initial-state :running}}]
  (if (some? custom-fn)
    custom-fn
    (fn [obj current-time frame-time root-state global-mode]
      (let [state (get-in obj [:animators animator-id :state])]
        (if (and (not= initial-state :running) (nil? state))
          (assoc-in obj [:animators animator-id :state] initial-state)
          (let [state     (get-in obj [:animators animator-id :state])
                cur-value (if (= domain :integers)
                            (get-in obj [:animators animator-id :scratch] initial-value)
                            (get-in obj data-path initial-value))

                cur-value (if (and (nil? cur-value) (some? initial-value))
                            initial-value
                            cur-value)

                c-value   (calculate-change change unit duration time-unit frame-time)
                u-value   (+ cur-value c-value)
                f-value   (cond (nil? limit)
                                u-value

                                (and (number? limit)
                                     (> u-value limit)
                                     (= on-limit :reset))
                                initial-value

                                (and (number? limit)
                                     (> u-value limit)
                                     (= on-limit :signal))
                                (do
                                  (proevent/send-signal! limit-signal obj)
                                  limit)

                                :or
                                u-value)]

            (if (and (or (nil? active-global-modes)
                         (and
                          (some? active-global-modes)
                          (contains? active-global-modes global-mode)))
                     (or (= state :running) (nil? state)))
              (cond-> (assoc-in obj data-path (if (= domain :integers)
                                                (int f-value)
                                                f-value))

                (nil? state)
                (assoc-in [:animators animator-id :state] initial-state)

                (= domain :integers)
                (assoc-in [:animators animator-id :scratch] f-value))

              obj)))))))

(defn flip
  "Given an animation spec, reverse the direction of the change
  ```
  (proanim/flip [[:translation 1] [[10 :pixels] [1 :second]] :constant])
  =>
  [[:translation 1] [[-10 :pixels] [1 :second]] :constant]

  ```"
  [animation-spec]
  (let [[[direction unit] [duration time-unit]] (nth animation-spec 1)]
    (assoc animation-spec 1 [[(* -1 direction) unit] [duration time-unit]])))
