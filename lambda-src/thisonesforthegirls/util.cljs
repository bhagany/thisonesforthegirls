(ns thisonesforthegirls.util
  (:require [cljs.core.async :refer [chan >! close!]]
            [cljs.nodejs :as node]
            [datascript.core :as d])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import [goog.object]))


(defn get-secret
  [db]
  (d/q '[:find ?secret .
         :where
         [?e :db/ident :secret]
         [?e :secret ?secret]]
       db))

(defn get-login-token
  [db]
  (let [jwt (node/require "jsonwebtoken")
        secret (get-secret db)]
    (.sign jwt
           #js {:admin true}
           secret
           #js {:algorithm "HS512"
                :expiresInMinutes 1440})))

(defn check-login-token
  [token db]
  (let [jwt (node/require "jsonwebtoken")
        secret (get-secret db)]
    (try
      (-> (.verify jwt token secret #js {:algorithms ["HS512"]})
          (goog.object/get "admin"))
      (catch (goog.object/get jwt "JsonWebTokenError") e
        false))))
