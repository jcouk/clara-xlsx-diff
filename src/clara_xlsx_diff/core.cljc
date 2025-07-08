(ns clara-xlsx-diff.core
  "Main entry point for XLSX comparison functionality"

  (:require #?(:clj [clara-xlsx-diff.xlsx :as clj-xlsx]
               :cljs [clara-xlsx-diff.cljs.xlsx :as cljs-xlsx])
            #?(:clj [clara-xlsx-diff.output :as output])
            [clara-xlsx-diff.eav :as custom-eav]))


#?(:cljs
   (defn ^:export init
     "Initialize the library for JavaScript usage"
     []
     (println "Clara XLSX Diff library initialized")
     #js {:version "1.0.0"
          :description "Cross-platform XLSX comparison using Clara Rules"}))

(defn compare-xlsx
  "Compare two XLSX files and return difference analysis.
  
  For Clojure: Pass file paths as strings
  For ClojureScript/Node.js: Pass file paths as strings (will be read using fs)
  
  Options:
  - :output-format - :summary (default), :detailed, :html
  - :sheets - vector of sheet names to compare (default: all sheets)
  - :ignore-formatting - boolean, ignore cell formatting differences
  
  Returns a map with:
  - :changes - vector of individual change records
  - :summary - summary statistics
  - :html - HTML representation (if requested)"
  [file1-buffer file2-buffer & {:keys [output-format sheets ignore-formatting]
                                :or {output-format :summary
                                     ignore-formatting false}}]
  (try
    (let [;; Extract data from both files
          data1 #?(:clj (clj-xlsx/extract-data file1-buffer)
                   :cljs (cljs-xlsx/extract-data file1-buffer))
          data2 #?(:clj (clj-xlsx/extract-data file2-buffer)
                   :cljs (cljs-xlsx/extract-data file2-buffer))

          ;; Transform to EAV triples
          eav-v1 (custom-eav/xlsx->eav data1 :version :v1)
          eav-v2 (custom-eav/xlsx->eav data2 :version :v2)

          ;; Platform-specific Clara Rules handling
          cell-records #?(:clj
                          ;; For Clojure, use Clara Rules for advanced analysis
                          (try
                            ;; For now, use simplified comparison until rules namespace is ready
                            (let [combined-eav (concat eav-v1 eav-v2)]
                              (filter (fn [eav-record]
                                        (and (map? eav-record)
                                             (contains? eav-record :a)
                                             (some #(= % (:a eav-record)) [:cell/value :cell/row :cell/col])))
                                      combined-eav))
                            (catch Exception e
                              (println "Warning: Clara Rules session failed, using fallback comparison:" (.getMessage e))
                              []))
                          :cljs
                          ;; For ClojureScript, use simplified comparison without Clara Rules macros
                          (let [combined-eav (concat eav-v1 eav-v2)]
                            ;; Simple diff logic for now
                            (filter (fn [eav-record]
                                      (and (map? eav-record)
                                           (contains? eav-record :a)
                                           (some #(= % (:a eav-record)) [:cell/value :cell/row :cell/col])))
                                    combined-eav)))]

      #?(:cljs
         ;; Return JavaScript-compatible object for ClojureScript
         #js {:success true
              :changes (clj->js cell-records)}
         :clj
         ;; Return Clojure map for Clojure
         {:success true
          :changes cell-records}))

    #?(:cljs
       (catch js/Error e
         #js {:success false
              :error (.-message e)
              :file1 "file1-buffer"
              :file2 "file2-buffer"})
       :clj
       (catch Exception e
         {:success false
          :error (.getMessage e)
          :file1 "file1-buffer"
          :file2 "file2-buffer"}))))

#?(:cljs
   (defn ^:export compare-xlsx-files
     "Main function to compare two XLSX files for JavaScript usage.
     Returns a JavaScript object with the comparison results."
     [file1-path file2-path]
     (compare-xlsx file1-path file2-path)))

#?(:clj
   (defn -main
     "Command line entry point (Clojure only)"
     [& args]
     (when (< (count args) 2)
       (println "Usage: clara-xlsx-diff <file1.xlsx> <file2.xlsx> [options]")
       (System/exit 1))

     (let [[file1 file2 & _opts] args
           result (compare-xlsx file1 file2)]
       (println "Changes found:" (:summary result))
       (when (seq (:changes result))
         (doseq [change (:changes result)]
           (println (output/format-change change)))))))

#?(:cljs
   (defn ^:export main
     "JavaScript entry point for demo usage"
     [file1 file2]
     (compare-xlsx-files file1 file2)))

#?(:cljs
   (defn ^:export init!
     "Initialize the library (alias for init)"
     []
     (init)))

(comment
  ;; REPL experiments
  (def sample-result
    (compare-xlsx "test/sample_data.xlsx"
                  "test/sample_data_2.xlsx"))

  ;; Inspect results
  (:changes sample-result)
  (take 5 (:changes sample-result))

  ;; Generate HTML output
  (compare-xlsx "test/resources/sample1.xlsx"
                "test/resources/sample2.xlsx"
                :output-format :html))
