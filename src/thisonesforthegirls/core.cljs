(ns thisonesforthegirls.core
  (:require [cljs.core.async :refer [<!]]
            [cljs-lambda.util :refer [async-lambda-fn]]
            [cljs.nodejs :as node]
            [datascript :as d]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.util :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def db-bucket "thisonesforthegirls.org-private")
(def db-key "db")

(def schema
  {:db/ident {:db/unique :db.unique/identity}
   :user/admin {}
   :user/name {}
   :user/password {}
   :token {}
   :secret {}})

(def ^:export set-admin-creds
  (async-lambda-fn
   (fn [event context]
     (let [{:keys [username password]} event
           bcrypt (node/require "bcrypt")
           hashed (.hashSync bcrypt password 10)
           conn-ch (db/get-conn-ch db-bucket db-key schema)]
       (go
         (let [conn (<! conn-ch)
               [err obj] (<! (db/transact!-ch
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
               [err obj] (<! (db/transact!-ch
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
           bcrypt (node/require "bcrypt")]
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
