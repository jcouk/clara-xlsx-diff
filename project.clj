(defproject clara-xlsx-diff "0.1.0-SNAPSHOT"
  :description "XLSX difference analysis using Clara Rules and Clara-EAV"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/clojurescript "1.11.60"]
                 [com.cerner/clara-rules "0.21.0"]
                 [clyfe/clara-eav "0.1.9" :exclusions [com.cerner/clara-rules]]
                 [org.apache.poi/poi "5.2.4"]
                 [org.apache.poi/poi-ooxml "5.2.4"]
                 [org.clojure/data.json "2.4.0"]
                 [hiccup/hiccup "1.0.5"]]
  
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.11"]]
  
  :source-paths ["src" "test"]
  
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/test.js"
                                   :main clara-xlsx-diff.rules-test
                                   :target :nodejs
                                   :optimizations :none}
                        :source-map true}
                       {:id "main"
                        :source-paths ["src"]
                        :compiler {:output-to "target/main.js"
                                   :main clara-xlsx-diff.main
                                   :target :nodejs
                                   :optimizations :none}
                        :source-map true}
                       {:id "simple"
                        :source-paths ["src"]
                        :compiler {:output-to "target/main-simple.js"
                                   :main clara-xlsx-diff.main
                                   :target :nodejs
                                   :optimizations :simple
                                   :pretty-print false
                                   :source-map false}
                        :source-map false}]}
  
  :doo {:build "test"
        :alias {:default [:node]}})
