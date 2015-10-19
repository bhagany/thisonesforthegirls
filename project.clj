(defproject thisonesforthegirls "0.1.0-SNAPSHOT"
  :description "FIXME"
  :url "http://please.FIXME"
  :dependencies [[com.cognitect/transit-cljs "0.8.225"]
                 [com.stuartsierra/component "0.3.0"]
                 [datascript "0.13.0"]
                 [datascript-transit "0.2.0" :exclusions [com.cognitect/transit-clj
                                                          com.cognitect/transit-cljs]]
                 [hiccups "0.3.0"]
                 [io.nervous/cljs-lambda "0.1.2"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.0"]
            [lein-npm "0.6.1"]
            [io.nervous/lein-cljs-lambda "0.2.4"]]

  :npm {:dependencies [[aws-sdk "2.1.50"]
                       [bcryptjs "2.2.1"]
                       [body-parser "1.14.1"]
                       [connect "3.4.0"]
                       [jsonwebtoken "5.0.5"]
                       [serve-static "1.10.0"]
                       [source-map-support "0.3.2"]
                       [ws "0.8.0"]]}

  :source-paths ["lambda-src" "lambda-dev" "browser-src" "browser-dev"]

  :clean-targets ^{:protect false} ["out" "target"]

  :cljs-lambda {:defaults {:role "arn:aws:iam::801085451725:role/thisonesforthegirls-lambda"}
                :functions [{:name   "set-admin-creds"
                             :invoke thisonesforthegirls.system/set-admin-creds}
                            {:name   "set-token-secret"
                             :invoke thisonesforthegirls.system/set-token-secret}
                            {:name   "generate-all-pages"
                             :invoke thisonesforthegirls.system/generate-all-pages}
                            {:name   "login"
                             :invoke thisonesforthegirls.system/login}
                            {:name   "admin-page"
                             :invoke thisonesforthegirls.system/admin-page}]}

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
                       {:id "browser"
                        :source-paths ["browser-src"]
                        :compiler {:main thisonesforthegirls.admin
                                   :output-to "out/browser/admin.js"
                                   :output-dir "out/browser/admin"
                                   :asset-path "assets/js/admin"
                                   :closure-defines {:thisonesforthegirls.admin.admin_page_url "/lambda-fns/admin-page"}
                                   :optimizations :advanced}}
                       {:id "browser-dev"
                        :source-paths ["browser-src"]
                        :figwheel true
                        :compiler {:main thisonesforthegirls.admin
                                   :output-to "out/browser-dev/admin.js"
                                   :output-dir "out/browser-dev/admin"
                                   :asset-path "assets/js/admin"
                                   :closure-defines {:thisonesforthegirls.admin.admin_page_url "/lambda-fns/admin-page"}
                                   :optimizations :none}}]}
  :figwheel {})
