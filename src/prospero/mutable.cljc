;; - Copyright 2020 - dkropfuntucht
(ns prospero.mutable)

;; - Mutable State for games - provided to the game object state tree
;; as :watch-mutable-state (when requested)

(defonce game-mutable-state (atom {}))

(defn set-state!
  "Set mutable state
  ```
  (promut/set-state! [:some :path :here] true)
  ```"
  [key-path update-value]
  (swap! game-mutable-state #(assoc-in % key-path update-value)))

(defn current-state
  "Can be used to fetch the entire mutable state.  Probably
  unwise to use this within a game-object update function.  It
  would be much better to use `(progo/watch-mutable)`."
  []
  @game-mutable-state)

(defn current-state-in
  "Can be used to fetch a portion of the mutable state.  Probably
  unwise to use this within a game-object update function.  It
  would be much better to use `(progo/watch-mutable)`."
  ([key-path]
   (get-in (current-state) key-path))
  ([key-path default]
   (get-in (current-state) key-path  default)))
