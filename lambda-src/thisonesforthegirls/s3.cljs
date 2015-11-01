(ns thisonesforthegirls.s3
  (:require [cljs.core.async :refer [promise-chan >!]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol S3
  (get-obj-ch [this bucket key-name])
  (put-obj!-ch
    [this bucket key-name body]
    [this bucket key-name body options])
  (delete-obj!-ch [this bucket key-name]))

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
    (let [ch (promise-chan)]
      (.getObject conn
                  #js {:Bucket bucket
                       :Key key-name}
                  (fn [err obj]
                    (go (>! ch [err obj]))))
      ch))
  (put-obj!-ch [this bucket key-name body]
    (put-obj!-ch this bucket key-name body {}))
  (put-obj!-ch [_ bucket key-name body options]
    (let [ch (promise-chan)
          params (merge options {:Bucket bucket :Key key-name :Body body})]
      (.putObject conn
                  (clj->js params)
                  (fn [err obj]
                    (go (>! ch [err obj]))))
      ch))
  (delete-obj!-ch [_ bucket key-name]
    (let [ch (promise-chan)
          params #js {:Bucket bucket :Key key-name}]
      (.deleteObject conn
                     params
                     (fn [err obj]
                       (go (>! ch [err obj]))))
      ch)))

(defn s3-connection
  []
  (map->S3Conn {}))
