(ns thisonesforthegirls.pages
  (:require [cljs.core.async :as async :refer [<!]]
            [clojure.string :as s]
            [cuerdas.core :as str]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [goog.string :as gs]
            [hiccups.runtime]
            [markdown.core :refer [md->html]]
            [thisonesforthegirls.db :as db]
            [thisonesforthegirls.s3 :as s3])
  (:import [goog.date Date]
           [goog Uri])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [hiccups.core :as hiccups :refer [html]]))

;; Templates and helpers

(defn base-template
  ([title main-content]
   (base-template title main-content []))
  ([title main-content end-content]
   (let [year (.getYear (Date.))
         t (gs/htmlEscape title)]
     (str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
          "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
          (html
           [:html {:xmlns "http://www.w3.org/1999/xhtml"}
            [:head
             [:title (str t " | This One's for the Girls")]
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

(defn devotion-markup
  [dev]
  (let [title (gs/htmlEscape (:devotion/title dev))
        author (gs/htmlEscape (:devotion/author dev))
        body (gs/htmlEscape (:devotion/body dev))]
    [[:dt {:id (:devotion/slug dev)}
      [:span.dTitle title]
      [:span.dAuthor (str "by " author)]]
     [:dd (md->html body)]]))

(defn devotion-list-item
  [dev]
  (let [title (gs/htmlEscape (:devotion/title dev))]
    [:li [:a {:href (str "#" (:devotion/slug dev))} title]]))

(defn scripture-category-list-item
  [category]
  (let [name (gs/htmlEscape (:scripture-category/name category))]
    [:li [:a {:href (str "/scripture/" (:scripture-category/slug category))}
          name]]))

(defn scripture-markup
  [scripture]
  (let [reference (gs/htmlEscape (:scripture/reference scripture))
        text (gs/htmlEscape (:scripture/text scripture))]
    [[:dt
      [:label reference]]
     [:dd (md->html text)]]))

(defn scripture-category*
  [entity]
  {:path (str "scripture/" (:scripture-category/slug entity))
   :content (site-template
             (str "Scripture | " (:scripture-category/name entity))
             [[:div#scripture
               [:img.header {:src "/assets/scripture.gif"
                             :alt "Scripture"}]
               (when-not (empty? (:scripture/_category entity))
                 [:dl (mapcat scripture-markup
                              (:scripture/_category entity))])
               [:p [:a {:href "/scripture"} "Back to Categories"]]]])})

(defn scripture-category
  [entity]
  (fn [_]
    (go (scripture-category* entity))))

(defn testimony-list-item
  [testimony]
  (let [title (gs/htmlEscape (:testimony/title testimony))]
    [:li [:a {:href (str "/testimonies/" (:testimony/slug testimony))}
          title]]))

(defn testimony*
  [entity]
  {:path (str "testimonies/" (:testimony/slug entity))
   :content (site-template
             (str "Testimonies | " (:testimony/title entity))
             (let [title (gs/htmlEscape (:testimony/title entity))
                   body (gs/htmlEscape (:testimony/body entity))]
               [[:div#testimonies
                 [:img.header {:src "/assets/testimonies.gif"
                               :alt "Testimonies"}]
                 [:p [:a {:href "/testimonies"}
                      "Back to Testimony Links"]]
                 [:dl
                  [:dt title]
                  [:dd (md->html body)]]
                 [:p [:a {:href "/testimonies"}
                      "Back to Testimony Links"]]]]))})

(defn testimony
  [entity]
  (fn [_]
    (go (testimony* entity))))

(defn contact-us
  [pages]
  (let [{:keys [lambda-base]} pages]
    (site-template
     "Contact Us"
     [[:div#contact
       [:img.header {:src "/assets/contact-us.gif" :alt "Contact Us"}]
       [:p#error]
       [:h2#success]
       [:form {:action (str lambda-base "send-email")
               :method "post"}
        [:dl
         [:dt [:label {:for "name"} "Your name"]]
         [:dd#name [:input {:type "text" :name "name"}]]
         [:dt [:label {:for "reply-to"} "Your email address"]]
         [:dd#return [:input {:type "text" :name "reply-to"}]]
         [:dt [:label {:for "message"} "Message"]]
         [:dd [:textarea#body {:name "message" :rows "24"
                               :cols "80"}]]
         [:dt "&nbsp;"]
         [:dd [:input#submit {:type "submit" :name "submit"
                              :value "Submit"}]]]]]
      [:script {:type "text/javascript" :src "/assets/js/contact.js"}]])))

(def error-fragment
  [:div.error
   [:h1 "Error"]
   [:p "We couldn't find what you were looking for"]
   [:p [:a {:href "/"} "Go back to the home page"]]])

(def error
  (site-template
   "Error"
   [error-fragment]))

;; Component and dependent functions

(defrecord PageConfig [lambda-base html-bucket db s3-conn])

(defn pages
  [lambda-base html-bucket]
  (component/using
   (map->PageConfig {:lambda-base lambda-base :html-bucket html-bucket})
   [:db :s3-conn]))

(def text-query '[:find ?text . :in $ ?e :where [?e :page/text ?text]])

(defn home
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (gs/htmlEscape (d/q text-query @conn [:db/ident :home]))
          page (site-template
                "Welcome"
                [[:div#welcome [:img.header {:src "/assets/welcome.gif" :alt "Welcome"}]
                  (md->html text)
                  [:img#youare {:src "/assets/you-are.gif"
                                :alt "You are loved..."}]]])]
      {:path "home" :content page})))

(defn about-us
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (gs/htmlEscape (d/q text-query @conn [:db/ident :about-us]))
          page (site-template
                "About Us"
                [[:div#about
                  [:img.header {:src "/assets/about-us.gif" :alt "About Us"}]
                  (md->html text)]])]
      {:path "about" :content page})))

(defn resources
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (gs/htmlEscape (d/q text-query @conn [:db/ident :resources]))
          page (site-template
                "Community Resources"
                [[:div#resources
                  [:img.header {:src "/assets/community-resources.gif"
                                :alt "Community Resources"}]
                  (md->html text)]])]
      {:path "community-resources" :content page})))

