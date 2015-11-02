;; TODO: cloudformation template
(ns thisonesforthegirls.system
  (:require [cljs-lambda.util :refer [async-lambda-fn]]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.pages :as p]
            [thisonesforthegirls.s3 :as s3]
            [thisonesforthegirls.ses :as ses]))

(def config
  {:db {:bucket "thisonesforthegirls.org-private"
        :key "db"}
   :pages {:lambda-base "https://ybs1weoc79.execute-api.us-east-1.amazonaws.com/prod/"
           :html-bucket "thisonesforthegirls.org"}})

(defn prod-system [config]
  (component/system-map
   :s3-conn (s3/s3-connection)
   :ses-conn (ses/ses-connection)
   :db (db/datascript-db (get-in config [:db :bucket])
                         (get-in config [:db :key]))
   :lambda-fns (l/lambda-fns)
   :pages (p/pages (get-in config [:pages :lambda-base])
                   (get-in config [:pages :html-bucket]))))

(def system (prod-system config))

(defn with-started-system
  [wrapped-fn system event context]
  (let [started-system (component/start system)
        async-fn (async-lambda-fn (wrapped-fn (:lambda-fns started-system)))]
    (async-fn event context)))

(defn ^:export set-admin-creds
  [event context]
  (with-started-system l/set-admin-creds system event context))

(defn ^:export set-token-secret
  [event context]
  (with-started-system l/set-token-secret system event context))

(defn ^:export generate-all-pages
  [event context]
  (with-started-system l/generate-all-pages system event context))

(defn ^:export login
  [event context]
  (with-started-system l/login system event context))

(defn ^:export admin-page
  [event context]
  (with-started-system l/admin-page system event context))

(defn ^:export edit-page
  [event context]
  (with-started-system l/edit-page system event context))

(defn ^:export delete-page
  [event context]
  (with-started-system l/delete-page system event context))

(defn ^:export send-email
  [event context]
  (with-started-system l/send-email system event context))
