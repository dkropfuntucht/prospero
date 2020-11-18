(defproject prospero/prospero "0.1.1"
  :description "A multi-target pure Clojure(Script) game engine"
  :url "https://github.com/dkropfuntucht/prospero"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure     "1.10.0"]
                 [org.clojure/core.async "1.3.610"]]
  :plugins      [[lein-codox "0.10.7"]]
  :codox        {:metadata {:doc/format :markdown}}
  :repl-options {:init-ns prospero.core})
