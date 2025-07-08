(ns clara-xlsx-diff-cljs.eav-test
  "Tests for ClojureScript EAV conversion"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clara-xlsx-diff.eav :as eav]))

(deftest eav-record-creation-test
  (testing "EAV record creation and structure"
    (let [eav-triple (eav/->EAV "entity-1" :attr/name "Alice")]
      (is (record? eav-triple) "Should create a record")
      (is (= (:e eav-triple) "entity-1") "Should have correct entity")
      (is (= (:a eav-triple) :attr/name) "Should have correct attribute")
      (is (= (:v eav-triple) "Alice") "Should have correct value"))))

(deftest cell->eav-conversion-test
  (testing "Converting a cell to EAV triples"
    (let [cell {:cell-ref "A1"
                :row 0
                :col 0
                :value "Test Value"
                :type "STRING"}
          eav-triples (eav/cell->eav "TestSheet" cell :v1)]
      
      (testing "Basic structure"
        (is (= (count eav-triples) 7) "Should create 7 EAV triples per cell")
        (is (every? record? eav-triples) "All triples should be EAV records"))
      
      (testing "Entity ID format"
        (let [entity-id (:e (first eav-triples))]
          (is (= entity-id "v1:TestSheet:A1") "Should have correct entity ID format")))
      
      (testing "Attribute coverage"
        (let [attributes (set (map :a eav-triples))]
          (is (= attributes #{:cell/sheet :cell/ref :cell/row :cell/col 
                             :cell/value :cell/type :cell/version})
              "Should have all expected attributes"))))))

(deftest sheet->eav-conversion-test
  (testing "Converting a sheet to EAV triples"
    (let [sheet {:sheet-name "TestSheet"
                 :cells [{:cell-ref "A1" :row 0 :col 0 :value "Header1" :type "STRING"}
                         {:cell-ref "B1" :row 0 :col 1 :value "Header2" :type "STRING"}
                         {:cell-ref "A2" :row 1 :col 0 :value "Data1" :type "STRING"}]}
          eav-triples (eav/sheet->eav sheet :v1)]
      
      (is (= (count eav-triples) 21) "Should create 21 triples (3 cells * 7 attributes)")
      
      (testing "Entity uniqueness"
        (let [entities (set (map :e eav-triples))]
          (is (= (count entities) 3) "Should have 3 unique entities")
          (is (every? #(str/includes? % "v1:TestSheet:") entities)
              "All entities should include version and sheet name"))))))

(deftest xlsx->eav-conversion-test
  (testing "Converting full XLSX data to EAV triples"
    (let [xlsx-data {:file-path "test.xlsx"
                     :sheets [{:sheet-name "Sheet1"
                               :cells [{:cell-ref "A1" :row 0 :col 0 :value "Name" :type "STRING"}
                                       {:cell-ref "B1" :row 0 :col 1 :value "Age" :type "STRING"}]}
                              {:sheet-name "Sheet2"
                               :cells [{:cell-ref "A1" :row 0 :col 0 :value "City" :type "STRING"}]}]
                     :sheet-count 2
                     :total-cells 3}
          eav-triples (eav/xlsx->eav xlsx-data :version :v2)]
      
      (is (= (count eav-triples) 21) "Should create 21 triples (3 cells * 7 attributes)")
      
      (testing "Version tagging"
        (let [entities (set (map :e eav-triples))]
          (is (every? #(str/includes? % "v2:") entities)
              "All entities should include v2 version tag"))))))

(deftest eav-utility-functions-test
  (testing "EAV utility functions"
    (let [eav-triples [(eav/->EAV "e1" :cell/sheet "Sheet1")
                       (eav/->EAV "e1" :cell/value "Test")
                       (eav/->EAV "e2" :cell/sheet "Sheet2")
                       (eav/->EAV "e2" :cell/value "Data")]]
      
      (testing "eav->cell-map conversion"
        (let [cell-map (eav/eav->cell-map eav-triples)]
          (is (= (get-in cell-map ["e1" :cell/sheet]) "Sheet1"))
          (is (= (get-in cell-map ["e2" :cell/value]) "Data"))))
      
      (testing "filter-eav-by-attribute"
        (let [sheet-triples (eav/filter-eav-by-attribute eav-triples :cell/sheet)]
          (is (= (count sheet-triples) 2))
          (is (every? #(= (:a %) :cell/sheet) sheet-triples))))
      
      (testing "get-entities-with-value"
        (let [sheet1-entities (eav/get-entities-with-value eav-triples :cell/sheet "Sheet1")]
          (is (= sheet1-entities #{"e1"})))))))

;; Remove the infinite loop - this was causing the stack overflow
;; To run tests, use (cljs.test/run-tests 'clara-xlsx-diff-cljs.eav-test) in REPL
