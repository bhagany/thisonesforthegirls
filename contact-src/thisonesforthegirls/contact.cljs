(ns thisonesforthegirls.contact
  (:require [goog.dom :as dom]
            [goog.dom.forms :as f]
            [goog.events :as e]
            [goog.net.cookies :as cookies]
            [goog.style :as style])
  (:import [goog.net XhrIo]))

;; This must be set in your build
(goog-define contact-url "http://your.api.com")

(defn handle-ajax-response
  [response]
  (let [xhr (.-target response)
        json (.getResponseJson xhr)]
    (if (< (.getStatus xhr) 300)
      (let [redirect (.-redirect json)]
        (cookies/set "contact-message" (.-success json) -1 "/")
        (set! (.-href (.-location js/window)) redirect))
      (let [error-p (.getElementById js/document "error")]
        (set! (.-innerHTML error-p) (.-errorMessage json))
        (style/setStyle error-p "display" "block")))))

(defonce form-submitter
  (let [body (aget (dom/getElementsByTagNameAndClass "body") 0)]
    (letfn [(cb [e]
              (.preventDefault e)
              (let [form (.-target e)
                    action (.getAttribute form "action")
                    form-data (f/getFormDataMap form)
                    xhr-json (->> (goog.object/get form-data "map_")
                                  js->clj
                                  (map (fn [[k v]] [k (v 0)]))
                                  (into {})
                                  clj->js
                                  (.stringify js/JSON))]
                (goog.net.XhrIo.send action
                                     handle-ajax-response
                                     "POST"
                                     xhr-json)))]
      (e/listen body e/EventType.SUBMIT cb))))

(defonce success-msg
  (when-let [message (cookies/get "contact-message")]
    (when-let [success (.getElementById js/document "success")]
      (set! (.-innerHTML success) message)
      (style/setStyle success "display" "block"))
    (cookies/remove "contact-message" "/")))
