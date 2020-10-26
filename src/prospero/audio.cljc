;; - Copyright 2020 - dkropfuntucht
(ns prospero.audio
  "This provides controls for the audio system.  It's
   still pretty limited right now."
  (:require [prospero.pluggable-game-system :as progame]))

;; - options that should be supported
::emit-signal-on-end

(defn register-audio-track!
  "This registers a sound to play.  This will likely always necessary for
   web games.  Audio tracks can be registerd when the game-system is created
   like this:
  ```
  (procore/start-game
  {::procore/game-system   ::iris/game-system
   ::proaudio/audio-assets {\"track-name\" [\"/sounds/sound.wav\" {}]
                            \"next-track\" [\"/sounds/another.mp3\" {}]}})
  ```"
  [game-system track-name track-source opts]
  (progame/register-audio-track! game-system track-name track-source opts))

(defn queue-audio-track!
  "Start playing the named track.
  An option `::prospero.audio/emit-signal-on-end` can be used to send a signal
  when a sound completes playing.  This can be used to set up loops, for
  instance."
  [game-system track-name opts]
  (progame/queue-audio-track! game-system track-name opts))

(defn stop-audio-track!
  "Force a playing sound to stop."
  [game-system track-name opts]
  (progame/stop-audio-track! game-system track-name opts))

(defn initial-register-audio-tracks!
  "Used by the system to intialize the sounds at startup."
  [{:keys [::audio-assets] :as game-system}]
  (doseq [[track-name [source opts]] audio-assets]
    (register-audio-track! game-system track-name source opts))
  game-system)
