(ns thisonesforthegirls.s3-dev
  (:require [cljs.core.async :refer [chan >! close!]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.s3 :as s3])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defrecord S3DevConn [conn]
  s3/S3
  (get-obj-ch [_ bucket key-name]
    (let [ch (chan)]
      (go
        (>! ch ["it's an error!" "hi"])
        (close! ch))
      ch))
  (put-obj!-ch [_ params]
    (let [ch (chan)]
      (go
        (>! ch [nil "that totally worked"])
        (close! ch))
      ch)))

(defn s3-dev-connection
  []
  (map->S3DevConn {}))
