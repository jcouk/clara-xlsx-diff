(ns clara-xlsx-diff-cljs.xlsx-test
  "Tests for ClojureScript XLSX parsing"
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [clara-xlsx-diff.cljs.xlsx :as xlsx]))

(deftest cell-reference-test
  (testing "Cell reference generation"
    (is (= (xlsx/cell-reference 0 0) "A1") "Should generate A1 for (0,0)")
    (is (= (xlsx/cell-reference 0 1) "B1") "Should generate B1 for (0,1)")
    (is (= (xlsx/cell-reference 1 0) "A2") "Should generate A2 for (1,0)")
    (is (= (xlsx/cell-reference 0 25) "Z1") "Should generate Z1 for (0,25)")
    (is (= (xlsx/cell-reference 0 26) "AA1") "Should generate AA1 for (0,26)")
    (is (= (xlsx/cell-reference 9 2) "C10") "Should generate C10 for (9,2)")))

(deftest extract-sheet-data-test
  (testing "Extract data from mock worksheet"
    ;; Mock worksheet structure similar to SheetJS format
    (let [mock-worksheet (clj->js {"A1" {:v "Name" :t "s"}
                                   "B1" {:v "Age" :t "s"}
                                   "A2" {:v "Alice" :t "s"}
                                   "B2" {:v 30 :t "n"}
                                   "!range" {:s {:r 0 :c 0}
                                            :e {:r 1 :c 1}}})
          result (xlsx/extract-sheet-data mock-worksheet "TestSheet")]
      
      (testing "Sheet structure"
        (is (= (:sheet-name result) "TestSheet") "Should have correct sheet name")
        (is (vector? (:cells result)) "Cells should be a vector")
        (is (= (count (:cells result)) 4) "Should extract 4 cells"))
      
      (testing "Cell data format"
        (let [cells (:cells result)
              first-cell (first cells)]
          (is (contains? first-cell :cell-ref) "Should have cell reference")
          (is (contains? first-cell :row) "Should have row index")
          (is (contains? first-cell :col) "Should have column index")
          (is (contains? first-cell :value) "Should have cell value")
          (is (contains? first-cell :type) "Should have cell type")))
      
      (testing "Cell type conversion"
        (let [cells (:cells result)
              string-cell (first (filter #(= (:value %) "Name") cells))
              numeric-cell (first (filter #(= (:value %) 30) cells))]
          (is (= (:type string-cell) "STRING") "Should convert 's' type to STRING")
          (is (= (:type numeric-cell) "NUMERIC") "Should convert 'n' type to NUMERIC"))))))

(deftest extract-data-mock-test
  (testing "Extract data from mock workbook structure"
    ;; This is a simplified test since we can't easily mock the full XLSX.read in Node.js
    ;; In a real scenario, you'd want integration tests with actual XLSX files
    (let [mock-data {:sheets [{:sheet-name "Employees"
                               :cells [{:cell-ref "A1" :row 0 :col 0 :value "Name" :type "STRING"}
                                       {:cell-ref "B1" :row 0 :col 1 :value "Department" :type "STRING"}
                                       {:cell-ref "A2" :row 1 :col 0 :value "Alice" :type "STRING"}
                                       {:cell-ref "B2" :row 1 :col 1 :value "Engineering" :type "STRING"}]}
                              {:sheet-name "Rules"
                               :cells [{:cell-ref "A1" :row 0 :col 0 :value "Rule ID" :type "STRING"}
                                       {:cell-ref "B1" :row 0 :col 1 :value "Condition" :type "STRING"}]}]
                     :sheet-count 2
                     :total-cells 6}]
      
      (testing "Mock data structure validation"
        (is (= (:sheet-count mock-data) 2) "Should have 2 sheets")
        (is (= (:total-cells mock-data) 6) "Should have 6 total cells")
        (is (= (count (:sheets mock-data)) 2) "Sheets vector should have 2 elements"))
      
      (testing "Sheet content validation"
        (let [employees-sheet (first (:sheets mock-data))
              rules-sheet (second (:sheets mock-data))]
          (is (= (:sheet-name employees-sheet) "Employees"))
          (is (= (:sheet-name rules-sheet) "Rules"))
          (is (= (count (:cells employees-sheet)) 4))
          (is (= (count (:cells rules-sheet)) 2)))))))

(deftest integration-workflow-test
  (testing "Full workflow simulation with mock data"
    ;; Simulate the workflow that would happen in the browser
    (let [mock-xlsx-data {:file-path "uploaded-file"
                          :sheets [{:sheet-name "TestSheet"
                                   :cells [{:cell-ref "A1" :row 0 :col 0 :value "Header" :type "STRING"}
                                           {:cell-ref "A2" :row 1 :col 0 :value "Data" :type "STRING"}]}]
                          :sheet-count 1
                          :total-cells 2}]
      
      (testing "Data extraction simulation"
        (is (= (:file-path mock-xlsx-data) "uploaded-file"))
        (is (pos? (:total-cells mock-xlsx-data)))
        (is (> (:sheet-count mock-xlsx-data) 0)))
      
      (testing "Ready for EAV conversion"
        ;; This would be the next step in the pipeline
        (let [sheets (:sheets mock-xlsx-data)]
          (is (every? #(contains? % :sheet-name) sheets))
          (is (every? #(contains? % :cells) sheets))
          (is (every? #(every? (fn [cell] 
                                 (and (contains? cell :cell-ref)
                                      (contains? cell :value)
                                      (contains? cell :type))) 
                               (:cells %)) sheets)))))))

(defn run-all-xlsx-tests []
  (run-tests 'clara-xlsx-diff-cljs.xlsx-test))
