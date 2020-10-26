;; - Copyright 2020 - dkropfuntucht
(ns prospero.compatibility
  "This namespace provides useful routines that differ across
  host languages."
  #?(:clj (:import [java.util Date])))

(defn get-time
  "Return the current system time."
  []
  #?(:cljs (.getTime (js/Date.))
     :clj  (.getTime (Date.))))