(defn featured-devotion
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          devotion (d/q '[:find (pull ?e [*]) .
                          :where [?e :devotion/featured? true]] @conn)
          content (site-template
                   "Devotions"
                   [[:div#devotions
                     [:img.header {:src "/assets/devotions.gif"
                                   :alt "Devotions"}]
                     (when devotion
                       (into [:dl] (devotion-markup devotion)))
                     [:p [:a {:href "/devotions/archive"} "Read more"]]]])]
      {:path "devotions" :content content})))

(defn archived-devotions
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          devotions (->> (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e :devotion/featured? false]] @conn)
                         (sort-by :devotion/created-at #(compare %2 %1)))
          content (site-template
                   "Devotions Archive"
                   [[:div#devotions
                     [:img.header {:src "/assets/devotions.gif"
                                   :alt "Devotions"}]
                     [:a {:href "/devotions"} "Back to Featured Devotion"]
                     [:h3 "Archive"]
                     [:ul (map devotion-list-item devotions)]
                     [:dl (mapcat devotion-markup devotions)]
                     [:a {:href "/devotions"} "Back to Featured Devotion"]]])]
      {:path "devotions/archive" :content content})))

(defn scripture-categories
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          categories (->> (d/q '[:find [(pull ?e [*]) ...]
                                 :where [?e :scripture-category/name]] @conn)
                          (sort-by :scripture-category/name))
          content (site-template
                   "Scripture"
                   [[:div#scripture
                     [:img.header {:src "/assets/scripture.gif"
                                   :alt "Scripture"}]
                     [:ul (map scripture-category-list-item categories)]]])]
      {:path "scripture" :content content})))

(defn testimonies
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          testimonies (->> (d/q '[:find [(pull ?e [*]) ...]
                                  :where [?e :testimony/title]] @conn)
                           (sort-by :testimony/title))
          content (site-template
                   "Testimonies"
                   [[:div#testimonies
                     [:img.header {:src "/assets/testimonies.gif"
                                   :alt "Testimonies"}]
                     [:ul (map testimony-list-item testimonies)]]])]
      {:path "testimonies" :content content})))

(defn all-page-info
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          defined [(<! (home pages))
                   (<! (about-us pages))
                   (<! (resources pages))
                   (<! (featured-devotion pages))
                   (<! (archived-devotions pages))
                   (<! (scripture-categories pages))
                   (<! (testimonies pages))
                   {:path "contact"
                    :content (contact-us pages)}
                   {:path "error"
                    :content error}
                   {:path "admin"
                    :content (admin-template "Admin")}
                   {:path "admin/welcome"
                    :content (admin-template "Welcome Admin")}
                   {:path "admin/about"
                    :content (admin-template "About Us Admin")}
                   {:path "admin/community-resources"
                    :content (admin-template "Community Resources Admin")}
                   {:path "admin/devotions"
                    :content (admin-template "Devotions Admin")}
                   {:path "admin/devotions/add"
                    :content (admin-template "Devotions Admin | Add")}
                   {:path "admin/devotions/edit"
                    :content (admin-template "Devotions Admin | Edit")}
                   {:path "admin/devotions/delete"
                    :content (admin-template "Devotions Admin | Delete")}
                   {:path "admin/testimonies"
                    :content (admin-template "Testimonies Admin")}
                   {:path "admin/testimonies/add"
                    :content (admin-template "Testimonies Admin | Add")}
                   {:path "admin/testimonies/edit"
                    :content (admin-template "Testimonies Admin | Edit")}
                   {:path "admin/testimonies/delete"
                    :content (admin-template "Testimonies Admin | Delete")}
                   {:path "admin/scripture/categories"
                    :content (admin-template "Scripture Admin")}
                   {:path "admin/scripture/categories/add"
                    :content (admin-template "Scripture Admin | Add Category")}
                   {:path "admin/scripture/categories/edit"
                    :content (admin-template "Scripture Admin | Edit Category")}
                   {:path "admin/scripture/categories/delete"
                    :content (admin-template "Scripture Admin | Delete Category")}
                   {:path "admin/scripture/add"
                    :content (admin-template "Scripture Admin | Add")}
                   {:path "admin/scripture/edit"
                    :content (admin-template "Scripture Admin | Edit")}
                   {:path "admin/scripture/delete"
                    :content (admin-template "Scripture Admin | Delete")}
                   {:path "admin/scripture"
                    :content (admin-template "Scripture Admin")}
                   {:path "admin/testimonies"
                    :content (admin-template "Testimonies Admin")}
                   {:path "admin/contact"
                    :content (admin-template "Contact Admin")}]
          s-cats (->> (d/q '[:find [(pull ?e [* {:scripture/_category [*]}]) ...]
                             :where [?e :scripture-category/name]] @conn)
                      (map scripture-category*))
          tmonies (->> (d/q '[:find [(pull ?e [*]) ...]
                              :where [?e :testimony/title]] @conn)
                       (map testimony*))]
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

