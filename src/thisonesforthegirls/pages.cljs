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

(def admin-template
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

(def text-query '[:find ?text :in $ ?e :where [?e :page/text ?text]])

(defn home
  [db]
  (let [text (d/q text-query db [:db/ident :home])]
    (site-template [[:img {:class "header" :src "/img/welcome.gif"
                           :alt "Welcome"}]
                    [:p text]
                    [:img#youare {:href "/img/youare.gif" :alt "You are loved..."}]])))

(defn about-us
  [db]
  (let [text (d/q text-query db [:db/ident :home])]
    (site-template [[:img {:class "header" :src "/img/aboutUs.gif"
                           :alt "About Us"}]
                    [:p text]])))

(defn resources
  [db]
  (let [text (d/q text-query db [:db/ident :home])]
    (site-template [[:img {:class "header" :src "/img/communityResources.gif"
                           :alt "Community Resources"}]
                    [:p text]])))

(defn contact-us
  [_]  ;; for consistency
  (site-template [[:div#contact
                   [:img {:class "header" :src "/img/contactUs.gif"
                          :alt "Contact Us"}]
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

(def all-page-info [{:fn home
                     :s3-key "home"}
                    {:fn about-us
                     :s3-key "about-us"}
                    {:fn resources
                     :s3-key "resources"}
                    {:fn contact-us
                     :s3-key "contact-us"}])
