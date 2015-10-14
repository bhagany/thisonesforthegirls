(ns thisonesforthegirls.pages
  (:require [datascript.core :as d]
            [hiccups.runtime])
  (:import [goog.date Date])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))


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
     [:img {:src "/assets/administration.gif" :alt "Administration"}]
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
      [:li [:span.sep "|"] [:a {:href "/admin/logout"} " Log out "]]]]]
   [[:script {:type "text/javascript" :src "/assets/admin.js"}]]))

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

(defn home
  [db]
  (let [text (d/q text-query db [:db/ident :home])]
    (site-template
     "Welcome"
     [[:div#welcome [:img.header {:src "/assets/welcome.gif" :alt "Welcome"}]
       [:p text]
       [:img#youare {:src "/assets/you-are.gif"
                     :alt "You are loved..."}]]])))

(defn about-us
  [db]
  (let [text (d/q text-query db [:db/ident :about-us])]
    (site-template
     "About Us"
     [[:div#about
       [:img.header {:src "/assets/about-us.gif" :alt "About Us"}]
       [:p text]]])))

(defn resources
  [db]
  (let [text (d/q text-query db [:db/ident :resources])]
    (site-template
     "Community Resources"
     [[:div#resources
       [:img.header {:src "/assets/community-resources.gif"
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
    (site-template
     "Devotions"
     [[:div#devotions
       [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
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
    (site-template
     "Devotions Archive"
     [[:div#devotions
       [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
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
    (site-template
     "Scripture"
     [[:div#scripture
       [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
       [:ul (map scripture-category-list-item categories)]]])))

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

(defn testimonies
  [db]
  (let [testimonies (->> (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e :testimony/title]] db)
                         (sort-by :testimony/title))]
    (site-template
     "Testimonies"
     [[:div#testimonies
       [:img.header {:src "/assets/testimonies.gif" :alt "Testimonies"}]
       [:ul (map testimony-list-item testimonies)]]])))

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

(def error
  (site-template
   "Error"
   [[:div#error
     [:h1 "Error"]
     [:p "We couldn't find what you were looking for"]
     [:p [:a {:href "/"} "Go back to the home page"]]]]))

(defn all-page-info
  [db]
  (let [defined [{:s3-key "home"
                  :body (home db)}
                 {:s3-key "about"
                  :body (about-us db)}
                 {:s3-key "community-resources"
                  :body (resources db)}
                 {:s3-key "devotions"
                  :body (featured-devotion db)}
                 {:s3-key "devotions/archive"
                  :body (archived-devotions db)}
                 {:s3-key "scripture"
                  :body (scripture-categories db)}
                 {:s3-key "testimonies"
                  :body (testimonies db)}
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
                           :where [?e :scripture-category/name]] db)
                    (map scripture-category))
        tmonies (->> (d/q '[:find [(pull ?e [*]) ...]
                            :where [?e :testimony/title]] db)
                     (map testimony))]
    (-> defined
        (into s-cats)
        (into tmonies))))