;; Admin page fragments

(def admin-footer
  [:div
   [:h4 "Administration Links"]
   [:ul.adminFooter
    [:li [:a {:href "/admin"} "Admin Home "]]
    [:li [:span.sep "|"] [:a {:href "/admin/welcome"} " Welcome "]]
    [:li [:span.sep "|"] [:a {:href "/admin/about"} " About Us "]]
    [:li [:span.sep "|"] [:a {:href "/admin/devotions"} " Devotions "]]
    [:li [:span.sep "|"] [:a {:href "/admin/scripture/categories"} " Scripture "]]
    [:li [:span.sep "|"] [:a {:href "/admin/testimonies"} " Testimonies "]]
    [:li [:span.sep "|"] [:a {:href "/admin/community-resources"}
                          " Community Resources "]]
    [:li [:span.sep "|"] [:a {:href "/admin/contact"} " Contact Us "]]
    [:li [:span.sep "|"] [:a#logout {:href "/admin/logout"} " Log out "]]]])

(def admin-error (html error-fragment))

(defn get-query-param
  [query key]
  (.get (.getQueryData (goog.Uri. query)) key))

(defn admin
  [pages]
  (html
   [:h2 "Click on the links below to do stuff"]
   [:p
    "Hello TOFTG Administrators! The links on the left will take you to the "
    "regular site. The links below will allow you to modify the regular site. "
    "Choose accordingly"]
   admin-footer))

(defn admin-contact
  [pages]
  (go
    (let [{:keys [db lambda-base]} pages
          conn (<! (:conn-ch db))
          email (-> '[:find ?email .
                      :in $ ?contact
                      :where [?contact :contact/email ?email]]
                    (d/q @conn [:db/ident :contact])
                    gs/htmlEscape)]
      (html
       [:img.header {:src "/assets/contact-us.gif" :alt "Contact Us"}]
       [:p#error]
       [:h2#success]
       [:form {:action (str lambda-base "edit-page") :method "post"}
        [:dl
         [:dt [:label {:for "email"} "Contact Email"]]
         [:dd [:input {:type "text" :name "email" :value email}]]
         [:dt "&nbsp;"]
         [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
       admin-footer))))

(defn admin-basic
  [ident header-img text-label]
  (fn [pages]
    (go
      (let [{:keys [lambda-base db]} pages
            conn (<! (:conn-ch db))
            text (gs/htmlEscape (d/q text-query @conn [:db/ident ident]))]
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

;; Devotions

(defn admin-devotion-li
  [devotion]
  (let [title (gs/htmlEscape (:devotion/title devotion))]
    [:li [:a {:href (str "/admin/devotions/edit?slug="
                         (:devotion/slug devotion))}
          title]]))

(defn admin-devotions
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          devotions (->> (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e :devotion/featured?]] @conn)
                         (sort-by :devotion/created-at #(compare %2 %1)))]
      (html
       [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
       [:h2#success]
       [:ul (map admin-devotion-li devotions)]
       [:p [:a {:href "/admin/devotions/add"} "Add a new Devotion"]]
       admin-footer))))

(defn admin-devotions-template
  ([pages]
   (admin-devotions-template pages {}))
  ([pages devotion]
   (let [{:keys [lambda-base]} pages
         title (gs/htmlEscape (:devotion/title devotion))
         author (gs/htmlEscape (:devotion/author devotion))
         body (gs/htmlEscape (:devotion/body devotion))]
     (html
      [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
      [:p#error]
      [:h2#success]
      [:form {:action (str lambda-base "edit-page") :method "post"}
       [:dl
        [:dt [:label {:for "title"} "Title"]]
        [:dd [:input {:type "text" :name "title" :value title}]]
        [:dt [:label {:for "author"} "Author"]]
        [:dd [:input {:type "text" :name "author" :value author}]]
        [:dt [:label {:for "devotion"} "Devotion"]]
        [:dd [:textarea {:name "devotion" :rows "24" :cols "80"} body]]
        [:dt "&nbsp;"]
        [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
      (when-not (empty? devotion)
        [:p
         [:a {:href (str "/admin/devotions/delete?slug="
                         (:devotion/slug devotion))}
          "Delete this devotion"]])
      admin-footer))))

(defn admin-devotions-add
  [pages]
  (admin-devotions-template pages))

(defn devotion-by-slug
  [db slug]
  (try
    (d/q '[:find (pull ?dev-id [*]) .
           :in $ ?dev-id]
         db
         [:devotion/slug slug])
    (catch js/Error e
      nil)))

(defn admin-devotions-edit
  [pages query]
  (go
    (let [{:keys [db]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          devotion (devotion-by-slug @conn slug)]
      (if (and slug devotion)
        (admin-devotions-template pages devotion)
        admin-error))))

(defn admin-devotions-delete
  [pages query]
  (go
    (let [{:keys [db lambda-base]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          devotion (devotion-by-slug @conn slug)]
      (if (and slug devotion)
        (let [title (gs/htmlEscape (:devotion/title devotion))]
          (html
           [:img.header {:src "/assets/devotions.gif" :alt "Devotions"}]
           [:p#error]
           [:h2#success]
           [:h2 (str "Do you want to delete \"" title "\"?")]
           [:dl
            [:dd.delForm [:form {:action (str lambda-base "delete-page")
                                 :method "post"}
                          [:input {:type "submit" :name "yes" :value "Yes"}]]]
            [:dd.delForm [:form {:action "/admin/devotions"
                                 :method "get"}
                          [:input {:type "submit" :name "no" :value "No"}]]]]
           admin-footer))
        admin-error))))

;; Testimonies

(defn admin-testimony-li
  [testimony]
  (let [title (gs/htmlEscape (:testimony/title testimony))]
    [:li [:a {:href (str "/admin/testimonies/edit?slug="
                         (:testimony/slug testimony))}
          title]]))

(defn admin-testimonies
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          testimonies (->> (d/q '[:find [(pull ?e [*]) ...]
                                  :where [?e :testimony/title]] @conn)
                           (sort-by :testimony/title))]
      (html
       [:img.header {:src "/assets/testimonies.gif" :alt "Testimonies"}]
       [:h2#success]
       [:ul (map admin-testimony-li testimonies)]
       [:p [:a {:href "/admin/testimonies/add"} "Add a new Testimony"]]
       admin-footer))))

(defn admin-testimonies-template
  ([pages]
   (admin-testimonies-template pages {}))
  ([pages testimony]
   (let [{:keys [lambda-base]} pages
         title (gs/htmlEscape (:testimony/title testimony))
         body (gs/htmlEscape (:testimony/body testimony))]
     (html
      [:img.header {:src "/assets/testimonies.gif" :alt "Testimonies"}]
      [:p#error]
      [:h2#success]
      [:form {:action (str lambda-base "edit-page") :method "post"}
       [:dl
        [:dt [:label {:for "title"} "Title"]]
        [:dd [:input {:type "text" :name "title" :value title}]]
        [:dt [:label {:for "body"} "Testimony"]]
        [:dd [:textarea {:name "body" :rows "24" :cols "80"} body]]
        [:dt "&nbsp;"]
        [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
      (when-not (empty? testimony)
        [:p
         [:a {:href (str "/admin/testimonies/delete?slug="
                         (:testimony/slug testimony))}
          "Delete this testimony"]])
      admin-footer))))

(defn admin-testimonies-add
  [pages]
  (admin-testimonies-template pages))

(defn testimony-by-slug
  [db slug]
  (try
    (d/q '[:find (pull ?t-id [*]) .
           :in $ ?t-id]
         db
         [:testimony/slug slug])
    (catch js/Error e
      nil)))

(defn admin-testimonies-edit
  [pages query]
  (go
    (let [{:keys [db]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          testimony (testimony-by-slug @conn slug)]
      (if (and slug testimony)
        (admin-testimonies-template pages testimony)
        admin-error))))

(defn admin-testimonies-delete
  [pages query]
  (go
    (let [{:keys [db lambda-base]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          testimony (testimony-by-slug @conn slug)]
      (if (and slug testimony)
        (let [title (gs/htmlEscape (:testimony/title testimony))]
          (html
           [:img.header {:src "/assets/testimonies.gif" :alt "Testimonies"}]
           [:p#error]
           [:h2#success]
           [:h2 (str "Do you want to delete \"" title "\"?")]
           [:dl
            [:dd.delForm [:form {:action (str lambda-base "delete-page")
                                 :method "post"}
                          [:input {:type "submit" :name "yes" :value "Yes"}]]]
            [:dd.delForm [:form {:action "/admin/testimonies"
                                 :method "get"}
                          [:input {:type "submit" :name "no" :value "No"}]]]]
           admin-footer))
        admin-error))))

;; Scripture categories

(defn admin-scripture-category-li
  [category]
  (let [name (gs/htmlEscape (:scripture-category/name category))]
    [:li [:a {:href (str "/admin/scripture/categories/edit?slug="
                         (:scripture-category/slug category))}
          name]]))

(defn admin-scripture-categories
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          categories (->> (d/q '[:find [(pull ?e [*]) ...]
                                 :where [?e :scripture-category/name]] @conn)
                          (sort-by :scripture-category/name))]
      (html
       [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
       [:h2#success]
       [:ul (map admin-scripture-category-li categories)]
       [:p [:a {:href "/admin/scripture/categories/add"}
            "Add a new Scripture Category"]]
       admin-footer))))

(defn admin-scripture-li
  [scripture]
  (let [ref (gs/htmlEscape (:scripture/reference scripture))
        cat-slug (:scripture-category/slug (:scripture/category scripture))]
    [:li [:a {:href (str "/admin/scripture/edit?category=" cat-slug "&slug="
                         (:scripture/slug scripture))}
          ref]]))

(defn admin-scripture-categories-template
  ([pages]
   (admin-scripture-categories-template pages {}))
  ([pages category]
   (let [{:keys [lambda-base]} pages
         name (gs/htmlEscape (:scripture-category/name category))]
     (html
      [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
      [:p#error]
      [:h2#success]
      [:form {:action (str lambda-base "edit-page") :method "post"}
       [:dl
        [:dt [:label {:for "name"} "Category Name"]]
        [:dd [:input {:type "text" :name "name" :value name}]]
        [:dt "&nbsp;"]
        [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
      (when-not (empty? category)
        [:div
         [:p
          [:a {:href (str "/admin/scripture/categories/delete?slug="
                          (:scripture-category/slug category))}
           "Delete this Category"]]
         (when-let [scriptures (:scripture/_category category)]
           [:h3 "Edit the following scriptures"]
           [:ul (map admin-scripture-li scriptures)])
         [:p [:a {:href (str "/admin/scripture/add?category="
                             (:scripture-category/slug category))}
              "Add a new Scripture"]]])
      admin-footer))))

(defn admin-scripture-categories-add
  [pages]
  (admin-scripture-categories-template pages))

(defn scripture-category-by-slug
  [db slug]
  (try
    (d/q '[:find (pull ?cat-id [* {:scripture/_category
                                   [* {:scripture/category
                                       [:scripture-category/slug]}]}]) .
           :in $ ?cat-id]
         db
         [:scripture-category/slug slug])
    (catch js/Error e
      nil)))

(defn admin-scripture-categories-edit
  [pages query]
  (go
    (let [{:keys [db]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          category (scripture-category-by-slug @conn slug)]
      (if (and slug category)
        (admin-scripture-categories-template pages category)
        admin-error))))

(defn admin-scripture-categories-delete
  [pages query]
  (go
    (let [{:keys [db lambda-base]} pages
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          category (scripture-category-by-slug @conn slug)]
      (if (and slug category)
        (let [name (gs/htmlEscape (:scripture-category/name category))]
          (html
           [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
           [:p#error]
           [:h2#success]
           [:h2 (str "Do you want to delete \"" name "\"?")]
           [:dl
            [:dd.delForm [:form {:action (str lambda-base "delete-page")
                                 :method "post"}
                          [:input {:type "submit" :name "yes" :value "Yes"}]]]
            [:dd.delForm [:form {:action "/admin/scripture"
                                 :method "get"}
                          [:input {:type "submit" :name "no" :value "No"}]]]]
           admin-footer))
        admin-error))))

(defn admin-scripture-template
  ([pages]
   (admin-scripture-template pages {}))
  ([pages scripture]
   (let [{:keys [lambda-base]} pages
         reference (gs/htmlEscape (:scripture/reference scripture))
         text (gs/htmlEscape (:scripture/text scripture))]
     (html
      [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
      [:p#error]
      [:h2#success]
      [:form {:action (str lambda-base "edit-page") :method "post"}
       [:dl
        [:dt [:label {:for "reference"} "Reference"]]
        [:dd [:input {:type "text" :name "reference" :value reference}]]
        [:dt [:label {:for "text"} "Scripture"]]
        [:dd [:textarea {:name "text" :rows "24" :cols "80"} text]]
        [:dt "&nbsp;"]
        [:dd [:input {:type "submit" :name "submit" :value "Submit"}]]]]
      (when-not (empty? scripture)
        [:p
         [:a {:href (str "/admin/scripture/delete?category="
                         (:scripture-category/slug (:scripture/category scripture))
                         "&slug="
                         (:scripture/slug scripture))}
          "Delete this scripture"]])
      admin-footer))))

(defn admin-scripture-add
  [pages]
  (admin-scripture-template pages))

(defn scripture-by-slug-and-category
  [db slug category-slug]
  (try
    (d/q '[:find (pull ?s [* {:scripture/category
                              [:scripture-category/slug]}]) .
           :in $ ?s ?cat-s
           :where [?s :scripture/category ?cat-s]]
         db
         [:scripture/slug slug]
         [:scripture-category/slug category-slug])
    (catch js/Error e
      nil)))

(defn admin-scripture-edit
  [pages query]
  (go
    (let [{:keys [db]} pages
          slug (get-query-param query "slug")
          cat-slug (get-query-param query "category")
          conn (<! (:conn-ch db))
          scripture (scripture-by-slug-and-category @conn slug cat-slug)]
      (if (and slug cat-slug scripture)
        (admin-scripture-template pages scripture)
        admin-error))))

(defn admin-scripture-delete
  [pages query]
  (go
    (let [{:keys [db lambda-base]} pages
          slug (get-query-param query "slug")
          cat-slug (get-query-param query "category")
          conn (<! (:conn-ch db))
          scripture (scripture-by-slug-and-category @conn slug cat-slug)]
      (if (and slug cat-slug scripture)
        (let [reference (gs/htmlEscape (:scripture/reference scripture))]
          (html
           [:img.header {:src "/assets/scripture.gif" :alt "Scripture"}]
           [:p#error]
           [:h2#success]
           [:h2 (str "Do you want to delete \"" reference "\"?")]
           [:dl
            [:dd.delForm [:form {:action (str lambda-base "delete-page")
                                 :method "post"}
                          [:input {:type "submit" :name "yes" :value "Yes"}]]]
            [:dd.delForm [:form {:action "/admin/scripture/categories/edit"
                                 :method "get"}
                          [:input {:type "hidden" :name "slug" :value cat-slug}]
                          [:input {:type "submit" :name "no" :value "No"}]]]]
           admin-footer))
        admin-error))))

;; Editing functions

(defn page-info->put-ch
  [pages]
  (fn [{:keys [path content]}]
    (s3/put-obj!-ch (:s3-conn pages)
                    (:html-bucket pages) path content
                    {:ContentLength (count content)
                     :ContentType "text/html"})))

(defn generate-page
  [pages]
  (fn [gen-fn]
    (go
      (let [{:keys [path content]} (<! (gen-fn pages))]
        (<! ((page-info->put-ch pages)
             {:path path :content content}))))))

(defn generate-pages
  [pages gen-fns success-msg]
  (go
    (let [put-ch (async/merge (map (generate-page pages) gen-fns))
          error (loop []
                  (let [[err :as val] (<! put-ch)]
                    (when err
                      (js/Error. err))
                    (when-not (nil? val)
                      (recur))))]
      (if error
        error
        success-msg))))

(defn delete-page
  [pages path]
  (let [{:keys [s3-conn html-bucket]} pages]
    (s3/delete-obj!-ch s3-conn html-bucket path)))

(defn edit-contact
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [email]} form]
      (if (empty? email)
        (js/Error. "Please fill out all the fields")
        (let [[err] (<! (db/transact!-ch
                         db
                         [{:db/id -1
                           :db/ident :contact
                           :contact/email email}]))]
          (if err
            (js/Error. err)
            (.stringify
             js/JSON (clj->js
                      {:success
                       "The contact email was successfully updated"}))))))))

(defn edit-basic
  [ident gen-fn]
  (fn [pages form]
    (go
      (let [{:keys [db]} pages
            {:keys [text]} form]
        (if (empty? text)
          (js/Error. "Please fill out all the fields")
          (let [[err] (<! (db/transact!-ch
                           db
                           [{:db/id -1
                             :db/ident ident
                             :page/text text}]))]
            (if err
              (js/Error. err)
              (<! (generate-pages
                   pages
                   [gen-fn]
                   (.stringify
                    js/JSON
                    (clj->js
                     {:success "The text was successfully edited"})))))))))))

(def edit-home (edit-basic :home home))

(def edit-about-us (edit-basic :about-us about-us))

(def edit-resources (edit-basic :resources resources))

(defn add-devotion
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [title author devotion]} form]
      (if (some empty? [title author devotion])
        (js/Error. "Please fill out all the fields")
        (let [conn (<! (:conn-ch db))
              featured-devotions (d/q '[:find [?e ...]
                                        :where [?e :devotion/featured? true]]
                                      @conn)
              tx (mapv (fn [d-id] {:db/id d-id :devotion/featured? false})
                       featured-devotions)
              slug (str/slugify title)
              [err] (<! (db/transact!-ch
                         db
                         (into tx [{:db/id -1
                                    :devotion/author author
                                    :devotion/title title
                                    :devotion/slug slug
                                    :devotion/body devotion
                                    :devotion/created-at (js/Date.)
                                    :devotion/featured? true}])))]
          (if err
            (js/Error. err)
            (<! (generate-pages
                 pages
                 [featured-devotion
                  archived-devotions]
                 (.stringify
                  js/JSON (clj->js
                           {:success "The devotion was successfully added"
                            :redirect "/admin/devotions"}))))))))))

(defn edit-devotion
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query title author devotion]} form]
      (if (some empty? [title author devotion])
        (js/Error. "Please fill out all the fields")
        (let [old-slug (get-query-param query "slug")
              slug (str/slugify title)
              [err] (if old-slug
                      (<! (db/transact!-ch
                           db
                           [{:db/id [:devotion/slug old-slug]
                             :devotion/author author
                             :devotion/title title
                             :devotion/slug slug
                             :devotion/body devotion}]))
                      ["Invalid slug"])]
          (if err
            (js/Error. err)
            (<! (generate-pages
                 pages
                 [featured-devotion
                  archived-devotions]
                 (.stringify
                  js/JSON (clj->js
                           {:success "The devotion was successfully edited"
                            :redirect "/admin/devotions"}))))))))))

(defn delete-devotion
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [path query]} form
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          featured? (d/q '[:find ?featured .
                           :in $ ?e
                           :where
                           [?e :devotion/featured? ?featured]]
                         @conn
                         [:devotion/slug slug])
          tx (or
              (when featured?
                (let [d (->>
                         (d/q
                          '[:find [(pull ?e [:db/id :devotion/created-at]) ...]
                            :where
                            [?e :devotion/featured? false]]
                          @conn)
                         (sort-by :devotion/created-at #(compare %2 %1))
                         first)]
                  (when d
                    [{:db/id (:db/id d)
                      :devotion/featured? true}])))
              [])
          [err] (<! (db/transact!-ch
                     db
                     (into tx [[:db.fn/retractEntity
                                [:devotion/slug slug]]])))]
      (if err
        (js/Error. err)
        (<! (generate-pages
             pages
             [featured-devotion
              archived-devotions]
             (.stringify
              js/JSON (clj->js
                       {:success "The devotion was successfully deleted"
                        :redirect "/admin/devotions"}))))))))

(defn add-testimony
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [title body]} form]
      (if (some empty? [title body])
        (js/Error. "Please fill out all the fields")
        (let [conn (<! (:conn-ch db))
              slug (str/slugify title)
              entity {:testimony/title title
                      :testimony/slug slug
                      :testimony/body body}
              [err] (<! (db/transact!-ch
                         db
                         [(assoc entity :db/id -1)]))]
          (if err
            (js/Error. err)
            (<! (generate-pages
                 pages
                 [testimonies
                  (testimony entity)]
                 (.stringify
                  js/JSON (clj->js
                           {:success "The testimony was successfully added"
                            :redirect "/admin/testimonies"}))))))))))

(defn edit-testimony
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query title body]} form]
      (if (some empty? [title body])
        (js/Error. "Please fill out all the fields")
        (let [old-slug (get-query-param query "slug")
              slug (str/slugify title)
              entity {:testimony/title title
                      :testimony/slug slug
                      :testimony/body body}
              [err] (if old-slug
                      (<! (db/transact!-ch
                           db
                           [(assoc entity :db/id [:testimony/slug old-slug])]))
                      ["Invalid slug"])]
          (if err
            (js/Error. err)
            (let [[del-err] (<! (delete-page pages
                                             (str "testimonies/" old-slug)))]
              (if del-err
                (js/Error. del-err)
                (<! (generate-pages
                     pages
                     [testimonies
                      (testimony entity)]
                     (.stringify
                      js/JSON (clj->js
                               {:success "The testimony was successfully edited"
                                :redirect "/admin/testimonies"}))))))))))))

(defn delete-testimony
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query]} form
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          [err] (<! (db/transact!-ch
                     db
                     [[:db.fn/retractEntity [:testimony/slug slug]]]))]
      (if err
        (js/Error. err)
        (let [[del-err] (<! (delete-page pages (str "testimonies/" slug)))]
          (if del-err
            (js/Error. del-err)
            (<! (generate-pages
                 pages
                 [testimonies]
                 (.stringify
                  js/JSON (clj->js
                           {:success "The testimony was successfully deleted"
                            :redirect "/admin/testimonies"}))))))))))

