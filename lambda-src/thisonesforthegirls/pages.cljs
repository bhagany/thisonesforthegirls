(ns thisonesforthegirls.pages
  (:require [cljs.core.async :refer [<!]]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [hiccups.runtime]
            [markdown.core :refer [md->html]]
            [thisonesforthegirls.db :as db])
  (:import [goog.date Date])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [hiccups.core :as hiccups :refer [html]]))

;; Templates and helpers

(defn base-template
  ([title main-content]
   (base-template title main-content []))
  ([title main-content end-content]
   (let [year (.getYear (Date.))]
     (str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
          "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
          (html
           [:html {:xmlns "http://www.w3.org/1999/xhtml"}
            [:head
             [:title (str title " | This One's for the Girls")]
             [:meta {:http-equiv "Content-Type"
                     :content "text/html; charset=UTF-8"}]
             [:meta {:http-equiv "Content-Language" :content "en-US"}]
             [:link {:href "/assets/girls.css" :media "screen"
                     :rel "stylesheet" :type "text/css"}]
             [:link {:rel "icon" :href "/assets/favicon.png"}]]
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
                   [:li [:a {:href "/community-resources"} "Community "
                         [:span "Resources"]]]
                   [:li [:a {:href "/contact"} "Contact Us"]]]]
                 [:div#navVines]
                 [:div#navBottom]]]
               (conj
                main-content
                [:div#footer
                 [:ul
                  [:li [:a {:href "/"} "Welcome "]]
                  [:li [:span.sep "|"] [:a {:href "/about"} " About Us "]]
                  [:li [:span.sep "|"] [:a {:href "/devotions"} " Devotions "]]
                  [:li [:span.sep "|"] [:a {:href "/scripture"} " Scripture "]]
                  [:li [:span.sep "|"] [:a {:href "/testimonies"}
                                        " Testimonies "]]
                  [:li [:span.sep "|"] [:a {:href "/community-resources"}
                                        " Community Resources "]]
                  [:li [:span.sep "|"] [:a {:href "/contact"} " Contact Us"]]]
                 [:p (str "&copy; " year " - this One's for the girls")]]))]
             end-content)])))))

(defn admin-template
  [title]
  (base-template
   title
   [[:div#admin
     [:img {:src "/assets/administration.gif" :alt "Administration"}]]]
   [[:script {:type "text/javascript" :src "/assets/js/admin.js"}]]))

