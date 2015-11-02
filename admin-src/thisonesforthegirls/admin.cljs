(ns thisonesforthegirls.admin
  (:require [clojure.string :as s]
            [goog.dom :as dom]
            [goog.dom.forms :as f]
            [goog.events :as e]
            [goog.net.cookies :as cookies]
            [goog.object :as gobj]
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
  (when-let [success (.getElementById js/document "success")]
    (set! (.-innerHTML success) message)
    (style/setStyle success "display" "block"))
  (when-let [error (.getElementById js/document "error")]
    (style/setStyle error "display" "none")))

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
            (cookies/set "jwt" (gobj/get json "jwt") -1 "/admin")
            (.reload (.-location js/window)))
          ;; other form processing
          (if-let [redirect (gobj/get json "redirect")]
            (do
              (cookies/set "message" (gobj/get json "success") -1 "/admin")
              (set! (.-href (.-location js/window)) redirect))
            (show-success (gobj/get json "success"))))
        ;; general error processing
        (do
          (when-let [error-p (.getElementById js/document "error")]
            (set! (.-innerHTML error-p) (gobj/get json "errorMessage"))
            (style/setStyle error-p "display" "block"))
          (when-let [success (.getElementById js/document "success")]
            (style/setStyle success "display" "none")))))))

(defonce form-submitter
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
                        xhr-json (->> (.-map_ form-data)
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
