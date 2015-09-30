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
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-npm "0.6.1"]
            [io.nervous/lein-cljs-lambda "0.2.4"]]

  :npm {:dependencies [[aws-sdk "2.1.50"]
                       [bcryptjs "2.2.1"]
                       [jsonwebtoken "5.0.5"]
                       [source-map-support "0.3.2"]]}

  :source-paths ["src-lambda"]

  :clean-targets ["out-lambda"]

  :cljs-lambda {:defaults {:role "arn:aws:iam::801085451725:role/thisonesforthegirls-lambda"}
                :functions [{:name   "set-admin-creds"
                             :invoke thisonesforthegirls.system/set-admin-creds}
                            {:name   "set-token-secret"
                             :invoke thisonesforthegirls.system/set-token-secret}
                            {:name   "login"
                             :invoke thisonesforthegirls.system/login}
                            {:name   "generate-all-pages"
                             :invoke thisonesforthegirls.system/generate-all-pages}]}

  :cljsbuild {:builds [{:id "thisonesforthegirls"
                        :source-paths ["src-lambda"]
                        :compiler {:output-to "out-lambda/thisonesforthegirls.js"
                                   :output-dir "out-lambda"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
