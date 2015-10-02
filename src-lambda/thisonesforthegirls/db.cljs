(ns thisonesforthegirls.db
  (:require [cljs.core.async :refer [chan pipe >! <! close!]]
            [cognitect.transit :as transit]
            [com.stuartsierra.component :as component]
            [datascript.btset :refer [Iter]]
            [datascript.core :as d]
            [datascript.transit :as dt]
            [thisonesforthegirls.s3 :as s3])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import [goog.object]))

(def schema
  {:db/ident {:db/unique :db.unique/identity}
   :user/admin {}
   :user/name {}
   :user/password {}
   :token {}
   :secret {}

   ;; Page attributes
   :page/text {}
   :page/sections {:db/cardinality :db.cardinality/many}
   :page/resources {:db/cardinality :db.cardinality/many}
   :page/scriptures {:db/cardinality :db.cardinality/many}
   :page/testimonies {:db/cardinality :db.cardinality/many}

   :devotion/author {}
   :devotion/title {}
   :devotion/body {}
   :devotion/created-at {}
   :devotion/featured? {}

   :resource/name {}
   :resource/text {}

   :section/name {}

   :scripture-category/name {}
   :scripture-category/slug {}

   :scripture/category {:db/valueType :db.type/ref}
   :scripture/text {}
   :scripture/reference {}

   :testimony/title {}
   :testimony/slug {}
   :testimony/text {}})

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

(defprotocol Db
  (refresh-conn-ch [this])
  (persist!-ch [this])
  (transact!-ch [this tx-data]))

(defrecord DatascriptS3Db [s3-conn bucket key-name conn-ch]
  component/Lifecycle
  (start [this]
    (if conn-ch
      this
      (refresh-conn-ch this)))
  (stop [this]
    (if conn-ch
      (do
        (close! conn-ch)
        (assoc this :conn-ch nil))
      this))

  Db
  (refresh-conn-ch [this]
    (let [s3-conn-ch (chan 1 (map (s3-obj->conn schema)))
          new-conn-ch (chan)]
      (pipe (s3/get-obj-ch s3-conn bucket key-name) s3-conn-ch)
      (go-loop [conn nil]
        (let [conn* (if conn
                      conn
                      (<! s3-conn-ch))]
          (>! new-conn-ch conn*)
          (recur conn*)))
      (assoc this :conn-ch new-conn-ch)))
  (persist!-ch [_]
    (go
      (let [db @(<! conn-ch)
            body (db->string db)
            ;; let bindings are unavailable to #js in go blocks
            ;; http://dev.clojure.org/jira/browse/ASYNC-117
            params (clj->js {:Body body
                             :Bucket bucket
                             :Key key-name})]
        (<! (s3/put-obj!-ch s3-conn params)))))
  (transact!-ch [this tx-data]
    (go
      (let [conn (<! conn-ch)]
        (d/transact! conn tx-data)
        (<! (persist!-ch this))))))

(defn datascript-db
  [bucket key-name]
  (component/using
   (map->DatascriptS3Db {:bucket bucket :key-name key-name})
   [:s3-conn]))
