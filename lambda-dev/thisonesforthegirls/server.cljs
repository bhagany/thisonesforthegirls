(ns thisonesforthegirls.server
  (:require [cljs.nodejs :as node]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.pages :as p])
  (:import [goog.object]
           [goog.string]))

(defn lambda-middleware
  [lambda-fns]
  (fn [request response next]
    (let [_ (println (.-body request))
          body (.parse js/JSON (.-body request))
          res-text (case (.-url request)
                     "/login" ((l/login lambda-fns) body {})
                     (p/login-form "testing"))]
      (.end response res-text))))

(defn static-headers
  [response path]
  (let [content-type (cond
                       (s/ends-with? path ".png") "image/png"
                       (s/ends-with? path ".gif") "image/gif"
                       (s/ends-with? path ".jpg") "image/jpg"
                       (s/ends-with? path ".css") "text/css"
                       :else "text/html")]
    (.setHeader response "Content-Type" content-type)))

(defn static-middleware
  [dir s3-like]
  (fn [request response next]
    (let [serve-static (node/require "serve-static")
          static (serve-static dir #js {:index #js ["home"]
                                        :setHeaders static-headers})]
      (when s3-like
        ;; need to not replace first /
        (let [filename (str "/" (s/replace (subs (.-url request) 1) "/" "___"))]
          (goog.object/set request "url" filename)))
      (static request response next))))

(defrecord DevServer [html-dir asset-dir port server lambda-fns]
  component/Lifecycle
  (start [this]
    (if-not server
      (let [http (node/require "http")
            connect (node/require "connect")
            app (connect)
            server* (.createServer http app)]
        (.use app "/lambda-fns" (lambda-middleware lambda-fns))
        (.use app "/assets" (static-middleware asset-dir false))
        (.use app "/" (static-middleware html-dir true))
        (.listen server* port)
        (assoc this :server server*))
      this))
  (stop [this]
    (when server
      (.close server))
    (assoc this :server nil)))

(defn dev-server
  ([html-dir asset-dir]
   (dev-server html-dir asset-dir 8080))
  ([html-dir asset-dir port]
   (component/using
    (map->DevServer {:html-dir html-dir :asset-dir asset-dir :port port})
    [:lambda-fns])))
