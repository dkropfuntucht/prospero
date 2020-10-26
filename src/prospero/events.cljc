;; - Copyright 2020 - dkropfuntucht
(ns prospero.events)

;; - valid list of event-types
::keyboard-down
::keyboard-held
::keyboard-up
::signal
::mouse-down
::mouse-up

;; - mouse event codes
::mouse-button-0
::mouse-button-1

;; - meta keys
::key-any-key

;; - valid list of key-codes
::key-arrow-up
::key-arrow-down
::key-arrow-left
::key-arrow-up

::key-letter-a
::key-letter-b
::key-letter-c
::key-letter-d
::key-letter-e
::key-letter-f
::key-letter-g
::key-letter-h
::key-letter-i
::key-letter-j
::key-letter-k
::key-letter-l
::key-letter-m
::key-letter-n
::key-letter-o
::key-letter-p
::key-letter-q
::key-letter-r
::key-letter-s
::key-letter-t
::key-letter-u
::key-letter-v
::key-letter-w
::key-letter-x
::key-letter-y
::key-letter-z


(def signals (atom {}))

(defn send-signal!
  ([custom-key arg-map]
   (swap! signals
          (fn [s]
            (if (contains? s custom-key)
              (update s custom-key conj arg-map)
              (assoc s custom-key [arg-map])))))
  ([custom-key]
   (send-signal! custom-key {})))

(defn emit-signal-on-event
  "Make a function that returns a signal - useful for filling
   out input event to signal mappings on nodes."
  [signal]
  (fn [me _]
    (send-signal! signal)
    me))

(defn do-mouse-event-in-bounds
  [the-fn]
  (fn [obj args]
    (let [[_ _ {:keys [mouse-coords]}] args
          [x y]                        mouse-coords
          [ox oy oz]                   (:absolute-translation obj)
          {:keys [width height]}       obj]
      (if (and (>= x ox)
               (>= y oy)
               (<= x (+ ox width))
               (<= y (+ oy height)))
        (the-fn obj args)
        obj))))

(defn gather-signals!
  []
  (let [sigs @signals]
    (reset! signals {})
    sigs))

(defn process-event
  [object {:keys [keyboard-down keyboard-held keyboard-up mouse-state signal-state] :as event-state} root-state]
  (reduce-kv
   (fn [obj [event-type event-code] event-fn]
     (let [event-fn (if (vector? event-fn)
                      (condp = (first event-fn)
                        ::emit-signal-on-event
                        (fn [o _] (send-signal! (second event-fn)) o)

                        ::change-animators-on-event
                        (fn [o _]
                          (reduce
                           (fn [os [as-k as-v]]
                             (assoc-in os [:animators as-k :state] as-v))
                           o
                           (second event-fn))))
                      event-fn)]

       (cond (and (= event-type ::keyboard-down)
                  (contains? keyboard-down event-code))
             (event-fn obj [event-type event-code {:keyboard-down keyboard-down
                                                   :root-state    root-state
                                                   :event-state   event-state}])

             (and (= event-type ::keyboard-held)
                  (contains? keyboard-held event-code))
             (event-fn obj [event-type event-code {:keyboard-held keyboard-held
                                                   :root-state    root-state
                                                   :event-state   event-state}])

             (and (= event-type ::keyboard-up)
                  (contains? keyboard-up event-code))
             (event-fn obj [event-type event-code {:keyboard-up keyboard-up
                                                   :root-state  root-state
                                                   :event-state event-state}])

             (and (= event-type ::mouse-down)
                  (= (:change mouse-state) ::mouse-down)
                  (contains? (:event-buttons mouse-state) event-code ))
             (event-fn obj [event-type event-code {:mouse-down   (:event-buttons mouse-state)
                                                   :mouse-coords (:mouse-coords mouse-state)
                                                   :root-state   root-state
                                                   :event-state  event-state}])

             (and (= event-type ::mouse-up)
                  (= (:change mouse-state) ::mouse-up)
                  (contains? (:event-buttons mouse-state) event-code))
             (event-fn obj [event-type event-code {:mouse-up     (:event-buttons mouse-state)
                                                   :mouse-coords (:mouse-coords mouse-state)
                                                   :root-state   root-state
                                                   :event-state  event-state}])

             (and (= event-type ::signal)
                  (contains? signal-state event-code))
             (reduce
              (fn [o sa]
                (event-fn o [event-type event-code {:signal-state signal-state
                                                    :root-state   root-state
                                                    :signal-args  sa
                                                    :event-state  event-state}]))
              obj
              (signal-state event-code))

             :or
             obj)))
   object
   (:process-event object)))