(defn site-template
  [title main-content]
  (base-template
   title
   [(into [:div#content] main-content)]
   [[:script {:src "http://www.google-analytics.com/ga.js"
              :type "text/javascript"}]
    [:script {:type "text/javascript"}
     (str "try { _gat._getTracker(\"UA-8266354-4\");"
          "pageTracker._trackPageview(); } catch(err) {}")]]))

(def text-query '[:find ?text . :in $ ?e :where [?e :page/text ?text]])

(defn devotion-markup
  [dev]
  [[:dt
     [:span.dTitle (:devotion/title dev)]
    [:span.dAuthor (str "by " (:devotion/author dev))]]
   [:dd (:devotion/body dev)]])

(defn devotion-list-item
  [dev]
  [:li [:a {:href (str "#" (:db/id dev))}]])

(defn scripture-category-list-item
  [category]
  [:li [:a {:href (str "/scripture/" (:scripture-category/slug category))}]])

(defn scripture-markup
  [scripture]
  [[:dt
    [:label (:scripture/reference scripture)]]
   [:dd (:scripture/text scripture)]])

(defn scripture-category
  [category]
  {:s3-key (str "/scripture/" (:scripture-category/slug category))
   :body (site-template
          (str "Scripture / " (:scripture-category/name category))
          [[:div#scripture
            [:img.header {:src "/assets/scripture.gif"
                          :alt "Scripture"}]
            [:dl (mapcat scripture-markup
                         (:scripture/_category category))]
            [:p [:a {:href "/scripture"}
                 "Back to Categories"]]]])})

(defn testimony-list-item
  [testimony]
  [:li [:a {:href (str "/testimonies/" (:testimony/slug testimony))}]])

(defn testimony
  [testimony]
  {:s3-key (str "/testimonies/" (:testimony/slug testimony))
   :body (site-template
          (str "Testimonies / " (:testimony/title testimony))
          [[:div#testimonies
            [:img.header {:src "/assets/testimonies.gif"
                          :alt "Testimonies"}]
            [:p [:a {:href "/testimonies"}
                 "Back to Testimony Links"]]
            [:dl
             [:dt (:testimony/title testimony)]
             [:dd (:testimony/text testimony)]]
            [:p [:a {:href "/testimonies"}
                 "Back to Testimony Links"]]]])})

(def contact-us
  (site-template
   "Contact Us"
   [[:div#contact
     [:img.header {:src "/assets/contact-us.gif" :alt "Contact Us"}]
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

(def error-fragment
  [:div.error
   [:h1 "Error"]
   [:p "We couldn't find what you were looking for"]
   [:p [:a {:href "/"} "Go back to the home page"]]])

(def error
  (site-template
   "Error"
   [error-fragment]))

;; Admin page fragments

(def admin-footer
  [:ul.adminFooter
   [:li [:a {:href "/admin"} "Admin Home "]]
   [:li [:span.sep "|"] [:a {:href "/admin/welcome"} " Welcome "]]
   [:li [:span.sep "|"] [:a {:href "/admin/about"} " About Us "]]
   [:li [:span.sep "|"] [:a {:href "/admin/devotions"} " Devotions "]]
   [:li [:span.sep "|"] [:a {:href "/admin/scripture"} " Scripture "]]
   [:li [:span.sep "|"] [:a {:href "/admin/testimonies"} " Testimonies "]]
   [:li [:span.sep "|"] [:a {:href "/admin/community-resources"}
                         " Community Resources "]]
   [:li [:span.sep "|"] [:a {:href "/admin/contact"} " Contact Us "]]
   [:li [:span.sep "|"] [:a {:href "/admin/logout"} " Log out "]]])

;; TODO: I need to know the url for lambda functions here
;; Possibly should make pages a component

(defrecord PageConfig [lambda-base db])

(defn pages
  [lambda-base]
  (component/using
   (map->PageConfig {:lambda-base lambda-base})
   [:db]))

(defn home
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (md->html (or (d/q text-query @conn [:db/ident :home])
                             ""))
          page (site-template
                "Welcome"
                [[:div#welcome [:img.header {:src "/assets/welcome.gif" :alt "Welcome"}]
                  "this text will not occur"
                  [:img#youare {:src "/assets/you-are.gif"
                                :alt "You are loved..."}]]])]
      (s/replace page "this text will not occur" text))))

(defn about-us
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (md->html (or (d/q text-query @conn [:db/ident :about-us])
                             ""))
          page (site-template
                "About Us"
                [[:div#about
                  [:img.header {:src "/assets/about-us.gif" :alt "About Us"}]
                  "this text will not occur"]])]
      (s/replace page "this text will not occur" text))))

(defn resources
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (md->html (or (d/q text-query @conn [:db/ident :resources])
                             ""))
          page (site-template
                "Community Resources"
                [[:div#resources
                  [:img.header {:src "/assets/community-resources.gif"
                                :alt "Community Resources"}]
                  "this text will not occur"]])]
      (s/replace page "this text will not occur" text))))

(defn featured-devotion
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          devotion (d/q '[:find (pull ?e [*]) .
                          :where [?e :devotion/featured? true]] @conn)]
      (site-template
       "Devotions"
       [[:div#devotions
         [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
         (into [:dl] (devotion-markup devotion))
         [:p [:a {:href "/devotions/archive"} "Read more"]]]]))))

(defn archived-devotions
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          devotions (->> (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e :devotion/featured? false]] @conn)
                         (sort-by :devotion/created-at #(compare %2 %1)))]
      (site-template
       "Devotions Archive"
       [[:div#devotions
         [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
         [:a {:href "/devotions"} "Back to Featured Devotion"]
         [:h3 "Archive"]
         [:ul (map devotion-list-item devotions)]
         [:dl (mapcat devotion-markup devotions)]
         [:a {:href "/devotions"} "Back to Featured Devotion"]]]))))

(defn scripture-categories
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          categories (->> (d/q '[:find [(pull ?e [*]) ...]
                                 :where [?e :scripture-category/name]] @conn)
                          (sort-by :scripture-category/name))]
      (site-template
       "Scripture"
       [[:div#scripture
         [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
         [:ul (map scripture-category-list-item categories)]]]))))

(defn testimonies
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          testimonies (->> (d/q '[:find [(pull ?e [*]) ...]
                                  :where [?e :testimony/title]] @conn)
                           (sort-by :testimony/title))]
      (site-template
       "Testimonies"
       [[:div#testimonies
         [:img.header {:src "/assets/testimonies.gif" :alt "Testimonies"}]
         [:ul (map testimony-list-item testimonies)]]]))))