;; Scripture Categories

(defn add-scripture-category
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [name]} form]
      (if (empty? name)
        (js/Error. "Please fill out all the fields")
        (let [conn (<! (:conn-ch db))
              slug (str/slugify name)
              entity {:scripture-category/name name
                      :scripture-category/slug slug}
              [err] (<! (db/transact!-ch
                         db
                         [(assoc entity :db/id -1)]))]
          (if err
            (js/Error. err)
            (<! (generate-pages
                 pages
                 [scripture-categories
                  (scripture-category entity)]
                 (.stringify
                  js/JSON
                  (clj->js
                   {:success "The scripture category was successfully added"
                    :redirect "/admin/scripture/categories"}))))))))))

(defn edit-scripture-category
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query name]} form]
      (if (empty? name)
        (js/Error. "Please fill out all the fields")
        (let [old-slug (get-query-param query "slug")
              slug (str/slugify name)
              [err tx-info] (if old-slug
                              (<! (db/transact!-ch
                                   db
                                   [{:db/id [:scripture-category/slug old-slug]
                                     :scripture-category/name name
                                     :scripture-category/slug slug}]))
                              ["Invalid slug"])]
          (if err
            (js/Error. err)
            (let [[del-err] (<! (delete-page pages (str "scripture/"
                                                        old-slug)))]
              (if del-err
                (js/Error. del-err)
                (let [entity (d/q '[:find (pull
                                           ?e
                                           [* {:scripture/_category [*]}]) .
                                    :in $ ?e]
                                  (:db-after tx-info)
                                  [:scripture-category/slug slug])]
                  (<!
                   (generate-pages
                    pages
                    [scripture-categories
                     (scripture-category entity)]
                    (.stringify
                     js/JSON
                     (clj->js
                      {:success "The scripture category was successfully edited"
                       :redirect "/admin/scripture/categories"})))))))))))))

