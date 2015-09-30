(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.node)

(cljs.build.api/build (cljs.build.api/inputs "src-lambda" "dev-lambda")
  {:output-to "out-dev-lambda/thisonesforthegirls.js"
   :output-dir "out-dev-lambda"
   :target :nodejs
   :optimizations :none
   :source-map true
   :verbose true})

(cljs.repl/repl (cljs.repl.node/repl-env)
  :watch (cljs.build.api/inputs "src-lambda" "dev-lambda")
  :target :nodejs
  :output-to "out-dev-lambda/thisonesforthegirls.js"
  :output-dir "out-dev-lambda")
