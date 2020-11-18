;; - Copyright 2020 - dkropfuntucht
(ns prospero.game-objects
  "This has collected convenience functions for building up game objects."
  (:require [prospero.animation :as proanim]
            [prospero.constraints :as proconstraint]))

(defn base-object
  "Create the base map for a game-object.  Most game-objects will be built
   with this pattern:
   ```
   (-> (progo/base-object game-system)
       ...)
   ```"
  [game-system]
  {:game-system game-system
   :object-id   (gensym)
   :children    []})

(defn render-status
  "Setting render-status to `:hidden` will hide it from display.
   Other `status` values will currently display the object.
   Available status values and behaviours may change with the targeted
   plug-able game system.
  ```
  (-> (progo/base-object game-system)
      (progo/render-status :hidden)) ; - hides the object from the display
  ```"
  [object status]
  (merge object
         {:render-status status}))

(defn debug
  "Instruct this object to dump some of its details to whatever debug features
   are supported by the targeted plug-able game system.  Currently this is a
  no-op, but is likely to see implementation in the 1.0.0 time-frame."
  [object]
  (merge object
         {:debug? true}))

(defn bounds-box
  "Set the `width` and `height` on an object. *Iris* uses this as the object's
   physical bounds, so these values are meaningful for 2d games.  Collisions
   are governed by the bounds-box specified in `watch-collisions`, so an
  object's hitbox can be different from its visual extent.
  ```
  (-> (progo/base-object game-system)
      (progo/bounds-box  64 64)) ; - create a 64x64 square game-object
  ```
  NB: future versions of *Prospero* are likely to introduce the concept of
  units to most display measurements. "
  [object width height]
  (merge object
         {:width width}
         {:height height}))

(defn position-2d
  "Sets the 2d position for a game-object.
  ```
  (-> (progo/base-object game-system)
      (progo/position-2d 50 50)) ; - position an object at 50, 50 pixels
  ```
  NB: future versions of *Prospero* are likely to introduce the concept of
  units to most display measurements. "
  [object x y]
  (merge object
         {:translation [x y 0]}))

(defn position-3d
  "Set the position for an object.
  ```
  (-> (progo/base-object game-system)
      (progo/position-3d 50 50 10)) ; - position an object at 50, 50, 10 pixels
  ```
  NB: future versions of *Prospero* are likely to introduce the concept of
  units to most display measurements. "
  [object x y z]
  (merge object
         {:translation [ x y z]}))

(defn rotation-2d
  "Set the rotation for an object in a 2d game - most meaningful for *Iris*.
  ```
  (-> (progo/base-object game-system)
      (progo/position-3d 50 50 10) ; - position an object at 50, 50, 10 pixels
      (progo/rotation-2d 45))      ; - and rotate 45 degrees clockwise
  ```
  NB: future versions of *Prospero* are likely to introduce the concept of
  units (both position and rotation) to most display measurements. "
  [object theta]
  (merge object
         {:rotation theta}))

(defn sprite-sheet
  "Make a sprite sheet available to an object.  A sprite sheet
   describes where a texture comes from and works in conjunction with
   `sprite-index`.
  A sprite sheet contains a map, as such:
  ```
  {:sprite-sheet-path   \"/path/to/image\"
     :sprite-width      a natural number specifying the width of a tile
     :sprite-height     a natural number specifying the height of a tile
     :columns           how many tile columns are in the sprite sheet
     :rows              how many rows}
  ```"
  [object {:keys [sprite-sheet-path sprite-width sprite-height columns rows]
           :as    sprite-sheet}]
  (merge object {:sprite-sheet sprite-sheet}))

(defn sprite-index
  "Provide a column and row index into a sprite-sheet
  ```
  (-> (progo/base-object game-system)
      (progo/sprite-sheet {:sprite-sheet-path \"/my/image.png\"
                           :sprite-width      64
                           :sprite-height     64
                           :columns           10
                           :rows              10}) ; - set up the image
      (progo/sprite-index 2 2)) ; - use image at column and row 2
  ```"
  [object col row]
  (merge object {:texture {:type :sprite :col col :row row}}))

(defn texture
  "Use a image file as a bitmap texture for the object.
  ```
  (-> (progo/base-object game-system)
      (progo/texture     \"/path\"to\"image.png\"))
  ```"
  [object texture-file]
  (merge object {:texture texture-file}))