(defn all-page-info
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          defined [{:s3-key "home"
                    :body (<! (home pages))}
                   {:s3-key "about"
                    :body (<! (about-us pages))}
                   {:s3-key "community-resources"
                    :body (<! (resources pages))}
                   {:s3-key "devotions"
                    :body (<! (featured-devotion pages))}
                   {:s3-key "devotions/archive"
                    :body (<! (archived-devotions pages))}
                   {:s3-key "scripture"
                    :body (<! (scripture-categories pages))}
                   {:s3-key "testimonies"
                    :body (<! (testimonies pages))}
                   {:s3-key "contact"
                    :body contact-us}
                   {:s3-key "error"
                    :body error}
                   {:s3-key "admin"
                    :body (admin-template "Admin")}
                   {:s3-key "admin/welcome"
                    :body (admin-template "Welcome Admin")}
                   {:s3-key "admin/about"
                    :body (admin-template "About Us Admin")}
                   {:s3-key "admin/community-resources"
                    :body (admin-template "Community Resources Admin")}
                   {:s3-key "admin/devotions"
                    :body (admin-template "Devotions Admin")}
                   {:s3-key "admin/scripture"
                    :body (admin-template "Scripture Admin")}
                   {:s3-key "admin/testimonies"
                    :body (admin-template "Testimonies Admin")}]
          s-cats (->> (d/q '[:find [(pull ?e [* {:scripture/_category [*]}]) ...]
                             :where [?e :scripture-category/name]] @conn)
                      (map scripture-category))
          tmonies (->> (d/q '[:find [(pull ?e [*]) ...]
                              :where [?e :testimony/title]] @conn)
                       (map testimony))]
      (-> defined
          (into s-cats)
          (into tmonies)))))

(defn login-form
  [pages]
  (let [{:keys [lambda-base]} pages]
    (html
     [:img.header {:src "/assets/login.gif" :alt "Login"}]
     [:p#error]
     [:form {:action (str lambda-base "login") :method "post"}
      [:dl
       [:dt [:label {:for "username"} "Username"]]
       [:dd [:input {:type "text" :name "username"}]]
       [:dt [:label {:for "password"} "Password"]]
       [:dd [:input {:type "password" :name "password"}]]
       [:dt "&nbsp;"]
       [:dd [:input {:type "submit" :value "Login"}]]]])))

(defn admin
  [pages]
  (html
   [:h2 "Click on the links below to do stuff"]
   [:p
    "Hello TOFTG Administrators! The links on the left will take you to the "
    "regular site. The links below will allow you to modify the regular site. "
    "Choose accordingly"]
   [:h4 "Administration Links"]
   admin-footer))

(defn admin-basic
  [ident header-img text-label]
  (fn [pages]
    (go
      (let [{:keys [lambda-base db]} pages
            conn (<! (:conn-ch db))
            text (d/q text-query @conn [:db/ident ident])]
        (html
         [:img.header header-img]
         [:p#error]
         [:h2#success]
         [:form {:action (str lambda-base "edit-page") :method "post"}
          [:dl
           [:dt [:label {:for "text"} text-label]]
           [:dd [:textarea {:name "text" :rows "24" :cols "80"} text]]
           [:dt "&nbsp;"]
           [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
         admin-footer)))))

(def admin-home (admin-basic :home
                             {:src "/assets/welcome.gif" :alt "Welcome"}
                             "Welcome Message"))

(def admin-about-us (admin-basic :about-us
                                 {:src "/assets/about-us.gif" :alt "About Us"}
                                 "About Us Text"))

(def admin-resources (admin-basic :resources
                                  {:src "/assets/community-resources.gif"
                                   :alt "Community Resources"}
                                  "Community Resources Text"))

(def admin-error (html error-fragment))

;; Editing functions

(defn edit-basic
  [ident]
  (fn [pages event]
    (go
      (let [{:keys [db]} pages
            {:keys [text]} event
            [err] (<! (db/transact!-ch
                       db
                       [{:db/id -1
                         :db/ident ident
                         :page/text text}]))]
        (if err
          (js/Error. err)
          "The text was successfully edited")))))

(def edit-home (edit-basic :home))

(def edit-about-us (edit-basic :about-us))

(def edit-resources (edit-basic :resources))
