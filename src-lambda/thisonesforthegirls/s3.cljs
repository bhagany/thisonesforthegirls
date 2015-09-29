(ns thisonesforthegirls.s3
  (:require [cljs.core.async :refer [chan >! close!]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol S3
  (get-obj-ch [this bucket key-name])
  (put-obj!-ch [this params]))

(defrecord S3Conn [conn]
  component/Lifecycle
  (start [this]
    (if conn
      this
      (let [aws (node/require "aws-sdk")
            conn (aws.S3.)]
        (assoc this :conn conn))))
  (stop [this]
    (if conn
      (assoc this :conn nil)
      this))

  S3
  (get-obj-ch [_ bucket key-name]
    (let [ch (chan)]
      (.getObject conn
                  #js {:Bucket bucket
                       :Key key-name}
                  (fn [err obj]
                    (go
                      (>! ch [err obj])
                      (close! ch))))
      ch))
  (put-obj!-ch [_ params]
    (let [ch (chan)]
      (.putObject conn
                  params
                  (fn [err obj]
                    (go
                      (>! ch [err obj])
                      (close! ch))))
      ch)))

(defn s3-connection
  []
  (map->S3Conn {}))

(defn page-info->ch
  [s3-conn bucket]
  (fn
    [{:keys [s3-key body]}]
    (let [params #js {:Bucket bucket
                      :Key s3-key
                      :Body body
                      :ContentLength (count body)
                      :ContentType "text/html"}]
      (put-obj!-ch s3-conn params))))
