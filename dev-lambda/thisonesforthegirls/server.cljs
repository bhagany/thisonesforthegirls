(ns thisonesforthegirls.server
  (:require [cljs.nodejs :as node]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.lambda-fns :as l]))

(defn lambda-middleware
  [lambda-fns]
  (fn [request response next]
    (let [body (.parse js/JSON (.-body request))
          res-text (case (.url request)
                     "/login" ((l/login lambda-fns) body {})
                     "404")]
      (.end response res-text))))

(defrecord DevServer [dir port server lambda-fns]
  component/Lifecycle
  (start [this]
    (if-not server
      (let [http (node/require "http")
            connect (node/require "connect")
            serve-static (node/require "serve-static")
            app (connect)
            static (serve-static dir #js {:index #js ["home"]})
            server* (.createServer http app)]
        (.use app "/lambda-fns" (lambda-middleware lambda-fns))
        (.use app "/" static)
        (.listen server* port)
        (assoc this :server server*))
      this))
  (stop [this]
    (when server
      (.close server))
    (assoc this :server nil)))

(defn dev-server
  ([dir]
   (dev-server dir 8080))
  ([dir port]
   (component/using
    (map->DevServer {:dir dir :port port})
    [:lambda-fns])))
