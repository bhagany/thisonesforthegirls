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

(defn main
  [page-url]
  (let [loc (.. js/window -location -href)
        xhr-json (.stringify js/JSON
                             #js {:path (.getPath (goog.Uri.parse loc))})
        jwt (cookies/get "jwt")
        headers (if jwt
                  #js {:x-jwt jwt}
                  #js {})]
    (letfn [(cb [response]
              (let [frag (dom/htmlToDocumentFragment
                          (.getResponseText (.-target response)))
                    node (.getElementById js/document "admin")]
                (.appendChild node frag)))]
      (goog.net.XhrIo.send page-url cb "POST" xhr-json headers))))

(defn handle-ajax-response
  [action]
  (fn
    [response]
    (let [xhr (.-target response)]
      (if (< (.getStatus xhr) 300)
        (let [text (.getResponseText xhr)]
          (if (s/ends-with? action "login")
            ;; login processing
            (do
              (cookies/set "jwt" text)
              (.reload (.-location js/window)))
            ;; other form processing
            (let [success (.getElementById js/document "success")]
              (set! (.-innerHTML success) text)
              (style/setStyle success "display" "block"))))
        ;; general error processing
        (let [resp-json (.getResponseJson xhr)
              error-p (.getElementById js/document "error")]
          (set! (.-innerHTML error-p) (.-errorMessage resp-json))
          (style/setStyle error-p "display" "block"))))))

(defn form-submitter
  []
  (let [body (aget (dom/getElementsByTagNameAndClass "body") 0)]
    (letfn [(cb [e]
              (let [form (aget (dom/getElementsByTagNameAndClass "form") 0)
                    method (.getAttribute form "method")]
                (when (= method "POST")
                  (.preventDefault e)
                  (let [action (.getAttribute form "action")
                        form-data (f/getFormDataMap form)
                        loc (.. js/window -location -href)
                        path (.getPath (goog.Uri.parse loc))
                        jwt (cookies/get "jwt")
                        headers (if jwt
                                  #js {:x-jwt jwt}
                                  #js {})
                        xhr-json (->> (goog.object/get form-data "map_")
                                      js->clj
                                      (map (fn [[k v]] [k (v 0)]))
                                      (into {:path path})
                                      clj->js
                                      (.stringify js/JSON))]
                    (goog.net.XhrIo.send action
                                         (handle-ajax-response action)
                                         "POST"
                                         xhr-json
                                         headers)))))]
      (e/listen body e/EventType.SUBMIT cb))))

(defonce get-page
  (main admin-page-url))

(defonce submit-listner
  (form-submitter))
