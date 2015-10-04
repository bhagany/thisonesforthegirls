(ns thisonesforthegirls.s3-dev
  (:require [cljs.core.async :refer [chan >! close!]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.s3 :as s3])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defrecord S3DevConn [conn]
  s3/S3
  (get-obj-ch [_ bucket key-name]
    (let [ch (chan)
          fs (node/require "fs")]
      (go
        (try
          (let [body (.readFileSync fs (str bucket "/" key-name) "utf8")]
            ;; let bindings are unavailable to #js in go blocks
            ;; http://dev.clojure.org/jira/browse/ASYNC-117
            (>! ch [nil (clj->js {:Body body})]))
          (catch js/Object e
            (>! ch [e nil])))
        (close! ch))
      ch))
  (put-obj!-ch [_ bucket key-name body]
    (let [ch (chan)
          fs (node/require "fs")]
      (go
        (try
          (.writeFileSync fs (str bucket "/" key-name) body "utf8")
          (>! ch [nil "that totally worked"])
          (catch js/Object e
            (>! ch [e nil])))
        (close! ch))
      ch))
  (put-obj!-ch [this bucket key-name body _]
    (s3/put-obj!-ch this bucket key-name body)))

(defn s3-dev-connection
  []
  (map->S3DevConn {}))