(defn colour-rgb
  "Request that the object have a supplied colour
  ```
  (-> (progo/base-object game-system)
      (progo/colour-rgb  0 255 0)) ; - make a pure green object
  ```
  NB: future versions of *Prospero* are likely to support more colour models
  this routine will likely remain safe to use, though. "
  [object r g b]
  (merge object {:colour-rgb [r g b]}))

(defn colour-rgba
  "Request that the object have a supplied colour
  ```
  (-> (progo/base-object game-system)
      (progo/colour-rgba 0 255 0 0.5)) ; - make a pure green object at 50% alpha
  ```
  NB: future versions of *Prospero* are likely to support more colour models
  this routine will likely remain safe to use, though. "
  [object r g b a]
  (merge object {:colour-rgba [r g b a]}))

(defn watch-mutable
  "Instruct the object to observe the game's mutable state at
  each frame.  If this is turned on, *Prospero* will
  provide a snapshot of the mutable state atom with each loop.  This was the
  first adhoc inter-object communication system added to the engine.  It's
  probably better to prefer `watch-root` and signals over this.
  The mutable state can be queried in `:watch-mutable-state` during event
  processing (`update-state`), collisions, and any other fn that takes
  the is passed the object as an argument.
  ```
  (-> (progo/base-object game-system)
      (progo/watch-mutable)  ; - flag interest in the mutable state
      (progo/update-state    ; - write an updater that uses it
        (fn [me _ _ _] (get-in me [:watch-mutable-state]))))
  ```
  NB: This seems easy to use, but it may be retired in future versions.
  signals and watch-root seem a mostly better option."
  [object]
  (merge object {:watch-mutable-state {}}))

(defn watch-root
  "Instruct the object to observe the game's evolving state at
  each frame of the game loop.  If this is turned on, Prospero will
  provide a snapshot of the top of the game-object graph allowing
  the local node to observe the state of other objects in the graph
  by using get-in or similar.
  ```
  (-> (progo/base-object game-system)
      (progo/watch-root)     ; - flag interest in the root state
      (progo/update-state    ; - write an updater that uses it
        (fn [me _ _ root-state] ...)))
  ```
  NB: This marker must be provided for an `update-state` fn to
  receive the root-state.  This restriction may be lifted in
  future versions, but it's probably a good practice to mark
  which objects look back \"up\" the graph. "
  [object]
  (merge object {:watch-root-state {}}))

(defn update-state
  "Provide a function that is called at each from to modify the state
   of this object.  The function should be in this form:
  ```
  (fn [me-the-object current-time-code frame-elapsed-time root-state])
  ```
  A game-object map must be returned
  ```
  (-> (progo/base-object game-system)
      (progo/update-state
        (fn [me _ _ root-state]
          ...
          me))) ; - make some modifications or send some signals
  ```"
  [object updater-fn]
  (assoc object :update-state updater-fn))

(defn add-update-state
  "Compose an update-state function with the current chain
  of state updating functions. This allows the build up
  of per-frame behaviours to game-objects in a composable
  way.
  ```
  (-> (progo/base-object game-system)
      (progo/update-state
        (fn [me _ _ root-state]
          ...
          me)) ; - make some modifications or send some signals
      (progo/add-update-state
        (fn [me _ _ _]
          ...
          me))) ; - add another fn around the first
  ```
  This is a good way to share composable state functions."
  [object updater-fn]
  (let [current (get-in object [:update-state])]
    (if (nil? current)
      (assoc object :update-state updater-fn)
      (assoc object :update-state (fn [o c f r gm]
                                    (updater-fn (current o c f r gm) c f r gm))))))

(defn add-animator
  "Add an animation to the object, specified by
   an animation description.  See `prospero.animation` for more details."
  [object [data-path change-description curve extra-args]]
  (add-update-state
   object
   (proanim/animator data-path change-description curve extra-args)))

(defn add-animators
  "Add multiple animators to the object, specified by a vector of animation descriptions.
  See `prospero.animation` for more details."
  [object animators]
  (reduce
   #(add-animator %1 %2)
   object
   animators))

(defn add-constraints
  "Add a constraint from the constraints namespace to the object.
  The use of canned constraints makes it easier to build an entirely
  declarative definition of the game state.
  ```
  (-> (progo/base-object game-system)
      (progo/add-constraints [::proconstraint/keep-on-screen]))
  ```"
  [object constraints]
  (reduce
   #(add-update-state %1 (proconstraint/get-constraint-fn %2 object))
   object
   constraints))

