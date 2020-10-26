;; - Copyright 2020 - dkropfuntucht
(ns prospero.pluggable-game-system
  "This namespace provides the extension points to connect
  other game systems to *Prospero*.  More documentation will
  follow on the 1.0.0 timeframe.")

(defmulti initial-setup!
  (fn [{:keys [:prospero.core/game-system] :as game-system} arg-map]
    game-system))

(defmulti clear
  (fn [{:keys [:prospero.core/game-system] :as game-system} object arg-map]
    game-system))

(defmulti render-object
  (fn [{:keys [:prospero.core/game-system] :as game-system} object arg-map]
    game-system))

(defmulti sample-keyboard-state
  (fn [{:keys [:prospero.core/game-system] :as game-system} object arg-map]
    game-system))

(defmulti sample-mouse-state
  (fn [{:keys [:prospero.core/game-system] :as game-system} object arg-map]
    game-system))

(defmulti initialize-audio!
  (fn [{:keys [:prospero.core/game-system] :as game-system} arg-map]
    game-system))

(defmulti register-audio-track!
  (fn [{:keys [:prospero.core/game-system] :as game-system}
       track-name
       track-source
       opts]
    game-system))

(defmulti queue-audio-track!
  (fn [{:keys [:prospero.core/game-system] :as game-system} track-name opts]
    game-system))

(defmulti stop-audio-track!
  (fn [{:keys [:prospero.core/game-system] :as game-system} track-name opts]
    game-system))
