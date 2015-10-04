(ns thisonesforthegirls.dev
  (:require [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [figwheel.client :as fw]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.s3-dev :as s3]
            [thisonesforthegirls.server :as serv]))

(node/enable-util-print!)

(def config
  {:server {:dir "/www/thisonesforthegirls/public"
            :port 8080}
   :db {:bucket "/www/thisonesforthegirls/private"
        :key "db"}
   :lambda-fns {:html-bucket "/www/thisonesforthegirls/public"}})

(defn dev-system [config]
  (component/system-map
   :server (serv/dev-server (get-in config [:server :dir])
                            (get-in config [:server :port]))
   :s3-conn (s3/s3-dev-connection)
   :db (db/datascript-db (get-in config [:db :bucket])
                         (get-in config [:db :key]))
   :lambda-fns (l/lambda-fns (get-in config [:lambda-fns :html-bucket]))))

(def system (atom nil))

(defn -main
  []
  (reset! system (component/start (dev-system config))))
(set! *main-cli-fn* -main)

(fw/start {:build-id "dev-lambda"})