(defn children
  "Attach any number of child objects to the game-state.
  ```
  (-> (progo/base-object game-system)
      (progo/children
        (-> (progo/base-object game-system) ...)
        (-> (progo/base-object game-system) ...)
        ...))
  ```"
  [object & kids]
  (merge object {:children (vec kids)}))

(defn process-event
  "Provide a map of event descriptors to functions or prebuilt routines that can
  modify this object.  Descriptors are vectors containing the event type and
   the subtype/signal.
  For example: `[:prospero.events/keyboard-up :prospero.events/key-letter-a]`
  The event codes will be mapped to a function taking the object/node itself,
  the arguments for the event, and the root-state.  Event arguments are a vector:
  `[major-event-code minor-event-code {:signal-args details}]`.

  ```
  (-> (progo/base-object game-system)
       (progo/process-event
                        {[::proevent/keyboard-held ::proevent/key-arrow-up]
                         [proevent/change-animators-on-event {:animate-up :running}]

                         [::proevent/keyboard-up   ::proevent/key-arrow-up]
                         [proevent/change-animators-on-event {:animate-up :stopped}]

                         [::proevent/keyboard-up   ::proevent/key-space-bar]
                         (fn [me [_ _ {:keys [signal-args]}] root-state]
                            ...)}))
  ```"
  [object event-map]
  (merge object {:process-event event-map}))

(defn watch-collision
  "Call this to add a set of collision detection bounds and
   details to an object.  Anything with collision details
   will be tested and a collision will be called before the
   object's state is updated.
  A collision map has the following form:
  ```
  {:bounds-box  [w h]   ; -can be different from normal bounds
    :offset     [x y z] ; - offset from \"left\" where the bounds start
    :level-set  #{} ; - a set of collision levels this belongs on
    :match-set  #{} ; - a set of collision levels this matches with
    :on-hit-fn  (fn [me collider root-state]) ; - a fn called during collision - updates me
    :reset-fn   (fn [me])} ; - called if me is no longer colliding
  ```"
  [object {:keys [bounds-box level-set match-set on-hit-fn reset-fn offset] :as cd-struct
           :or   {offset [0 0 0]}}]
  ;;TODO: if bounds are nil, maybe set them from the object shape?
  (merge object {:collisions (assoc cd-struct :offset offset)}))

(defn text-padding
  "Provide some space around a text game-object
  ```
   (-> (progo/base-object game-system)
       (progo/text-padding 10 20 10 20)) ; - build in some white-space around the text
  ```
  NB: future versions of *Prospero* are likely to introduce the concept of
  units to most display measurements. "
  [object left top right bottom]
  (merge object {:text-padding [left top right bottom]}))

(defn text
  "Make this node a text node
  ```
  (-> (progo/base-object game-system)
       (progo/text \"some text to display\"))
  ```
  Text is likely to be incompatible with textures, sprite sheets, etc. "
  [object text]
  (merge object {:text text}))

(defn text-rgb
  "Request that the object have a supplied colour for its text
  ```
  (-> (progo/base-object game-system)
      (progo/colour-rgb  255 255 255)
      (progo/text-rgb    0     0   0)) ; - black text on white
  ```
  NB: future versions of *Prospero* are likely to support more colour models
  this routine will likely remain safe to use, though. "
  [object r g b]
  (merge object {:text-rgb [r g b]}))

(defn wireframe
  "Do not use a fill colour, paint edges only.
  ```
  (-> (progo/base-object game-system)
      (progo/bounds-box  25 25)
      (progo/colour-rgb  0 0 255)
      (progo/wireframe)) ; - make an empty blue box 25x25
  ```"
  [object]
  (merge object {:wireframe true}))

(defn border-radius
  "Round the corners of a square box.  If the object is a box, round
  off the corners.  Likely most useful in *Iris*.
  ```
  (-> (progo/base-object   game-system)
      (progo/bounds-box    25 25)
      (progo/colour-rgb    0 0 255)
      (progo/border-radius 3
      (progo/wireframe)) ; - make an empty blue box 25x25 with rounded corners
  ```"
  [object r]
  (merge object {:border-radius r}))
