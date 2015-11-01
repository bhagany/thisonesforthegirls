(ns thisonesforthegirls.ses
  (:require [cljs.core.async :refer [chan >! close!]]
            [cljs.nodejs :as node]
            [com.stuartsierra.component :as component])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol SES
  (send!-ch [this from to reply-to message]))

(defrecord SESConn [conn]
  component/Lifecycle
  (start [this]
    (if conn
      this
      (let [aws (node/require "aws-sdk")
            conn (aws.SES.)]
        (assoc this :conn conn))))
  (stop [this]
    (if conn
      (assoc this :conn nil)
      this))

  SES
  (send!-ch [_ from to reply-to message]
    (let [ch (chan)]
      (.sendEmail conn
                  (clj->js
                   {:Source from
                    :Destination {:ToAddresses [to]}
                    :ReplyToAddresses [reply-to]
                    :Message {:Subject {:Data "From the sites Contact Us form"
                                        :Charset "utf-8"}
                              :Body {:Text {:Data message :Charset "utf-8"}}}})
                  (fn [err obj]
                    (go
                      (>! ch [err obj])
                      (close! ch))))
      ch)))

(defn ses-connection
  []
  (map->SESConn {}))
