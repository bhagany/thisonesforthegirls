(ns thisonesforthegirls.core
  (:require [cljs.core.async :refer [<! merge]]
            [cljs-lambda.util :refer [async-lambda-fn]]
            [cljs.nodejs :as node]
            [datascript.core :as d]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.pages :as p]
            [thisonesforthegirls.util :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def db-bucket "thisonesforthegirls.org-private")
(def db-key "db")
(def public-bucket "thisonesforthegirls.org")

(def schema
  {:db/ident {:db/unique :db.unique/identity}
   :user/admin {}
   :user/name {}
   :user/password {}
   :token {}
   :secret {}

   ;; Page attributes
   :page/text {}
   :page/devotions {:db/cardinality :db.cardinality/many}
   :page/sections {:db/cardinality :db.cardinality/many}
   :page/resources {:db/cardinality :db.cardinality/many}
   :page/scriptures {:db/cardinality :db.cardinality/many}
   :page/testimonies {:db/cardinality :db.cardinality/many}

   :devotion/author {}
   :devotion/title {}
   :devotion/archived? {}

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

(def ^:export set-admin-creds
  (async-lambda-fn
   (fn [event context]
     (let [{:keys [username password]} event
           bcrypt (node/require "bcryptjs")
           hashed (.hashSync bcrypt password 10)
           conn-ch (db/get-conn-ch db-bucket db-key schema)]
       (go
         (let [conn (<! conn-ch)
               [err] (<! (db/transact!-ch
                          conn
                          [{:db/id -1
                            :db/ident :admin
                            :user/name username
                            :user/password hashed}]
                          db-bucket
                          db-key))]
           (if err
             (throw (js/Error. err))
             "Credentials set")))))))

(def ^:export set-token-secret
  (async-lambda-fn
   (fn [event context]
     (let [{:keys [secret]} event
           conn-ch (db/get-conn-ch db-bucket db-key schema)]
       (go
         (let [conn (<! conn-ch)
               [err] (<! (db/transact!-ch
                          conn
                          [{:db/id -1
                            :db/ident :secret
                            :secret secret}]
                          db-bucket
                          db-key))]
           (if err
             (throw (js/Error. err))
             "Secret set")))))))

(def ^:export login
  (async-lambda-fn
   (fn [event context]
     (let [conn-ch (db/get-conn-ch db-bucket db-key schema)
           {:keys [username password]} event
           bcrypt (node/require "bcryptjs")]
       (go
         (let [conn (<! conn-ch)
               db @conn
               stored-hash (d/q '[:find ?password .
                                  :in $ ?username
                                  :where
                                  [?e :db/ident :admin]
                                  [?e :user/name ?username]
                                  [?e :user/password ?password]]
                                db
                                username)]
           (if (.compareSync bcrypt password stored-hash)
             (u/get-login-token db)
             (throw (js/Error. "Wrong password")))))))))

(def ^:export generate-all-pages
  (async-lambda-fn
   (fn [event context]
     (let [conn-ch (db/get-conn-ch db-bucket db-key schema)]
       (go
         (let [conn (<! conn-ch)
               db @conn
               put-ch (->> p/all-page-info
                           (map (fn [i]
                                  (u/put-obj!-ch ((:fn i) db) public-bucket (:s3-key i)))
                                )
                           merge)]
           (loop []
             (let [[err :as val] (<! put-ch)]
               (when err
                 (throw (js/Error. err)))
               (when-not (nil? val)
                 (recur))))
           "success"))))))
