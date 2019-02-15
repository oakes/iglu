(defproject iglu "0.1.0-SNAPSHOT"
  :description "A Clojure data -> GLSL library"
  :url "https://github.com/oakes/iglu"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :plugins [[lein-tools-deps "0.4.3"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :profiles {:dev {:main iglu.core}})
