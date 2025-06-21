(ns clara-xlsx-diff.browser
  "Browser interface for XLSX diff functionality"
  (:require [clara-xlsx-diff.cljs.xlsx :as xlsx]
            [clara-xlsx-diff.cljs.eav :as eav]
            [clojure.set :as set]))

(defn log [& args]
  (apply js/console.log args))

(defn process-xlsx-file
  "Process an uploaded XLSX file and convert to EAV triples"
  [file version]
  (-> (xlsx/load-file-as-array-buffer file)
      (.then xlsx/extract-data)
      (.then #(eav/xlsx->eav % :version version))
      (.then (fn [eav-triples]
               (log "Processed file:" (.-name file))
               (log "Total EAV triples:" (count eav-triples))
               (log "Sample triples:" (take 5 eav-triples))
               eav-triples))
      (.catch (fn [error]
                (log "Error processing file:" error)
                (throw error)))))

(defn compare-xlsx-files
  "Compare two XLSX files and return differences"
  [file1 file2]
  (js/Promise.all
    #js [(process-xlsx-file file1 :v1)
         (process-xlsx-file file2 :v2)])
  (.then (fn [[eav1 eav2]]
           (log "Comparison complete:")
           (log "File 1 triples:" (count eav1))
           (log "File 2 triples:" (count eav2))
           
           ;; Simple difference analysis
           (let [entities1 (set (map :e eav1))
                 entities2 (set (map :e eav2))
                 only-in-1 (set/difference entities1 entities2)
                 only-in-2 (set/difference entities2 entities1)
                 common (set/intersection entities1 entities2)]
             
             (log "Only in file 1:" (count only-in-1))
             (log "Only in file 2:" (count only-in-2))
             (log "Common entities:" (count common))
             
             {:file1-eav eav1
              :file2-eav eav2
              :only-in-1 only-in-1
              :only-in-2 only-in-2
              :common common}))))

(defn setup-file-handlers
  "Set up file input handlers for the demo"
  []
  (when-let [file1-input (.getElementById js/document "file1")]
    (.addEventListener file1-input "change"
      (fn [e]
        (when-let [file (aget (.. e -target -files) 0)]
          (process-xlsx-file file :v1)))))
  
  (when-let [file2-input (.getElementById js/document "file2")]
    (.addEventListener file2-input "change"
      (fn [e]
        (when-let [file (aget (.. e -target -files) 0)]
          (process-xlsx-file file :v2)))))
  
  (when-let [compare-btn (.getElementById js/document "compare")]
    (.addEventListener compare-btn "click"
      (fn [_e]
        (let [file1 (aget (.getElementById js/document "file1") "files" 0)
              file2 (aget (.getElementById js/document "file2") "files" 0)]
          (if (and file1 file2)
            (compare-xlsx-files file1 file2)
            (log "Please select both files")))))))

(defn ^:export init!
  "Initialize the browser interface"
  []
  (log "Clara XLSX Diff - Browser interface initialized")
  (setup-file-handlers))

(comment
  ;; To use in browser console:
  ;; clara_xlsx_diff.browser.init_BANG_()
  )
