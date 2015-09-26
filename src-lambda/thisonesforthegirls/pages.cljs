(ns thisonesforthegirls.pages
  (:require [datascript.core :as d]
            [hiccups.runtime])
  (:import [goog.date Date])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))


(defn base-template
  ([main-content]
   (base-template main-content []))
  ([main-content end-content]
   (let [year (.getYear (Date.))]
     (html
      [:html {:xmlns "http://www.w3.org/1999/xhtml"}
       [:head
        [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
        [:meta {:http-equiv "Content-Language" :content "en-US"}]
        [:link {:href "/css/girls.css" :media "screen"
                :rel "stylesheet" :type "text/css"}]]
       (into
        [:body
         (into
          [:div#wrapper
           [:div#header]
           [:div#navContainer
            [:div#navTop]
            [:div#nav
             [:ul
              [:li [:a {:href "/"} "Welcome"]]
              [:li [:a {:href "/about"} "About Us"]]
              [:li [:a {:href "/devotions"} "Devotions"]]
              [:li [:a {:href "/scripture"} "Scripture"]]
              [:li [:a {:href "/testimonies"} "Testimonies"]]
              [:li [:a {:href "/resournces"} "Community " [:span "Resources"]]]
              [:li [:a {:href "/contact"} "Contact Us"]]]]
            [:div#navVines]
            [:div#navBottom]]]
          (conj
           main-content
           [:div#footer
            [:ul
             [:li [:a {:href "/"} "Welcome"]]
             [:li [:span.sep "|"] [:a {:href "/about"} "About Us"]]
             [:li [:span.sep "|"] [:a {:href "/devotions"} "Devotions"]]
             [:li [:span.sep "|"] [:a {:href "/scripture"} "Scripture"]]
             [:li [:span.sep "|"] [:a {:href "/testimonies"} "Testimonies"]]
             [:li [:span.sep "|"] [:a {:href "/resources"}
                                   "Community Resources"]]
             [:li [:span.sep "|"] [:a {:href "/contact"} "Contact Us"]]]
            [:p (str "&copy; " year " - this One's for the girls")]]))]
        end-content)]))))

(defn admin-template
  [_]  ;; for consistency
  (base-template
   [[:div#admin
     [:img {:src "/img/administration.gif" :alt "Administration"}]]
    [:ul.adminFooter
     [:li [:a {:href "/admin"} "Admin Home"]]
     [:li [:span.sep "|"] [:a {:href "/admin/welcome"} "Welcome"]]
     [:li [:span.sep "|"] [:a {:href "/admin/about"} "About Us"]]
     [:li [:span.sep "|"] [:a {:href "/admin/devotions"} "Devotions"]]
     [:li [:span.sep "|"] [:a {:href "/admin/scripture"} "Scripture"]]
     [:li [:span.sep "|"] [:a {:href "/admin/testimonies"} "Testimonies"]]
     [:li [:span.sep "|"] [:a {:href "/admin/resources"}
                           "Community Resources"]]
     [:li [:span.sep "|"] [:a {:href "/admin/contact"} "Contact Us"]]
     [:li [:span.sep "|"] [:a {:href "/admin/logout"} "Log out"]]]]))

(defn site-template
  [main-content]
  (base-template
   [(into [:div#content] main-content)]
   [[:script {:src "http://www.google-analytics.com/ga.js"
              :type "text/javascript"}]
    [:script {:type "text/javascript"}
     (str "try { _gat._getTracker(\"UA-8266354-4\");"
          "pageTracker._trackPageview(); } catch(err) {}")]]))

(def text-query '[:find ?text . :in $ ?e :where [?e :page/text ?text]])

(defn home
  [db]
  (let [text (d/q text-query db [:db/ident :home])]
    (site-template [[:div#welcome [:img.header {:src "/img/welcome.gif" :alt "Welcome"}]
                     [:p text]
                     [:img#youare {:href "/img/youare.gif"
                                   :alt "You are loved..."}]]])))

(defn about-us
  [db]
  (let [text (d/q text-query db [:db/ident :about-us])]
    (site-template [[:div#about
                     [:img.header {:src "/img/aboutUs.gif" :alt "About Us"}]
                     [:p text]]])))

(defn resources
  [db]
  (let [text (d/q text-query db [:db/ident :resources])]
    (site-template [[:div#resources
                     [:img.header {:src "/img/communityResources.gif"
                                   :alt "Community Resources"}]
                     [:p text]]])))

(defn devotion-markup
  [dev]
  [[:dt
     [:span.dTitle (:devotion/title dev)]
    [:span.dAuthor (str "by " (:devotion/author dev))]]
   [:dd (:devotion/body dev)]])

(defn featured-devotion
  [db]
  (let [devotion (d/q '[:find (pull ?e [*]) .
                        :where [?e :devotion/featured? true]] db)]
    (site-template [[:div#devotions
                     [:img.header {:src "/img/devotions.gif" :alt "Devotions"}]
                     (into [:dl] (devotion-markup devotion))
                     [:p [:a {:href "/devotions/archive"} "Read more"]]]])))

(defn devotion-list-item
  [dev]
  [:li [:a {:href (str "#" (:db/id dev))}]])

(defn archived-devotions
  [db]
  (let [devotions (->> (d/q '[:find [(pull ?e [*]) ...]
                              :where [?e :devotion/featured? false]] db)
                       (sort-by :devotion/created-at #(compare %2 %1)))]
    (site-template [[:div#devotions
                     [:img.header {:src "/img/devotions.gif" :alt "Devotions"}]
                     [:a {:href "/devotions"} "Back to Featured Devotion"]
                     [:h3 "Archive"]
                     [:ul (map devotion-list-item devotions)]
                     [:dl (mapcat devotion-markup devotions)]
                     [:a {:href "/devotions"} "Back to Featured Devotion"]]])))

(defn scripture-category-list-item
  [category]
  [:li [:a {:href (str "/scripture/" (:scripture-category/slug category))}]])

(defn scripture-categories
  [db]
  (let [categories (->> (d/q '[:find [(pull ?e [*]) ...]
                               :where [?e :scripture-category/name]] db)
                        (sort-by :scripture-category/name))]
    (site-template [[:div#scripture
                     [:img.header {:src "/img/scripture.gif" :alt "Scripture"}]
                     [:ul (map scripture-category-list-item categories)]]])))

(def contact-us
  (site-template [[:div#contact
                   [:img.header {:src "/img/contactUs.gif" :alt "Contact Us"}]
                   [:form {:enctype "application/x-www-form-urlencoded"
                           :action "/contact/send"
                           :method "post"}
                    [:dl
                     [:dt [:label {:for "name"} "Your name"]]
                     [:dd#name [:input {:type "text" :name "name"}]]
                     [:dt [:label {:for "return"} "Your email address"]]
                     [:dd#return [:input {:type "text" :name "return"}]]
                     [:dt [:label {:for "body"} "Message"]]
                     [:dd [:textarea#body {:name "body" :rows "24"
                                           :cols "80"}]]
                     [:dt "&nbsp;"]
                     [:dd [:input#submit {:type "submit" :name "submit"
                                          :value "Submit"}]]]]]]))

(defn all-page-info
  [db]
  (let [defined [{:s3-key "home"
                  :body (home db)}
                 {:s3-key "about-us"
                  :body (about-us db)}
                 {:s3-key "resources"
                  :body (resources db)}
                 {:s3-key "devotions"
                  :body (featured-devotion db)}
                 {:s3-key "devotions/archive"
                  :body (archived-devotions db)}
                 {:s3-key "scripture"
                  :body (scripture-categories db)}
                 {:s3-key "contact-us"
                  :body contact-us}]]
    defined))
