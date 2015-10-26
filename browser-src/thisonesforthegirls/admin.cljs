(ns thisonesforthegirls.admin
  (:require [clojure.string :as s]
            [goog.dom :as dom]
            [goog.dom.forms :as f]
            [goog.events :as e]
            [goog.net.cookies :as cookies]
            [goog.style :as style])
  (:import [goog.net XhrIo]
           [goog Uri]))

;; This must be set in your build
(goog-define admin-page-url "http://your.api.com")

(defn path-and-query
  []
  (let [loc (.. js/window -location -href)
        uri (goog.Uri.parse loc)]
    {:path (.getPath uri)
     :query (str "?" (.getQuery uri))}))

(defn show-success
  [message]
  (let [success (.getElementById js/document "success")]
    (when success
      (set! (.-innerHTML success) message)
      (style/setStyle success "display" "block"))))

(defn main
  [page-url]
  (let [xhr-json (.stringify js/JSON (clj->js (path-and-query)))
        jwt (cookies/get "jwt")
        headers (if jwt
                  #js {:x-jwt jwt}
                  #js {})]
    (letfn [(cb [response]
              (let [frag (dom/htmlToDocumentFragment
                          (.getResponseText (.-target response)))
                    node (.getElementById js/document "admin")]
                (.appendChild node frag)
                (when-let [message (cookies/get "message")]
                  (show-success message)
                  (cookies/remove "message" "/admin"))))]
      (goog.net.XhrIo.send page-url cb "POST" xhr-json headers))))

(defn handle-ajax-response
  [action]
  (fn
    [response]
    (let [xhr (.-target response)
          json (.getResponseJson xhr)]
      (if (< (.getStatus xhr) 300)
        (if (s/ends-with? action "login")
          ;; login processing
          (do
            (cookies/set "jwt" (.-jwt json) -1 "/admin")
            (.reload (.-location js/window)))
          ;; other form processing
          (if-let [redirect (.-redirect json)]
            (do
              (cookies/set "message" (.-success json) -1 "/admin")
              (set! (.-href (.-location js/window)) redirect))
            (show-success (.-success json))))
        ;; general error processing
        (let [error-p (.getElementById js/document "error")]
          (set! (.-innerHTML error-p) (.-errorMessage json))
          (style/setStyle error-p "display" "block"))))))

(defn form-submitter
  []
  (let [body (aget (dom/getElementsByTagNameAndClass "body") 0)]
    (letfn [(cb [e]
              (let [form (.-target e)
                    method (.getAttribute form "method")]
                (when (= method "post")
                  (.preventDefault e)
                  (let [action (.getAttribute form "action")
                        form-data (f/getFormDataMap form)
                        jwt (cookies/get "jwt")
                        headers (if jwt
                                  #js {:x-jwt jwt}
                                  #js {})
                        xhr-json (->> (goog.object/get form-data "map_")
                                      js->clj
                                      (map (fn [[k v]] [k (v 0)]))
                                      (into (path-and-query))
                                      clj->js
                                      (.stringify js/JSON))]
                    (goog.net.XhrIo.send action
                                         (handle-ajax-response action)
                                         "POST"
                                         xhr-json
                                         headers)))))]
      (e/listen body e/EventType.SUBMIT cb))))

(defonce logout
  (let [body (aget (dom/getElementsByTagNameAndClass "body") 0)]
    (letfn [(cb [e]
              (when (= "logout" (.getAttribute (.-target e) "id"))
                (.preventDefault e)
                (cookies/remove "jwt" "/admin")
                (.reload (.-location js/window))))]
      (e/listen body e/EventType.CLICK cb))))

(defonce get-page
  (main admin-page-url))

(defonce submit-listener
  (form-submitter))
