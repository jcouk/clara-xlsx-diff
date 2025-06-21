(ns clara-xlsx-diff.xlsx
  "XLSX file parsing and data extraction utilities"
  (:require [clojure.java.io :as io])
  (:import [org.apache.poi.ss.usermodel WorkbookFactory Sheet Row Cell CellType]
           [org.apache.poi.ss.util CellReference]
           [java.io FileInputStream]))

(defn cell-value
  "Extract value from a POI Cell object"
  [^Cell cell]
  (when cell
    (let [cell-type (.getCellType cell)]
      (cond
        (= cell-type CellType/STRING) (.getStringCellValue cell)
        (= cell-type CellType/NUMERIC) (.getNumericCellValue cell)
        (= cell-type CellType/BOOLEAN) (.getBooleanCellValue cell)
        (= cell-type CellType/FORMULA) (.getCellFormula cell)
        (= cell-type CellType/BLANK) nil
        (= cell-type CellType/_NONE) nil
        :else nil))))

(defn- cell-reference
  "Get Excel-style cell reference (e.g., 'A1', 'B5')"
  [^Cell cell]
  (when cell
    (.formatAsString (CellReference. (.getRowIndex cell) (.getColumnIndex cell)))))

(defn- extract-sheet-data
  "Extract all data from a single sheet"
  [^Sheet sheet]
  (let [sheet-name (.getSheetName sheet)]
    {:sheet-name sheet-name
     :cells (for [^Row row sheet
                  ^Cell cell row
                  :let [value (cell-value cell)
                        ref (cell-reference cell)]
                  :when (some? value)]
              {:cell-ref ref
               :row (.getRowIndex cell)
               :col (.getColumnIndex cell)
               :value value
               :type (str (.getCellType cell))})}))

(defn extract-data
  "Extract structured data from XLSX file.
  
  Options:
  - :sheets - vector of sheet names to extract (default: all sheets)
  
  Returns a map with sheet data and metadata"
  [file-path & {:keys [sheets]}]
  (with-open [fis (FileInputStream. (io/file file-path))
              workbook (WorkbookFactory/create fis)]
    (let [all-sheets (for [i (range (.getNumberOfSheets workbook))]
                       (.getSheetAt workbook i))
          target-sheets (if sheets
                          (filter #(contains? (set sheets) (.getSheetName %)) all-sheets)
                          all-sheets)
          sheet-data (mapv extract-sheet-data target-sheets)]
      {:file-path file-path
       :sheets sheet-data
       :sheet-count (count sheet-data)
       :total-cells (reduce + (map #(count (:cells %)) sheet-data))})))

(comment
  ;; REPL experiments
  (extract-data "test/resources/sample.xlsx")
  
  ;; Extract specific sheets only
  (extract-data "test/resources/sample.xlsx" :sheets ["Sheet1" "Data"])
  
  ;; Inspect sheet structure
  (let [data (extract-data "test/resources/sample.xlsx")]
    (map :sheet-name (:sheets data)))
  )
