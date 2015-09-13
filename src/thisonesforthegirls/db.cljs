(ns thisonesforthegirls.db
  (:require [cljs.nodejs :as node]
            [cljs.core.async :refer [chan pipe >! close!]]
            [cognitect.transit :as transit]
            [datascript :as d]
            [datascript.btset :refer [Iter]]
            [datascript.transit :as dt]
            [goog.object])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def aws (node/require "aws-sdk"))
(def s3 (aws.S3.))

(def my-write-handlers (assoc dt/write-handlers Iter (transit/ListHandler.)))

(defn write-transit-str [o]
  (transit/write (transit/writer :json {:handlers my-write-handlers}) o))

(defn string->db
  [s schema]
  (d/init-db (dt/read-transit-str s) schema))

(defn db->string
  [db]
  (write-transit-str (d/datoms db :eavt)))

(defn get-obj-ch
  [bucket key-name]
  (let [ch (chan)]
    (.getObject s3
                #js {:Bucket bucket
                     :Key key-name}
                (fn [err obj]
                  (go
                    (>! ch [err obj])
                    (close! ch))))
    ch))

(defn put-obj!-ch
  [body bucket key-name]
  (let [ch (chan)]
    (.putObject s3
                #js {:Bucket bucket
                     :Key key-name
                     :Body body}
                (fn [err obj]
                  (go
                    (>! ch [err obj])
                    (close! ch))))
    ch))

(defn s3-obj->conn
  [schema]
  (fn [[err obj]]
    (let [conn (d/create-conn)]
      (when-not err
        (let [db (string->db (goog.object/get obj "Body") schema)]
          (reset! conn db)))
      conn)))

(defn get-conn-ch
  [bucket key-name schema]
  (let [ch (chan 1 (map (s3-obj->conn schema)))]
    (pipe (get-obj-ch bucket key-name) ch)
    ch))

(defn persist!-ch
  [db bucket key-name]
  (let [body (db->string db)]
    (put-obj!-ch body bucket key-name)))

(defn transact!-ch
  [conn tx-data bucket key-name]
  (let [report (d/transact! conn tx-data)
        db (:db-after report)]
    (persist!-ch db bucket key-name)))
