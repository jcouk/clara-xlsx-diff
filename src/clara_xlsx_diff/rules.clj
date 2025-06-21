(ns clara-xlsx-diff.rules
  "Clara-EAV Rules for analyzing XLSX differences using EAV data.
  
  IMPORTANT: Due to clara-eav macro constraints, rules must be defined at the top level 
  of a namespace and loaded via (er/defsession session-name 'namespace) pattern.
  
  See the Rich Test Fixture (RTF) comment at the bottom for usage instructions."
  (:require [clara-eav.rules :as er]))

;; Simple rule to find cells that have matching values between v1 and v2 versions  
(er/defrule v1-v2-matching-values
  "Find cells where v1 and v2 have the same value in the same sheet/position"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/ref ?cell-ref]]
  [[?e1 :cell/value ?value1]]
  [[?e2 :cell/version :v2]]
  [[?e2 :cell/sheet ?sheet]]    ; same sheet
  [[?e2 :cell/ref ?cell-ref]]   ; same cell reference
  [[?e2 :cell/value ?value2]]    ; same value
  =>
  (let [matching? (= ?value1 ?value2)]
    (er/upsert-unconditional! [
                               [?e1 :v1:cell/match? matching?]
                               [?e2 :v2:cell/match? matching?]
    ])
    )
  )





(comment
  ;; Rich Test Fixture (RTF) - Instructions for testing clara-xlsx-diff rules
  ;;
  ;; These rules compare v1 and v2 versions of XLSX files and detect:
  ;;   ✅ MATCH: cells with same values in both versions
  ;;   🔄 CHANGED: cells with different values between versions  
  ;;   ❌ DELETED: cells present in v1 but missing in v2
  ;;   ➕ ADDED: cells present in v2 but missing in v1
  ;;
  ;; === STEP 1: Load required namespaces ===
  (require '[clara-xlsx-diff.xlsx :as xlsx])
  (require '[clara-xlsx-diff.eav :as custom-eav])
  (require '[clara-eav.rules :as er])
  (require '[clara.rules :as rules])
  
  ;; === STEP 2: Load sample data from v1 and v2 files ===
  (def v1-data (xlsx/extract-data "test/sample_data.xlsx"))
  (def v2-data (xlsx/extract-data "test/sample_data_2.xlsx"))
  
  ;; === STEP 3: Convert to EAV triples with version tags ===
  (def eav-v1 (custom-eav/xlsx->eav v1-data :version :v1))
  (def eav-v2 (custom-eav/xlsx->eav v2-data :version :v2))
  
  ;; Verify the data format:
  (println "V1 sample EAV:" (first eav-v1))
  (println "V2 sample EAV:" (first eav-v2))
  (println "V1 count:" (count eav-v1) "V2 count:" (count eav-v2))
  
  ;; === STEP 4: Create session and load both versions ===
  (er/defsession diff-session 'clara-xlsx-diff.rules)
  (def session (-> diff-session
                   (er/upsert eav-v1)          ; Load v1 data
                   (er/upsert eav-v2)          ; Load v2 data
                   (rules/fire-rules)))        ; Fire all diff rules
  
  ;; === Expected Output ===
  ;; You should see output like:
  ;;   ✅ MATCH: Employees A1 = Name (unchanged between v1 and v2)
  ;;   🔄 CHANGED: Employees B2 : Alice Johnson → Alice Smith
  ;;   ❌ DELETED: Employees C3 was 75000.0 (missing in v2)
  ;;   ➕ ADDED: Employees D4 = New Value (new in v2)
  ;;
  ;; === STEP 5: Experiment with rules ===
  ;; Try modifying the sample files and re-running to see different diff patterns.
  ;; Add more rules for specific analysis needs (e.g., salary changes, new employees).
  
  :rtf)
