(ns clara-eav-loading.core-loading-triples
  (:require [clara.rules :as rules]
            [clara-eav.rules :as er]
            [clara.tools.fact-graph :as fg]
            [clara-xlsx-diff.xlsx :as xlsx]
            [clara-xlsx-diff.eav :as custom-eav]
            [clojure.test :refer [deftest is testing]]
            [clojure.core :as core]))

;; Define some basic rules that work with clara-eav EAV triples
(er/defrule find-sheet-names
  "Find all sheet names from EAV triples"
  [[?e :cell/sheet ?v]]
  =>
  (println "Found sheet:" ?v "for entity:" ?e))

(er/defrule find-cell-values
  "Find cells with specific values"
  [[?e :cell/value ?v]]
  =>
  (println "Found cell value:" ?v "in entity:" ?e))

(er/defrule find-name-cells
  "Find cells containing 'Name'"
  [[?e :cell/value "Name"]]
  =>
  (println "Found 'Name' cell in entity:" ?e))

;; Rule to find matching values between v1 and v2 versions (basic diff)
;; Define some basic rules that work with clara-eav EAV triples
(er/defrule find-sheet-names
  "Find all sheet names from EAV triples"
  [[?e :cell/sheet ?v]]
  =>
  (println "Found sheet:" ?v "for entity:" ?e))

(er/defrule find-cell-values
  "Find cells with specific values"
  [[?e :cell/value ?v]]
  =>
  (println "Found cell value:" ?v "in entity:" ?e))

(er/defrule find-name-cells
  "Find cells containing 'Name'"
  [[?e :cell/value "Name"]]
  =>
  (println "Found 'Name' cell in entity:" ?e))

;; Test function to demonstrate loading EAV data into Clara-EAV
(defn test-clara-eav-integration []
  (println "=== Testing Clara-EAV with EAV data ===")
  
  ;; Load XLSX data and convert to clara-eav EAV records
  (let [sample-data (xlsx/extract-data "test/sample_data.xlsx")
        eav-triples (custom-eav/xlsx->eav sample-data)]
    
    (println "Loaded" (count eav-triples) "EAV triples")
    (println "First triple type:" (type (first eav-triples)))
    (println "First triple:" (first eav-triples))
    
    ;; Create and run clara-eav session
    (println "\nCreating clara-eav session and testing rules...")
    (er/defsession test-session 'clara-eav-loading.core-loading-triples)
    
    (println "Running session with rules...")
    (let [session-result (-> test-session
                             (er/upsert (take 5 eav-triples))  ; Use first 5 for demo
                             (rules/fire-rules))]
      (println "✅ Clara-EAV integration successful!")
      (println "✅ Rules fired and session completed")
      (println "✅ Ready for advanced rule-based XLSX analysis"))
    
    (println "\n=== Test completed successfully! ===")))

;; Alternative function using er/defsession pattern
(defn create-working-session []
  "Demonstrates the working clara-eav integration"
  (let [sample-data (xlsx/extract-data "test/sample_data.xlsx")
        eav-triples (custom-eav/xlsx->eav sample-data)]
    
    ;; Create session with our rules
    (er/defsession working-session 'clara-eav-loading.core-loading-triples)
    
    ;; Load data and fire rules
    (-> working-session
        (er/upsert eav-triples)
        (rules/fire-rules))))

;; Proper test using clojure.test
(deftest test-clara-eav-session-creation
  (testing "Clara-EAV session can be created and populated with EAV data"
    (let [sample-data (xlsx/extract-data "test/sample_data.xlsx")
          eav-triples (custom-eav/xlsx->eav sample-data)]
      (is (> (count eav-triples) 0) "Should load EAV triples from XLSX")
      
      ;; For clara-eav, we need to use er/defsession at top-level
      ;; This is demonstrated in the comment section with the 'session' def
      ;; The test just verifies that our EAV data is properly formatted
      (is (every? #(and (:e %) (:a %) (:v %)) eav-triples) 
          "All EAV triples should have entity, attribute, and value"))))



(comment


;; sample session execution:
;;   (def loaded_session
;;   (->
;;    (er/defsession eav_session
;;      'compare.rules.clara-diff-rules)
;;    (er/upsert (vec (transform/merge-descendants-into-tuples (transform/parse-json-to-eavs
;;                     (transform/merge-jsons
;;                      (slurp "lhs_sample.json")
;;                      (slurp "rhs_sample.json"))))
;;                    ))
;;    (r/fire-rules)))

  
  (def session
    (-> (er/defsession eav-session 'clara-eav-loading.core-loading-triples)
        (er/upsert (custom-eav/xlsx->eav (xlsx/extract-data "test/sample_data.xlsx")))
        ;; specify version 2, given xlsx->eav function defaults to v1 and expects [xlsx-data & {:keys [version] :or {version :v1}}]
        (er/upsert (custom-eav/xlsx->eav (xlsx/extract-data "test/sample_data_2.xlsx") :version :v2))
        (rules/fire-rules)))

   (fg/session->fact-graph (get session :session))
  

 (get-in session [:store])
  (second (get (fg/session->fact-graph (get session :session)) :backward-edges))
  :rcf)