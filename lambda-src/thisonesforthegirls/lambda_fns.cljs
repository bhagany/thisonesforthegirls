(ns thisonesforthegirls.lambda-fns
  (:require [cljs.core.async :refer [chan >! <! close! merge]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.pages :as p])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.object]))


(defrecord LambdaFns [db pages])

(defn lambda-fns
  []
  (component/using
   (map->LambdaFns {})
   [:db :pages]))

;; Helpers

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
            (js/Error. err)
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
            (js/Error. err)
            "Secret set"))))))

(defn generate-all-pages
  [lambda-fns]
  (fn [event context]
    (let [{:keys [db pages]} lambda-fns]
      (go
        (let [put-ch (->> (<! (p/all-page-info pages))
                          (map (p/page-info->put-ch lambda-fns))
                          merge)
              error (loop []
                      (let [[err :as val] (<! put-ch)]
                        (when err
                          (js/Error. err))
                        (when-not (nil? val)
                          (recur))))]
          (if error
            error
            "success"))))))

;; Public functions (exposed through API Gateway)

(defn login
  [lambda-fns]
  (fn [event context]
    (let [{:keys [username password]} event
          {:keys [db]} lambda-fns
          bcrypt (node/require "bcryptjs")]
      (go
        (let [conn (<! (:conn-ch db))
              stored-hash (or (d/q '[:find ?password .
                                     :in $ ?username
                                     :where
                                     [?e :db/ident :admin]
                                     [?e :user/name ?username]
                                     [?e :user/password ?password]]
                                   @conn
                                   username)
                              "")]
          (cond
            (some nil? [username password]) (js/Error. "Creds missing")
            (.compareSync bcrypt password stored-hash) (make-login-token @conn)
            :else (js/Error. "Wrong username or password")))))))

(defn admin-page
  [lambda-fns]
  (fn [event context]
    (go
      (let [{:keys [path jwt]} event
            {:keys [pages db]} lambda-fns
            conn (<! (:conn-ch db))]
        ;; check token, return login form if bad
        (if (check-login-token @conn jwt)
          (case path
            "/admin" (p/admin pages)
            "/admin/welcome" (<! (p/admin-home pages))
            "/admin/about" (<! (p/admin-about-us pages))
            "/admin/community-resources" (<! (p/admin-resources pages))
            p/admin-error)
          (p/login-form pages))))))

(defn edit-page
  [lambda-fns]
  (fn [event context]
    (go
      (let [{:keys [path jwt]} event
            {:keys [pages db]} lambda-fns
            conn (<! (:conn-ch db))]
        (if (check-login-token @conn jwt)
          (let [edit-fn
                (cond
                  (= path "/admin/welcome") p/edit-home
                  (= path "/admin/about") p/edit-about-us
                  (= path "/admin/community-resources") p/edit-resources)]
            (<! (edit-fn pages event)))
          (js/Error "Please log in"))))))
