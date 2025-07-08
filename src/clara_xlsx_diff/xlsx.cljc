(ns clara-xlsx-diff.xlsx
  "XLSX file parsing and data extraction utilities - Cross-platform version"
  #?(:cljs (:require [clara-xlsx-diff.cljs.xlsx :as xlsx-cljs])
     :clj  (:require [clara-xlsx-diff.xlsx-jvm :as xlsx-jvm])))

#?(:cljs
   (defn extract-data
     "Extract data from XLSX file - ClojureScript version using SheetJS"
     [file-path] 
      (xlsx-cljs/extract-data (xlsx-cljs/create-file-buffer file-path)))

   :clj
   (defn extract-data
     "Extract data from XLSX file - Clojure version using Apache POI"
     [file-path]
     (->
      (xlsx-jvm/create-file-buffer file-path) 
      (xlsx-jvm/extract-data)))
   )



(comment
  
;;   (xlsx-jvm/extract-data (xlsx-jvm/create-file-buffer "test/sample_data.xlsx"))
  
   
 (extract-data "test/sample_data.xlsx")

  :rcf)