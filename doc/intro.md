# Introduction to Prospero

Figwheel is a good way to start using Prospero in a web browser.  Consider
setting up a figwheel project and starting with ClojureScript.


Add Prospero to your Project:

```
["prospero/prospero" "0.1.0"]
```

There needs to be an underlying game-system so pull in Iris, to start

```
["prospero/iris" "0.1.0"]
```

Iris is fit for 2D games and works well enough with Figwheel.  (It's also the
only plugable engine available for Prospero right now too...)

In your game's starting namespace, pull in Prospero's main namespace:
```
(require [prospero.core :as procore])
```

Grab Iris for its namespaced keywords and dependencies:
````
(require [iris.core :as iris])
````

That's about all we'll need to do to think about iris.  All functions we need
to build a game are called through Prospero and it delegates to Iris, as
necessary.

Define a game system with a simple map
```
(def game-system {::procore/game-system ::iris/game-system
                  :display-width        1024
                  :display-height       758
                  ::iris/web-root       (.getElementById js/document "app")})`
```

`:display-width` and `:display-height` are not namespaced yet, as they're
likely going to change in a future version of Prospero.  Right now, they're just
the pixel dimensions of the root dom element used by Iris.

To start the game, we'll need a game object for Prospero to render:

````
(def game-object {:game-system game-system
                  :object-id   (gensym "my root")
                  :width       1024
                  :height      768
                  :colour-rgb  [255 0 0]
                  :children    []})
````

Now start the engine:
```
(procore/start-game game-system [game-object])
```

If you've set up figwheel, and have been reasonably successful, you should see a big red box.

Not an interesting game, but a pretty good start.

Prospero includes the `prospero.game-objects` namespace to make trees of game-objects easier to build.
It is entirely possible to (sticking with pure-data elements) define a game tree in edn and load it,
but for most games, this namespace makes for a quick way to build behaviours and will shield most games
from changes to the underlying data model.

Let's try the big red box this way:

```
(require '[prospero.game-objects :as progo])

(def game-object (-> (progo/base-object game-system)
                     (progo/bounds-box  1024 768)
                     (progo/colour-rgb  255 0 0))
```

Now let's make a slightly more interesting game
(A smaller red box the player can move):

We pull in events to respond to player input

```
(require [prospero.events :as proevent])
```

```
(def game-object
(-> (progo/base-object  game-system)
    (progo/bounds-box   100 100)
    (progo/colour-rgb   255 0 0)
    (progo/position-3d  512 384)
    (progo/process-event
    {[::proevent/keyboard-held ::proevent/key-arrow-up]
     [proevent/change-animators-on-event {:animate-up :running}]

     [::proevent/keyboard-up   ::proevent/key-arrow-up]
     [proevent/change-animators-on-event {:animate-up :stopped}]})
     (progo/add-animators
     [[[:translation 1] [[1 :pixel] [1 :frame]] :constant {:animator-id   :animate-up
                                                           :initial-state :stopped}]
      [[:translation 1] [[-1 :pixel] [1 :frame]] :constant {:animator-id   :animate-down
                                                            :initial-state :stopped}]])))
```

It should be straight forward to explore the rest of the system from here.

There's `progo/children` to add nodes to the game-object tree.  Signal events for inter-object communication.
`progo/watch-collision` to tie into the collision-detection system, and more.

LocalWords:  Prospero Figwheel figwheel ClojureScript prospero js dom
LocalWords:  plugable namespace Prospero's procore namespaced gensym
LocalWords:  getElementById rgb edn progo proevent coulour
