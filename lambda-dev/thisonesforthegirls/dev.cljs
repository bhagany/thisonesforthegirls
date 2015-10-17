(ns thisonesforthegirls.dev
  (:require [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.s3-dev :as s3]
            [thisonesforthegirls.server :as serv]))

(node/enable-util-print!)

(def config
  {:server {:html-dir "/www/thisonesforthegirls/fake-s3/public"
            :asset-dir "/www/thisonesforthegirls/assets"
            :js-dir "/www/thisonesforthegirls/out/browser-dev"
            :port 8080}
   :db {:bucket "/www/thisonesforthegirls/fake-s3/private"
        :key "db"}
   :lambda-fns {:html-bucket "/www/thisonesforthegirls/fake-s3/public"}})

(defn dev-system [config]
  (component/system-map
   :server (serv/dev-server (get-in config [:server :html-dir])
                            (get-in config [:server :asset-dir])
                            (get-in config [:server :js-dir])
                            (get-in config [:server :port]))
   :s3-conn (s3/s3-dev-connection)
   :db (db/datascript-db (get-in config [:db :bucket])
                         (get-in config [:db :key]))
   :lambda-fns (l/lambda-fns (get-in config [:lambda-fns :html-bucket]))))

(defonce system (atom nil))

;; TODO: figure out how to stop and start components when figwheel reloads

(defn -main
  []
  (if @system
    (do
      (swap! system component/stop)
      (swap! system component/start))
    (reset! system (component/start (dev-system config)))))

(set! *main-cli-fn* -main)
