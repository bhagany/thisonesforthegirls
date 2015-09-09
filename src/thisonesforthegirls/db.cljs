(ns thisonesforthegirls.db
  (:require [cljs.nodejs :as node]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [chan pipe >! close!]]
            [datascript :as d]
            [datascript.core :refer [Datom]]
            [datascript.btset :refer [Iter]]
            [goog.object])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def aws (node/require "aws-sdk"))
(def s3 (aws.S3.))

(deftype DatomHandler []
  Object
  (tag [_ _] "datascript/Datom")
  (rep [_ d] #js [(.-e d) (.-a d) (.-v d) (.-tx d)])
  (stringRep [_ _] nil))

(def datascript-transit-reader
  (transit/reader :json
                  {:handlers {"datascript/Datom" (fn [[e a v tx]]
                                                   (d/datom e a v tx))}}))

(def datascript-transit-writer
  (transit/writer :json
                  {:handlers {Datom (DatomHandler.)
                              Iter (transit/VectorHandler.)}}))

(defn string->data
  [s]
  (transit/read datascript-transit-reader s))

(defn data->string
  [data]
  (transit/write datascript-transit-writer data))

(defn string->db
  [s schema]
  (d/init-db (string->data s) schema))

(defn db->string
  [db]
  (data->string (d/datoms db :eavt)))

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
