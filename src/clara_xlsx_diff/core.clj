(ns clara-xlsx-diff.core
  "Main entry point for XLSX comparison functionality"
  (:require [clara-xlsx-diff.xlsx :as xlsx]
            [clara-xlsx-diff.eav :as eav]
            [clara-xlsx-diff.rules :as rules]
            [clara-xlsx-diff.output :as output]))

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
        data1 (xlsx/extract-data file1-path :sheets sheets)
        data2 (xlsx/extract-data file2-path :sheets sheets)
        
        ;; Transform to EAV triples
        eav1 (eav/xlsx->eav data1 :version :v1)
        eav2 (eav/xlsx->eav data2 :version :v2)
        
        ;; Analyze differences using Clara rules
        changes (rules/analyze-changes eav1 eav2)
        
        ;; Generate summary
        summary (rules/summarize-changes changes)]
    
    (cond-> {:changes changes
             :summary summary}
      (= output-format :html) (assoc :html (output/generate-html changes data1 data2)))))

(defn -main
  "Command line entry point"
  [& args]
  (when (< (count args) 2)
    (println "Usage: clara-xlsx-diff <file1.xlsx> <file2.xlsx> [options]")
    (System/exit 1))
  
  (let [[file1 file2 & opts] args
        result (compare-xlsx file1 file2)]
    (println "Changes found:" (:summary result))
    (when (seq (:changes result))
      (doseq [change (:changes result)]
        (println (output/format-change change))))))

(comment
  ;; REPL experiments
  (def sample-result 
    (compare-xlsx "test/resources/sample1.xlsx" 
                  "test/resources/sample2.xlsx"))
  
  ;; Inspect results
  (:summary sample-result)
  (take 5 (:changes sample-result))
  
  ;; Generate HTML output
  (compare-xlsx "test/resources/sample1.xlsx" 
                "test/resources/sample2.xlsx"
                :output-format :html)
  )
