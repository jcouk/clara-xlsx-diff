(ns clara-xlsx-diff.core
  "Main entry point for XLSX comparison functionality"
  (:require [clara-xlsx-diff.xlsx :as xlsx]
            [clara-xlsx-diff.eav :as custom-eav]
            #?(:clj [clara-xlsx-diff.output :as output])
            [clara-eav.rules :as er]
            [clara.rules :as rules]))

(defn compare-xlsx
  "Compare two XLSX files and return difference analysis.
  
  Options:
  - :output-format - :summary (default), :detailed, :html
  - :sheets - vector of sheet names to compare (default: all sheets)
  - :ignore-formatting - boolean, ignore cell formatting differences
  
  Returns a map with:
  - :changes - vector of individual change records
  - :summary - summary statistics
  - :html - HTML representation (if requested)"
  [file1-path file2-path & {:keys [output-format sheets ignore-formatting]
                            :or {output-format :summary
                                 ignore-formatting false}}]
  (let [;; Extract data from both files
        data1 (xlsx/extract-data file1-path)
        data2 (xlsx/extract-data file2-path)

        ;; Transform to EAV triples
        eav-v1 (custom-eav/xlsx->eav data1 :version :v1)
        eav-v2 (custom-eav/xlsx->eav data2 :version :v2)

        ;; Analyze differences using Clara rules
        diff-session (er/defsession diff-session 'clara-xlsx-diff.rules)
        session (-> diff-session
                    (er/upsert eav-v1)
                    (er/upsert eav-v2)
                    (rules/fire-rules))

        ;; Generate summary
        cells (->> (get-in session [:store :eav-index])
                   (filter (fn [[_entity-id data]]
                             (and (map? data)
                                  (some #(= "output" (namespace %)) (keys data)))))
                   (into []))
        ]

    ;; (cond-> {:changes cells}
    ;;   (= output-format :html) (assoc :html (output/generate-html cells data1 data2))) 
    
    ;; for now just return changes:
    {:changes cells
     }

    ))

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
     "JavaScript entry point"
     [file1 file2]
     (compare-xlsx file1 file2)))

#?(:cljs
   (defn ^:export init!
     "Initialize the library"
     []
     (println "Clara XLSX Diff library initialized")))

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
