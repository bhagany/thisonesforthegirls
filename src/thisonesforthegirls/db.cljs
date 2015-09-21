(ns thisonesforthegirls.db
  (:require [cljs.core.async :refer [chan pipe]]
            [cognitect.transit :as transit]
            [datascript.btset :refer [Iter]]
            [datascript.core :as d]
            [datascript.transit :as dt]
            [thisonesforthegirls.util :as u])
  (:import [goog.object]))

(def my-write-handlers (assoc dt/write-handlers Iter (transit/ListHandler.)))

(defn write-transit-str [o]
  (transit/write (transit/writer :json {:handlers my-write-handlers}) o))

(defn string->db
  [s schema]
  (d/init-db (dt/read-transit-str s) schema))

(defn db->string
  [db]
  (write-transit-str (d/datoms db :eavt)))

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
    (pipe (u/get-obj-ch bucket key-name) ch)
    ch))

(defn persist!-ch
  [db bucket key-name]
  (let [body (db->string db)]
    (u/put-obj!-ch body bucket key-name)))

(defn transact!-ch
  [conn tx-data bucket key-name]
  (let [report (d/transact! conn tx-data)
        db (:db-after report)]
    (persist!-ch db bucket key-name)))
