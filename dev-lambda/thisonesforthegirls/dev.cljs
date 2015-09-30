(ns thisonesforthegirls.dev
  (:require [com.stuartsierra.component :as component]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.s3-dev :as s3]))

(def config
  {:db {:bucket "thisonesforthegirls.org-private"
        :key "db"}
   :lambda-fns {:html-bucket "thisonesforthegirls.org"}})

(defn dev-system [config]
  (component/system-map
   :s3-conn (s3/s3-dev-connection)
   :db (db/datascript-db (get-in config [:db :bucket])
                         (get-in config [:db :key]))
   :lambda-fns (l/lambda-fns (get-in config [:lambda-fns :html-bucket]))))

(def system (dev-system config))
