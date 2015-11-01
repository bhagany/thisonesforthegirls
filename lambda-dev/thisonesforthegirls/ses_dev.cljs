(ns thisonesforthegirls.ses-dev
  (:require [cljs.core.async :refer [chan sliding-buffer >! close!]]
            [com.stuartsierra.component :as component]
            [thisonesforthegirls.ses :as ses])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defrecord SESDevConn [conn]
  component/Lifecycle
  (start [this]
    (if conn
      this
      (let [conn (chan (sliding-buffer 1))]
        (assoc this :conn conn))))
  (stop [this]
    (if conn
      (assoc this :conn nil)
      this))

  ses/SES
  (send!-ch [_ from to reply-to message]
    (let [ch (chan)]
      (go
        (>! conn {:from from :to to :reply-to reply-to :message message}))
      (go
        (>! ch [nil "omg you sent an email"])
        (close! ch))
      ch)))

(defn ses-dev-connection
  []
  (map->SESDevConn {}))
