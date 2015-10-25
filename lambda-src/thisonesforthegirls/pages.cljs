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
  (:import [goog.date Date])
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
  (let [slug (gs/htmlEscape (:scripture-category/slug category))]
    [:li [:a {:href (str "/scripture/" slug)}]]))

(defn scripture-markup
  [scripture]
  (let [reference (gs/htmlEscape (:scripture/reference scripture))
        text (gs/htmlEscape (:scripture/text scripture))]
    [[:dt
      [:label reference]]
     [:dd text]]))

(defn scripture-category
  [category]
  {:s3-key (str "/scripture/" (:scripture-category/slug category))
   :body (site-template
          (str "Scripture | " (:scripture-category/name category))
          [[:div#scripture
            [:img.header {:src "/assets/scripture.gif"
                          :alt "Scripture"}]
            [:dl (mapcat scripture-markup
                         (:scripture/_category category))]
            [:p [:a {:href "/scripture"} "Back to Categories"]]]])})

(defn testimony-list-item
  [testimony]
  (let [slug (gs/htmlEscape (:testimony/slug testimony))]
    [:li [:a {:href (str "/testimonies/" slug)}]]))

(defn testimony
  [testimony]
  {:s3-key (str "/testimonies/" (:testimony/slug testimony))
   :body (site-template
          (str "Testimonies | " (:testimony/title testimony))
          (let [title (gs/htmlEscape (:testimony/title testimony))
                text (:testimony/text testimony)]
            [[:div#testimonies
              [:img.header {:src "/assets/testimonies.gif"
                            :alt "Testimonies"}]
              [:p [:a {:href "/testimonies"}
                   "Back to Testimony Links"]]
              [:dl
               [:dt title]
               [:dd text]]
              [:p [:a {:href "/testimonies"}
                   "Back to Testimony Links"]]]]))})

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
          text (->> (d/q text-query @conn [:db/ident :home])
                    gs/htmlEscape
                    md->html)
          page (site-template
                "Welcome"
                [[:div#welcome [:img.header {:src "/assets/welcome.gif" :alt "Welcome"}]
                  "this text will not occur"
                  [:img#youare {:src "/assets/you-are.gif"
                                :alt "You are loved..."}]]])
          content (s/replace page "this text will not occur" text)]
      {:path "home" :content content})))

(defn about-us
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (->> (d/q text-query @conn [:db/ident :about-us])
                    gs/htmlEscape
                    md->html)
          page (site-template
                "About Us"
                [[:div#about
                  [:img.header {:src "/assets/about-us.gif" :alt "About Us"}]
                  "this text will not occur"]])
          content (s/replace page "this text will not occur" text)]
      {:path "about" :content content})))

(defn resources
  [pages]
  (go
    (let [{:keys [db]} pages
          conn (<! (:conn-ch db))
          text (->> (d/q text-query @conn [:db/ident :resources])
                    gs/htmlEscape
                    md->html)
          page (site-template
                "Community Resources"
                [[:div#resources
                  [:img.header {:src "/assets/community-resources.gif"
                                :alt "Community Resources"}]
                  "this text will not occur"]])
          content (s/replace page "this text will not occur" text)]
      {:path "community-resources" :content content})))

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
                    :content contact-us}
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
                   {:path "admin/scripture"
                    :content (admin-template "Scripture Admin")}
                   {:path "admin/testimonies"
                    :content (admin-template "Testimonies Admin")}]
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

(def admin-error (html error-fragment))

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
         [:h4 "Administration Links"]
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

(defn admin-devotion-li
  [devotion]
  (let [title (gs/htmlEscape (:devotion/title devotion))]
    [:li [:a {:href (str "/admin/devotions/edit/" (:devotion/slug devotion))}
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
       [:ul (map admin-devotion-li devotions)]
       [:p [:a {:href "/admin/devotions/add"} "Add a new Devotion"]]
       [:h4 "Administration Links"]
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
         [:a {:href (str "/admin/devotions/delete/"
                         (:devotion/slug devotion))}
          "Delete this devotion"]])
      [:h4 "Administration Links"]
      admin-footer))))

(defn admin-devotions-add
  [pages]
  (admin-devotions-template pages))

(defn admin-devotions-edit
  [pages path]
  (go
    (let [{:keys [db]} pages
          slug (s/replace path "/admin/devotions/edit/" "")
          conn (<! (:conn-ch db))
          devotion (d/q '[:find (pull ?dev-id [*]) .
                          :in $ ?dev-id]
                        @conn
                        [:devotion/slug slug])]
      (if (empty? devotion)
        admin-error
        (admin-devotions-template pages devotion)))))

(defn admin-devotions-delete
  [pages path]
  (go
    (let [{:keys [db lambda-base]} pages
          slug (s/replace path "/admin/devotions/delete/" "")
          conn (<! (:conn-ch db))
          devotion (d/q '[:find (pull ?dev-id [*]) .
                          :in $ ?dev-id]
                        @conn
                        [:devotion/slug slug])]
      (if (empty? devotion)
        admin-error
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
           [:h4 "Administration Links"]
           admin-footer))))))

(defn dynamic-admin-page
  [title path]
  (fn [_]
    (go
      {:path path
       :content (admin-template title)})))

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

(defn edit-basic
  [ident gen-fn]
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
          (<! (generate-pages
               pages
               [gen-fn]
               "The text was successfully edited")))))))

(def edit-home (edit-basic :home home))

(def edit-about-us (edit-basic :about-us about-us))

(def edit-resources (edit-basic :resources resources))

(defn add-devotion
  [pages event]
  (go
    (let [{:keys [db]} pages
          {:keys [title author devotion]} event
          conn (<! (:conn-ch db))
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
              archived-devotions
              (dynamic-admin-page
               (str "Devotions Admin | Edit \"" title "\"")
               (str "admin/devotions/edit/" slug))
              (dynamic-admin-page
               (str "Devotions Admin | Delete \"" title "\"")
               (str "admin/devotions/delete/" slug))]
             "The devotion was successfully added"))))))

(defn edit-devotion
  [pages event]
  (go
    (let [{:keys [db]} pages
          {:keys [path title author devotion]} event
          old-slug (s/replace path "/admin/devotions/edit/" "")
          slug (str/slugify title)
          [err] (<! (db/transact!-ch
                     db
                     [{:db/id [:devotion/slug old-slug]
                       :devotion/author author
                       :devotion/title title
                       :devotion/slug slug
                       :devotion/body devotion}]))]
      (if err
        (js/Error. err)
        (<! (generate-pages
             pages
             [featured-devotion
              archived-devotions
              (dynamic-admin-page
               (str "Devotions Admin | Edit \"" title "\"")
               (str "admin/devotions/edit/" slug))
              (dynamic-admin-page
               (str "Devotions Admin | Delete \"" title "\"")
               (str "admin/devotions/delete/" slug))]
             "The devotion was successfully edited"))))))
