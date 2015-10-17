(ns thisonesforthegirls.lambda-fns
  (:require [cljs.core.async :refer [<! merge]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.s3 :as s3]
            [thisonesforthegirls.pages :as p]
            [thisonesforthegirls.util :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defrecord LambdaFns [db s3-conn html-bucket])

(defn lambda-fns
  [html-bucket]
  (component/using
   (map->LambdaFns {:html-bucket html-bucket})
   [:db :s3-conn]))

;; Helpers

(defn page-info->ch
  [lambda-fns bucket]
  (fn [{:keys [s3-key body]}]
    (s3/put-obj!-ch (:s3-conn lambda-fns)
                    bucket s3-key body
                    {:ContentLength (count body)
                     :ContentType "text/html"})))

(defn get-secret
  [db]
  (d/q '[:find ?secret .
         :where
         [?e :db/ident :secret]
         [?e :secret ?secret]]
       db))

(defn make-login-token
  [db]
  (let [jwt (node/require "jsonwebtoken")
        secret (get-secret db)]
    (.sign jwt
           #js {:admin true}
           secret
           #js {:algorithm "HS512"
                :expiresInMinutes 1440})))

(defn check-login-token
  [db token]
  (let [jwt (node/require "jsonwebtoken")
        secret (get-secret db)]
    (println (.verify jwt token secret #js {:algorithms #js ["HS512"]}))
    (try
      (-> (.verify jwt token secret #js {:algorithms #js ["HS512"]})
          (goog.object/get "admin"))
      (catch (goog.object/get jwt "JsonWebTokenError") e
        false))))

;;; Functions for export

;; Private functions (not exposed through API Gateway)

(defn set-admin-creds
  [lambda-fns]
  (fn [event context]
    (let [{:keys [username password]} event
          {:keys [db]} lambda-fns
          bcrypt (node/require "bcryptjs")
          hashed (.hashSync bcrypt password 10)]
      (go
        (let [[err] (<! (db/transact!-ch
                         db
                         [{:db/id -1
                           :db/ident :admin
                           :user/name username
                           :user/password hashed}]))]
          (if err
            (throw (js/Error. err))
            "Credentials set"))))))

(defn set-token-secret
  [lambda-fns]
  (fn [event context]
    (let [{:keys [secret]} event
          {:keys [db]} lambda-fns]
      (go
        (let [[err] (<! (db/transact!-ch
                         db
                         [{:db/id -1
                           :db/ident :secret
                           :secret secret}]))]
          (if err
            (throw (js/Error. err))
            "Secret set"))))))

(defn generate-all-pages
  [lambda-fns]
  (fn [event context]
    (let [{:keys [db html-bucket]} lambda-fns]
      (go
        (let [conn (<! (:conn-ch db))
              db-data @conn
              put-ch (->> (p/all-page-info db-data)
                          (map (page-info->ch lambda-fns html-bucket))
                          merge)]
          (loop []
            (let [[err :as val] (<! put-ch)]
              (when err
                (throw (js/Error. err)))
              (when-not (nil? val)
                (recur))))
          "success")))))

;; Public functions (exposed through API Gateway)

(defn login
  [lambda-fns]
  (fn [event context]
    (let [{:keys [body]} event
          {:keys [username password]} body
          {:keys [db]} lambda-fns
          bcrypt (node/require "bcryptjs")]
      (go
        (let [conn (<! (:conn-ch db))
              stored-hash (d/q '[:find ?password .
                                 :in $ ?username
                                 :where
                                 [?e :db/ident :admin]
                                 [?e :user/name ?username]
                                 [?e :user/password ?password]]
                               @conn
                               username)]
          (cond
            (some nil? [username password]) (throw (js/Error. "Creds missing"))
            (.compareSync bcrypt password stored-hash) (make-login-token @conn)
            :else (throw (js/Error. "Wrong password"))))))))
