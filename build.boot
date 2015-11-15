#!/usr/bin/env boot

(set-env!
  :source-paths #{"cljs-src" "cljs-test"}
  :resource-paths #{"cljs-resources"}
  :target-path "lib"
  :dependencies '[; Boot setup
                  [adzerk/boot-cljs "1.7.170-1"]
                  [deraen/boot-less "0.4.2"]
                  [adzerk/boot-test "1.0.5"]

                  ; App dependencies
                  [org.clojure/clojurescript "1.7.170"]
                  [org.omcljs/om "1.0.0-alpha22-SNAPSHOT"]

                  ; Other dependencies
                  [devcards "0.2.0-8"]])

(task-options!
  pom {:project 'atom-clj-refactor
       :version "0.0.0"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-test :refer :all]
         '[deraen.boot-less :refer [less]])

(deftask test-once
  []
  (comp (test)))

(deftask test-auto
  []
  (comp (watch)
        (speak)
        (test-once)))

(deftask build-once
  []
  (comp (less)
        (cljs :source-map true
              :optimizations :simple
              :compiler-options {:language-in :ecmascript5
                                 :language-out :ecmascript5
                                 :target :nodejs
                                 :hashbang :false
                                 :pretty-print true})
        (test-once)))

(deftask build-auto
  []
  (comp (watch)
        (speak)
        (build-once)))
