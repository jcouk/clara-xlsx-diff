(ns xlsx-parsing.core-parsing
  "Tests for loading XLSX files and converting to EAV triples"
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clara-xlsx-diff.xlsx :as xlsx]
            [clara-xlsx-diff.eav :as eav]))

(deftest load-sample-data-as-triples-test
  (testing "Loading sample_data.xlsx and converting to EAV triples"
    (let [xlsx-data (xlsx/extract-data "test/sample_data.xlsx")
          eav-triples (eav/xlsx->eav xlsx-data :version :v1)]
      
      (testing "XLSX data extraction"
        (is (map? xlsx-data) "Should return a map structure")
        (is (contains? xlsx-data :file-path) "Should contain file path")
        (is (contains? xlsx-data :sheets) "Should contain sheets data")
        (is (vector? (:sheets xlsx-data)) "Sheets should be a vector")
        (is (pos? (:total-cells xlsx-data)) "Should have some cells"))
      
      (testing "EAV triple conversion"
        (is (sequential? eav-triples) "Should return a sequence of triples")
        (is (every? record? eav-triples) 
            "All triples should be EAV records")
        (is (pos? (count eav-triples)) "Should have some EAV triples")
        
        ;; Check structure of first triple
        (let [first-triple (first eav-triples)]
          (is (contains? first-triple :e) "EAV should have entity")
          (is (contains? first-triple :a) "EAV should have attribute") 
          (is (contains? first-triple :v) "EAV should have value")))
      
      (testing "EAV content verification"
        ;; Check that we have expected attributes for cells
        (let [attributes (set (map :a eav-triples))
              entities (set (map :e eav-triples))]
          (is (contains? attributes :cell/value) "Should have cell values")
          (is (contains? attributes :cell/ref) "Should have cell references")
          (is (contains? attributes :cell/sheet) "Should have sheet names")
          (is (contains? attributes :cell/version) "Should have version info")
          (is (every? string? entities) "All entities should be strings")
          (is (every? #(str/includes? % "v1:") entities) 
              "All entities should include version prefix"))))))

(deftest load-second-sample-data-test
  (testing "Loading sample_data_2.xlsx and converting to EAV triples"
    (let [xlsx-data (xlsx/extract-data "test/sample_data_2.xlsx")
          eav-triples (eav/xlsx->eav xlsx-data :version :v2)]
      
      (testing "Second file loads successfully"
        (is (map? xlsx-data) "Should return a map structure")
        (is (pos? (:total-cells xlsx-data)) "Should have some cells"))
      
      (testing "Version tagging works correctly"
        (let [entities (set (map :e eav-triples))]
          (is (every? #(str/includes? % "v2:") entities)
              "All entities should include v2 version prefix"))))))

(deftest compare-sample-files-structure-test
  (testing "Both sample files can be loaded and have comparable structure"
    (let [data1 (xlsx/extract-data "test/sample_data.xlsx")
          data2 (xlsx/extract-data "test/sample_data_2.xlsx")
          eav1 (eav/xlsx->eav data1 :version :v1)
          eav2 (eav/xlsx->eav data2 :version :v2)]
      
      (testing "Both files load successfully"
        (is (and (pos? (:total-cells data1)) (pos? (:total-cells data2)))
            "Both files should have cells"))
      
      (testing "EAV structures are comparable"
        (is (and (pos? (count eav1)) (pos? (count eav2)))
            "Both should generate EAV triples")
        
        ;; Check that we have the same types of attributes in both
        (let [attrs1 (set (map :a eav1))
              attrs2 (set (map :a eav2))]
          (is (= attrs1 attrs2) 
              "Both files should generate the same types of attributes"))))))


(comment
  
  (let [sample-data (xlsx/extract-data "test/sample_data.xlsx")
        eav-triples (eav/xlsx->eav sample-data :version :v1)]
    (take 5 eav-triples))


  :rcf)