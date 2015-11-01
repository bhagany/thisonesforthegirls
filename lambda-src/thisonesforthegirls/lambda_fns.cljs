(ns thisonesforthegirls.lambda-fns
  (:require [cljs.core.async :refer [chan >! <! close! merge]]
            [cljs.nodejs :as node]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.pages :as p]
            [thisonesforthegirls.ses :as ses])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.object]))


(defrecord LambdaFns [db pages])

(defn lambda-fns
  []
  (component/using
   (map->LambdaFns {})
   [:db :pages :ses-conn]))

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
    (let [{:keys [pages]} lambda-fns]
      (go
        (let [put-ch (->> (<! (p/all-page-info pages))
                          (map (p/page-info->put-ch pages))
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
            (.compareSync bcrypt
                          password
                          stored-hash) (.stringify
                                        js/JSON
                                        (clj->js
                                         {:jwt (make-login-token @conn)}))
            :else (js/Error. "Wrong username or password")))))))

(defn admin-page-router
  [pages path query]
  (go
    (cond
      (= path "/admin") (p/admin pages)
      (= path "/admin/welcome") (<! (p/admin-home pages))
      (= path "/admin/about") (<! (p/admin-about-us pages))
      (= path "/admin/community-resources") (<! (p/admin-resources pages))
      (= path "/admin/devotions") (<! (p/admin-devotions pages))
      (= path "/admin/devotions/add") (p/admin-devotions-add pages)
      (s/starts-with? path "/admin/devotions/edit") (<! (p/admin-devotions-edit pages query))
      (s/starts-with? path "/admin/devotions/delete") (<! (p/admin-devotions-delete pages query))
      (= path "/admin/testimonies") (<! (p/admin-testimonies pages))
      (= path "/admin/testimonies/add") (p/admin-testimonies-add pages)
      (s/starts-with? path "/admin/testimonies/edit") (<! (p/admin-testimonies-edit pages query))
      (s/starts-with? path "/admin/testimonies/delete") (<! (p/admin-testimonies-delete pages query))
      (= path "/admin/scripture/categories") (<! (p/admin-scripture-categories pages))
      (= path "/admin/scripture/categories/add") (p/admin-scripture-categories-add pages)
      (s/starts-with? path "/admin/scripture/categories/edit") (<! (p/admin-scripture-categories-edit pages query))
      (s/starts-with? path "/admin/scripture/categories/delete") (<! (p/admin-scripture-categories-delete pages query))
      (s/starts-with? path "/admin/scripture/add") (p/admin-scripture-add pages)
      (s/starts-with? path "/admin/scripture/edit") (<! (p/admin-scripture-edit pages query))
      (s/starts-with? path "/admin/scripture/delete") (<! (p/admin-scripture-delete pages query))
      (= path "/admin/contact") (<! (p/admin-contact pages))
      :else p/admin-error)))

(defn admin-page
  [lambda-fns]
  (fn [event context]
    (go
      (let [{:keys [path query jwt]} event
            {:keys [pages db]} lambda-fns
            conn (<! (:conn-ch db))]
        ;; check token, return login form if bad
        (if (check-login-token @conn jwt)
          (<! (admin-page-router pages path query))
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
                  (= path "/admin/community-resources") p/edit-resources
                  (= path "/admin/devotions/add") p/add-devotion
                  (s/starts-with? path "/admin/devotions/edit") p/edit-devotion
                  (= path "/admin/testimonies/add") p/add-testimony
                  (s/starts-with? path "/admin/testimonies/edit") p/edit-testimony
                  (= path "/admin/scripture/categories/add") p/add-scripture-category
                  (s/starts-with? path "/admin/scripture/categories/edit") p/edit-scripture-category
                  (s/starts-with? path "/admin/scripture/add") p/add-scripture
                  (s/starts-with? path "/admin/scripture/edit") p/edit-scripture
                  (= path "/admin/contact") p/edit-contact)]
            (<! (edit-fn pages event)))
          (js/Error "Please log in"))))))

(defn delete-page
  [lambda-fns]
  (fn [event context]
    (go
      (let [{:keys [path jwt]} event
            {:keys [pages db]} lambda-fns
            conn (<! (:conn-ch db))]
        (if (check-login-token @conn jwt)
          (let [delete-fn
                (cond
                  (s/starts-with? path "/admin/devotions/delete") p/delete-devotion
                  (s/starts-with? path "/admin/testimonies/delete") p/delete-testimony
                  (s/starts-with? path "/admin/scripture/categories/delete") p/delete-scripture-category
                  (s/starts-with? path "/admin/scripture/delete") p/delete-scripture)]
            (<! (delete-fn pages event)))
          (js/Error "Please log in"))))))

(defn send-email
  [lambda-fns]
  (fn [event context]
    (go
      (let [{:keys [name reply-to message]} event
            {:keys [ses-conn db]} lambda-fns
            nice-reply-to (str name " <" reply-to ">")
            conn (<! (:conn-ch db))
            contact-email (d/q '[:find ?email .
                                 :where
                                 [?contact :db/ident :contact]
                                 [?contact :contact/email ?email]]
                               @conn)]
        (if (some empty? [name reply-to message contact-email])
          (js/Error. "Please fill out all the fields")
          (let [[err] (<! (ses/send!-ch ses-conn
                                        "contact@thisonesforthegirls.org"
                                        "thisonesforthegirls@gmail.com"
                                        nice-reply-to
                                        message))]
            (if err
              (js/Error. err)
              (.stringify js/JSON (clj->js {:success "Your message was sent"
                                            :redirect "/contact"})))))))))