(defn delete-scripture-category
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query]} form
          slug (get-query-param query "slug")
          conn (<! (:conn-ch db))
          [err] (<! (db/transact!-ch
                     db
                     [[:db.fn/retractEntity
                       [:scripture-category/slug slug]]]))]
      (if err
        (js/Error. err)
        (let [[del-err] (<! (delete-page pages (str "scripture/" slug)))]
          (if del-err
            (js/Error. del-err)
            (<!
             (generate-pages
              pages
              [scripture-categories]
              (.stringify
               js/JSON
               (clj->js
                {:success "The scripture category was successfully deleted"
                 :redirect "/admin/scripture/categories"}))))))))))

;; Scripture

(defn add-scripture
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query reference text]} form]
      (if (some empty? [reference text])
        (js/Error. "Please fill out all the fields")
        (let [conn (<! (:conn-ch db))
              slug (str/slugify reference)
              cat-slug (get-query-param query "category")
              [err tx-info] (<! (db/transact!-ch
                                 db
                                 [{:db/id -1
                                   :scripture/reference reference
                                   :scripture/text text
                                   :scripture/slug slug
                                   :scripture/category
                                   [:scripture-category/slug
                                    cat-slug]}]))
              category (scripture-category-by-slug (:db-after tx-info)
                                                   cat-slug)]
          (if err
            (js/Error. err)
            (<! (generate-pages
                 pages
                 [scripture-categories
                  (scripture-category category)]
                 (.stringify
                  js/JSON
                  (clj->js
                   {:success "The scripture was successfully added"
                    :redirect (str "/admin/scripture/categories/edit?slug="
                                   cat-slug)}))))))))))

