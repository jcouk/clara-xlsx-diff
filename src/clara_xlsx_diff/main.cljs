(ns clara-xlsx-diff.main
  "Main entry point for the compiled JavaScript library"
  (:require
   [clara-xlsx-diff.rules :as diff-rules]
   [clara-xlsx-diff.cljs.xlsx :as xlsx]
   [clara-xlsx-diff.eav :as local-eav]
   [clara.rules :as rules :include-macros true]
   [clara-eav.rules :as eav.rules :include-macros true]))


;; Create session using the rules from the main namespace
(eav.rules/defsession session 'clara-xlsx-diff.rules)


(defn analyze-changes
  "Analyze differences between two sets of EAV triples"
  [eav1 eav2]
  (let [session1 (eav.rules/upsert session (concat eav1 eav2))
        session2 (rules/fire-rules session1)
        output-records (->> (rules/query session2 diff-rules/get-all-output-records)
                           (map (fn [{:keys [?output]}]
                                  (map (fn [output]
                                         (dissoc output :eav/eid))
                                       ?output)))
                           first)
        summary-results (diff-rules/return-output-summary-record output-records)]
    {:cells output-records
     :summary summary-results}))

(defn ^:export compare-xlsx-buffers
  "Compare two XLSX files from buffers (ideal for git workflows).
  file1-buffer and file2-buffer should be Uint8Array or Buffer objects.
  file1-name and file2-name are optional labels for the comparison."
  ([file1-buffer file2-buffer]
   (compare-xlsx-buffers file1-buffer file2-buffer "file1" "file2"))
  ([file1-buffer file2-buffer file1-name file2-name]
   (try
     (let [file1-array (if (instance? js/Uint8Array file1-buffer)
                         file1-buffer
                         (js/Uint8Array. file1-buffer))
           file2-array (if (instance? js/Uint8Array file2-buffer)
                         file2-buffer
                         (js/Uint8Array. file2-buffer))
           
           data1 (xlsx/extract-data file1-array)
           data2 (xlsx/extract-data file2-array)
           
           eav1 (local-eav/xlsx->eav data1 :version :v1)
           eav2 (local-eav/xlsx->eav data2 :version :v2)
           
           changes (analyze-changes eav1 eav2)]

       #js {:success true
            :file1 file1-name
            :file2 file2-name
            :summary (clj->js (:summary changes))
            :cells (clj->js (:cells changes))})

     (catch js/Error e
       #js {:success false
            :error (.-message e)
            :file1 file1-name
            :file2 file2-name}))))


(defn ^:export init
  "Initialize the library"
  []
  #js {:version "1.0.0"
       :description "Cross-platform XLSX comparison using Clara Rules"})

;; For Node.js compatibility
(when (exists? js/module)
  (set! js/module.exports
        #js {:compareXlsxBuffers compare-xlsx-buffers
             :init init}))
