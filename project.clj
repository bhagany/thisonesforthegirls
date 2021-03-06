(defproject thisonesforthegirls "0.1.0-SNAPSHOT"
  :description "FIXME"
  :url "http://please.FIXME"
  :dependencies [[com.cognitect/transit-cljs "0.8.225"]
                 [com.stuartsierra/component "0.3.0"]
                 [datascript "0.13.0"]
                 [datascript-transit "0.2.0" :exclusions [com.cognitect/transit-clj
                                                          com.cognitect/transit-cljs]]
                 [funcool/cuerdas "0.6.0"]
                 [hiccups "0.3.0"]
                 [io.nervous/cljs-lambda "0.3.5"]
                 [markdown-clj "0.9.75"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.371"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-4"]
            [lein-npm "0.6.1"]
            [io.nervous/lein-cljs-lambda "0.6.5"]]

  :npm {:dependencies [[aws-sdk "2.1.50"]
                       [bcryptjs "2.2.1"]
                       [body-parser "1.14.1"]
                       [connect "3.4.0"]
                       [jsonwebtoken "5.0.5"]
                       [serve-static "1.10.0"]
                       [source-map-support "0.3.2"]
                       [ws "0.8.0"]]}

  :source-paths ["lambda-src" "lambda-dev" "admin-src" "contact-src"]

  :clean-targets ^{:protect false} ["out" "target"]

  :cljs-lambda {:cljs-build-id "lambda"
                :defaults {:role "arn:aws:iam::801085451725:role/thisonesforthegirls-lambda"}
                :functions [{:name   "set-admin-creds"
                             :invoke thisonesforthegirls.system/set-admin-creds}
                            {:name   "set-token-secret"
                             :invoke thisonesforthegirls.system/set-token-secret}
                            {:name   "generate-all-pages"
                             :invoke thisonesforthegirls.system/generate-all-pages}
                            {:name   "login"
                             :invoke thisonesforthegirls.system/login}
                            {:name   "admin-page"
                             :invoke thisonesforthegirls.system/admin-page}
                            {:name   "edit-page"
                             :invoke thisonesforthegirls.system/edit-page}
                            {:name   "delete-page"
                             :invoke thisonesforthegirls.system/delete-page}
                            {:name   "send-email"
                             :invoke thisonesforthegirls.system/send-email}]}

  :cljsbuild {:builds [{:id "lambda"
                        :source-paths ["lambda-src"]
                        :compiler {:output-to "out/lambda/thisonesforthegirls.js"
                                   :output-dir "out/lambda"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "lambda-dev"
                        :source-paths ["lambda-src" "lambda-dev"]
                        :figwheel true
                        :compiler {:main thisonesforthegirls.dev
                                   :output-to "out/lambda-dev/thisonesforthegirls.js"
                                   :output-dir "out/lambda-dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "admin"
                        :source-paths ["admin-src"]
                        :compiler {:main thisonesforthegirls.admin
                                   :output-to "out/browser/admin.js"
                                   :output-dir "out/admin/files"
                                   :asset-path "assets/js/admin"
                                   :closure-defines {:thisonesforthegirls.admin.admin_page_url
                                                     "https://ybs1weoc79.execute-api.us-east-1.amazonaws.com/prod/admin-page"}
                                   :optimizations :advanced}}
                       {:id "admin-dev"
                        :source-paths ["admin-src"]
                        :figwheel true
                        :compiler {:main thisonesforthegirls.admin
                                   :output-to "out/browser-dev/admin.js"
                                   :output-dir "out/browser-dev/admin"
                                   :asset-path "/assets/js/admin"
                                   :closure-defines {:thisonesforthegirls.admin.admin_page_url "/lambda-fns/admin-page"
                                                     :thisonesforthegirls.admin.secure_admin_cookie false}
                                   :optimizations :none}}
                       {:id "contact"
                        :source-paths ["contact-src"]
                        :compiler {:main thisonesforthegirls.contact
                                   :output-to "out/browser/contact.js"
                                   :output-dir "out/contact/files"
                                   :asset-path "assets/js/contact"
                                   :optimizations :advanced}}
                       {:id "contact-dev"
                        :source-paths ["contact-src"]
                        :figwheel true
                        :compiler {:main thisonesforthegirls.contact
                                   :output-to "out/browser-dev/contact.js"
                                   :output-dir "out/browser-dev/contact"
                                   :asset-path "/assets/js/contact"
                                   :optimizations :none}}]}
  :figwheel {})