(defn edit-scripture
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query reference text]} form]
      (if (some empty? [reference text])
        (js/Error. "Please fill out all the fields")
        (let [old-slug (get-query-param query "slug")
              cat-slug (get-query-param query "category")
              slug (str/slugify reference)
              [err tx-info] (if old-slug
                              (<! (db/transact!-ch
                                   db
                                   [{:db/id [:scripture/slug old-slug]
                                     :scripture/text text
                                     :scripture/slug slug
                                     :scripture/category
                                     [:scripture-category/slug
                                      cat-slug]}]))
                              ["Invalid slug"])]
          (if err
            (js/Error. err)
            (let [entity (d/q '[:find (pull
                                       ?e
                                       [* {:scripture/_category [*]}]) .
                                :in $ ?e]
                              (:db-after tx-info)
                              [:scripture-category/slug cat-slug])]
              (<! (generate-pages
                   pages
                   [(scripture-category entity)]
                   (.stringify
                    js/JSON
                    (clj->js
                     {:success "The scripture was successfully edited"
                      :redirect (str "/admin/scripture/categories/edit"
                                     "?slug=" cat-slug)})))))))))))

(defn delete-scripture
  [pages form]
  (go
    (let [{:keys [db]} pages
          {:keys [query]} form
          slug (get-query-param query "slug")
          cat-slug (get-query-param query "category")
          conn (<! (:conn-ch db))
          scripture (scripture-by-slug-and-category @conn slug cat-slug)
          [err tx-info] (<! (db/transact!-ch
                             db
                             [[:db.fn/retractEntity (:db/id scripture)]]))]
      (if err
        (js/Error. err)
        (let [entity (d/q '[:find (pull
                                   ?e
                                   [* {:scripture/_category [*]}]) .
                            :in $ ?e]
                          (:db-after tx-info)
                          [:scripture-category/slug cat-slug])]
          (<! (generate-pages
               pages
               [(scripture-category entity)]
               (.stringify
                js/JSON
                (clj->js {:success "The scripture was successfully deleted"
                          :redirect (str "/admin/scripture/categories/edit"
                                         "?slug=" cat-slug)})))))))))
