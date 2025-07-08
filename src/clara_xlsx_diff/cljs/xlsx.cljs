(ns clara-xlsx-diff.cljs.xlsx
  "ClojureScript XLSX file parsing using SheetJS"
  (:require ["xlsx" :as XLSX]
            ["fs" :as fs]))

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
  (let [range (aget worksheet "!ref")]
    (if-not range
      {:sheet-name sheet-name :cells []}
      ;; Extract all cell addresses (excluding metadata keys that start with !)
      (let [cell-addresses (js->clj (js/Object.keys worksheet))
            data-addresses (filter #(not (.startsWith % "!")) cell-addresses)
            cells (for [cell-addr data-addresses
                        :let [cell (aget worksheet cell-addr)]
                        :when cell]
                    (let [;; Extract row and column from cell address (e.g., "A1" -> row 0, col 0)
                          row-col (js->clj (.utils.decode_cell XLSX cell-addr))
                          row (get row-col "r")
                          col (get row-col "c")]
                      {:cell-ref cell-addr
                       :row row
                       :col col
                       :value (cond
                                (aget cell "v") (aget cell "v")
                                (aget cell "w") (aget cell "w")
                                :else nil)
                       :type (case (aget cell "t")
                               "s" "STRING" "n" "NUMERIC" "b" "BOOLEAN"
                               "f" "FORMULA" "z" "BLANK" "STRING")
                       ;; Enhanced formatting information
                       :formatted-text (aget cell "w")           ; Formatted display text
                       :formula (aget cell "f")                  ; Formula (if any)
                       :style-index (aget cell "s")              ; Style index
                       :number-format (aget cell "z")            ; Number format
                       :hyperlink (aget cell "l")                ; Hyperlink info
                       :comment (aget cell "c")                  ; Cell comment
                       :html (aget cell "h")                     ; HTML representation
                       }))]
        {:sheet-name sheet-name
         :cells cells}))))

(defn extract-data
  "Extract structured data from XLSX file.
  
  Takes either:
  - Buffer from Node.js file reading
  - ArrayBuffer from file upload
  - Base64 string
  
  Options:
  - :sheets - vector of sheet names to extract (default: all sheets)
  
  Returns a map with sheet data and metadata"
  [file-buffer & {:keys [sheets]}]
  (let [read-options (cond-> #js {:type "buffer"}
                       include-styles (doto
                                       (aset "cellStyles" true)
                                        (aset "cellHTML" true)
                                        (aset "cellDates" true)
                                        (aset "cellNF" true)))
        workbook (XLSX/read file-buffer read-options)
        sheet-names (js->clj (aget workbook "SheetNames"))
        selected-sheets (if sheets (filter (set sheets) sheet-names) sheet-names)
        sheet-data (mapv (fn [sheet-name]
                           (let [worksheet (aget (aget workbook "Sheets") sheet-name)]
                             (extract-sheet-data worksheet sheet-name)))
                         selected-sheets)]
    {:file-path "buffer-data"
     :sheets sheet-data
     :sheet-count (count sheet-data)
     :total-cells (reduce + (map #(count (:cells %)) sheet-data))}))

(defn extract-data-from-file
  "Extract data from XLSX file path (Node.js only).
  
  For ClojureScript in Node.js context, reads file from filesystem.
  
  Options:
  - :sheets - vector of sheet names to extract (default: all sheets)"
  [file-path & {:keys [sheets]}]
  (let [fs ^js (js/require "fs")
        file-buffer (.readFileSync fs file-path)]
    (extract-data file-buffer :sheets sheets)))

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

;; create a file buffer assuming we have fs available
;; This is for Node.js usage, in browser you would use a File object directly
(defn create-file-buffer
  "Create a file buffer from a file path (Node.js only).
  Returns a Uint8Array."
  [file-path]
  (let [fs ^js (js/require "fs")
        file-buffer (.readFileSync fs file-path)]
    (js/Uint8Array. file-buffer)))

(comment
  ;; NOTE: Usage patterns differ between Browser and Node.js:
  ;;
  ;; BROWSER USAGE (with File input):
  ;; - User selects file via <input type="file">
  ;; - Get File object from input.files[0]
  ;; - Use extract-data-from-file with the File object
  ;;
  ;; NODE.JS USAGE (with file paths):
  ;; - Use extract-data with a file path string
  ;; - Node.js will read the file using fs module
  ;;
  ;; Example for Node.js:
  (def file-buffer (create-file-buffer "test/sample_data.xlsx"))
  (def extracted-data (extract-data file-buffer))
  ;;
  ;; Example for Browser:
  ;; (extract-data-from-file file-object-from-input)


  ;; For use with base64 data:
  ;; (let [data (extract-data base64-string)]
  ;;   (js/console.log "Sheet count:" (:sheet-count data)))
  )
