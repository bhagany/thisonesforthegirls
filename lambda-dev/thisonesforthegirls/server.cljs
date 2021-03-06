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
          req-body (.-body request)
          body (if (string? req-body)
                 (.parse js/JSON req-body)
                 req-body)
          headers (.-headers request)
          event (merge {:jwt (goog.object/get headers "x-jwt")}
                       (js->clj body :keywordize-keys true))
          res-fn (case (.-url request)
                   "/admin-page" (l/admin-page lambda-fns)
                   "/edit-page" (l/edit-page lambda-fns)
                   "/delete-page" (l/delete-page lambda-fns)
                   "/login" (l/login lambda-fns)
                   "/send-email" (l/send-email lambda-fns))]
      (go
        (let [result (<! (res-fn event {}))]
          (if (instance? js/Error result)
            (do
              (set! (.-statusCode response) 500)
              (.end response
                    (.stringify js/JSON
                     ;; #js reader macro doesn't work inside go blocks
                     ;; http://dev.clojure.org/jira/browse/ASYNC-117
                     (clj->js {:errorMessage (.-message result)
                               :errorType (.. result -constructor -name)
                               :stackTrace (.-stack result)}))))
            (.end response (.stringify js/JSON (clj->js result)))))))))

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
            parser (node/require "body-parser")
            app (connect)
            server* (.createServer http app)]
        (.use app "/lambda-fns" (.text parser #js {:type "*/*"}))
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
