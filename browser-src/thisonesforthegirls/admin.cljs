(ns thisonesforthegirls.admin
  (:require [goog.dom :as dom])
  (:import [goog.net XhrIo]
           [goog Uri]))

;; This must be set in your build
(goog-define admin-page-url "http://your.api.com")

(defn main
  [page-url]
  (let [loc (.. js/window -location -href)
        xhr-json (.stringify js/JSON
                             #js {:path (.getPath (goog.Uri.parse loc))})]
    (letfn [(cb [response]
              (let [frag (dom/htmlToDocumentFragment
                          (.getResponseText (.-target response)))
                    node (.getElementById js/document "admin")]
                (.appendChild node frag)))]
      (goog.net.XhrIo.send page-url cb "POST" xhr-json))))

(defonce get-page
  (do (main admin-page-url)))
