(ns thisonesforthegirls.server
  (:require [cljs.core.async :refer [<!]]
            [cljs.nodejs :as node]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.lambda-fns :as l]
            [thisonesforthegirls.pages :as p])
  (:import [goog.object]
           [goog.string])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn lambda-middleware
  [lambda-fns]
  (fn [request response next]
    (let [{:keys [pages]} lambda-fns
          req-body (goog.object/get request "body")
          body (if (string? req-body)
                 (.parse js/JSON req-body)
                 req-body)
          headers (goog.object/get request "headers")
          event {:jwt (goog.object/get headers "x-jwt")
                 :method (goog.object/get request "method")
                 :body (js->clj body :keywordize-keys true)}
          res-fn (case (goog.object/get request "url")
                   "/login" (l/login lambda-fns))]
      (go
        ;; TODO: catch errors, fail nicely
        (let [res-text (<! (res-fn event {}))]
          (.end response res-text))))))

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

(defrecord DevServer [html-dir asset-dir js-dir port server lambda-fns]
  component/Lifecycle
  (start [this]
    (if-not server
      (let [http (node/require "http")
            connect (node/require "connect")
            app (connect)
            server* (.createServer http app)]
        (.use app "/lambda-fns" (lambda-middleware lambda-fns))
        (.use app "/assets/js" (static-middleware js-dir false))
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
  ([html-dir asset-dir js-dir]
   (dev-server html-dir asset-dir js-dir 8080))
  ([html-dir asset-dir js-dir port]
   (component/using
    (map->DevServer {:html-dir html-dir :asset-dir asset-dir
                     :js-dir js-dir :port port})
    [:lambda-fns])))
