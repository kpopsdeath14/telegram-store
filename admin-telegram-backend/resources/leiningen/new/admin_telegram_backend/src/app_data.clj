(ns {{name}}.app-data
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   )
  )


(def app_data (with-open [r (io/reader "./app_state.edn")]
               (edn/read (java.io.PushbackReader. r))))