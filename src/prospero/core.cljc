;; - Copyright 2020 - dkropfuntucht
(ns prospero.core
  "Clojure game engine with plugable renderer options.
   Check out the docs `prospero.game-objects` for a good place to start."
  (:require [prospero.audio :as proaudio]
            [prospero.loop :as proloop]
            [prospero.pluggable-game-system :as progame]))

(defn start-game
  "Call this to start your game and pass control over to Prospero's game loop.
   `objects` should be a vector containing 1 or more roots for the game state.
   `objects` are best built using `prospero.game-objects` functions.
  `game-system` is a map that should include, at a minimum, the key
  `:prospero.core/game-system` that should point to the plugable game system
  to be used for rendering.  You can set the desired minimum frame time with
  `prospero.loop/frame-delay` in the `game-system` options, too. "
  [game-system objects]
  (proloop/start-game (-> game-system
                          (progame/initial-setup! {})
                          (progame/initialize-audio! {})
                          (proaudio/initial-register-audio-tracks!))
                      objects))
