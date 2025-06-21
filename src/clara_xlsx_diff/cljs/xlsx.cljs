(ns clara-xlsx-diff.cljs.xlsx
  "ClojureScript XLSX file parsing using SheetJS"
  (:require ["xlsx" :as XLSX]))

(defn cell-reference
  "Convert row/col indices to Excel-style reference (e.g., 'A1', 'B5')"
  [row col]
  (let [col-letter (fn [n]
                     (if (< n 26)
                       (str (char (+ 65 n)))
                       (str (char (+ 65 (quot n 26) -1))
                            (char (+ 65 (mod n 26))))))]
    (str (col-letter col) (inc row))))

(defn extract-sheet-data
  "Extract all data from a single worksheet"
  [worksheet sheet-name]
  (let [range (.-range worksheet)
        start-row (.-s.r range)
        end-row (.-e.r range)
        start-col (.-s.c range)
        end-col (.-e.c range)]
    {:sheet-name sheet-name
     :cells (for [r (range start-row (inc end-row))
                  c (range start-col (inc end-col))
                  :let [cell-addr (str (cell-reference r c))
                        cell (aget worksheet cell-addr)]
                  :when cell]
              {:cell-ref cell-addr
               :row r
               :col c
               :value (.-v cell)
               :type (case (.-t cell)
                       "s" "STRING"
                       "n" "NUMERIC" 
                       "b" "BOOLEAN"
                       "f" "FORMULA"
                       "z" "BLANK"
                       "STRING")})}))

(defn extract-data
  "Extract structured data from XLSX file.
  
  Takes either:
  - ArrayBuffer from file upload
  - Base64 string
  - File object
  
  Options:
  - :sheets - vector of sheet names to extract (default: all sheets)
  
  Returns a map with sheet data and metadata"
  [file-data & {:keys [sheets]}]
  (let [workbook (XLSX/read file-data #js {:type "array"})
        sheet-names (.-SheetNames workbook)
        target-sheets (if sheets
                        (filter #(contains? (set sheets) %) sheet-names)
                        sheet-names)
        sheet-data (mapv (fn [sheet-name]
                          (let [worksheet (aget (.-Sheets workbook) sheet-name)]
                            (extract-sheet-data worksheet sheet-name)))
                        target-sheets)]
    {:file-path "uploaded-file"
     :sheets sheet-data
     :sheet-count (count sheet-data)
     :total-cells (reduce + (map #(count (:cells %)) sheet-data))}))

(defn load-file-as-array-buffer
  "Load a file and return a promise that resolves to ArrayBuffer"
  [file]
  (js/Promise.
    (fn [resolve reject]
      (let [reader (js/FileReader.)]
        (set! (.-onload reader)
              (fn [e]
                (resolve (js/Uint8Array. (.. e -target -result)))))
        (set! (.-onerror reader)
              (fn [_e]
                (reject (js/Error. "Failed to read file"))))
        (.readAsArrayBuffer reader file)))))

(comment
  ;; ClojureScript REPL experiments
  
  ;; For use in browser with file input:
  ;; (-> (load-file-as-array-buffer file)
  ;;     (.then extract-data)
  ;;     (.then #(js/console.log "Extracted data:" %)))
  
  ;; For use with base64 data:
  ;; (let [data (extract-data base64-string)]
  ;;   (js/console.log "Sheet count:" (:sheet-count data)))
  )
