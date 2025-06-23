(ns clara-xlsx-diff.rules
  "Clara-EAV Rules for analyzing XLSX differences using EAV data.
  
  IMPORTANT: Due to clara-eav macro constraints, rules must be defined at the top level 
  of a namespace and loaded via (er/defsession session-name 'namespace) pattern.
  
  See the Rich Test Fixture (RTF) comment at the bottom for usage instructions."
  (:require
   [clara-eav.rules :as er]
   [clara-xlsx-diff.util :as util]))

;; Simple rule to find cells that have matching values between v1 and v2 versions  
;; Really only need v1
(er/defrule match?-cell-comparison
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
    (er/upsert-unconditional! [[?e1 :cell/match? matching?]
                               [?e1 :cell/oldValue ?value2]])))

;; if the cell isn't matching, see if there is a cell on the other sheet and other version which does match
(er/defrule !cellMatch-find-match-if-possible
  "Go look for a match for itself in the case that it was just moved"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?value]]
  [[?e1 :cell/row-col-pair ?rowColPair1]]
  [[?e2 :cell/version :v2]]
  [[?e2 :cell/sheet ?sheet]]
  [[?e2 :cell/value ?value]]
  [[?e2 :cell/row-col-pair ?rowColPair2]]
  =>
  (er/upsert-unconditional! [["cellMatch" :cellMatch/v1Eid ?e1]
                             ["cellMatch" :cellMatch/v1-row-col-pair ?rowColPair1]
                             ["cellMatch" :cellMatch/v2Eid ?e2]
                             ["cellMatch" :cellMatch/sheet ?sheet]
                             ["cellMatch" :cellMatch/value ?value]
                             ["cellMatch" :cellMatch/v2-row-col-pair ?rowColPair2]]))


(er/defrule RowColPair-transform-row-col-pair
  "Transform row and column into a pair for easier neighbor calculations"
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/col ?col]]
  =>
  (let [pair {:row ?row :col ?col}]
    (er/upsert-unconditional! [[?e1 :cell/row-col-pair pair]])))

(er/defrule neighbor-calculate-all-neighboring-cells
  "Identify all pairs of cells that are immediate neighbors (8-directional adjacency)"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/row ?row1]]
  [[?e1 :cell/col ?col1]]
  =>
  (let [calculatedNeighbors (util/calculateAllRows ?row1 ?col1)
        ;; transform into a list of sets i.e. {{:row 1 :col 2} {:row 1 :col 3} ...}
        calculatedNeighborsMap (map (fn [[row col]]
                                      {:row row :col col})
                                    calculatedNeighbors)]
    (er/upsert-unconditional! [[?e1 :cell/neighbor calculatedNeighborsMap]])))



(er/defrule !neighor-entities
  "Create entites for each cell which map attributes we care about from the neighbor cells"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/neighbor ?neighbors]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e2 :cell/version :v1]]
  [[?e2 :cell/match? false]]
  [[?e2 :cell/sheet ?sheet]]
  [[?e2 :cell/row-col-pair ?rowColPair2]]
  [[?e2 :cell/match? ?matchValue]]
  [:test (some #(= % ?rowColPair2) ?neighbors)]
  =>
  (er/upsert-unconditional! [["newNeighborEntity" :neighbor/source ?e2]
                             ["newNeighborEntity" :neighbor/parentId ?e1]
                             ["newNeighborEntity" :neighbor/match? false]
                             ["newNeighborEntity" :neighbor/row-col-pair ?rowColPair2]
                             ;;  add other values as needed
                             ]))





(er/defrule !column-entities
  "Create entites for each cell on the same column"
  [[?e :cell/col ?col1]]
  [[?e :cell/sheet ?sheet]]
  [[?e :cell/version ?version]]
  [:not (:and [[_ :column/col ?col1]] [[_ :column/sheet ?sheet]] [[_ :column/version ?version]])]
  =>
  (er/upsert-unconditional! [["newColumnEntityStart" :column/col ?col1]
                             ["newColumnEntityStart" :column/sheet ?sheet]
                             ["newColumnEntityStart" :column/version ?version]]))



(er/defrule !row-entities
  "Create entites for each cell on the same row"
  [[?e :cell/col ?row1]]
  [[?e :cell/sheet ?sheet]]
  [[?e :cell/version ?version]]
  [:not (:and [[_ :column/col ?row1]] [[_ :column/sheet ?sheet]] [[_ :column/version ?version]])]
  =>
  (er/upsert-unconditional! [["newColumnEntityStart" :row/row ?row1]
                             ["newColumnEntityStart" :row/sheet ?sheet]
                             ["newColumnEntityStart" :row/version ?version]]))


(er/defrule !col-cells
  "for a given version, column, and sheet create an array of cell values for comparison purposes"
  [[?columnEid :column/col ?col1]]
  [[?columnEid :column/sheet ?sheet]]
  [[?columnEid :column/version ?version]]
  [[?e :cell/col ?col1]]
  [[?e :cell/sheet ?sheet]]
  [[?e :cell/version ?version]]
  [[?e :cell/value ?value]]
  ;; [[?e :cell/match? ?match]]
  [[?e :cell/row-col-pair ?columnColPair]]
  =>
  (er/upsert-unconditional! [["newcolumnCell" :columnCell/parentEid ?columnEid]
                             ["newcolumnCell" :columnCell/cellEid ?e]
                             ["newcolumnCell" :columnCell/value ?value]
                             ;;  ["newcolumnCell" :columnCell/match? ?match]
                             ["newcolumnCell" :columnCell/row-col-pair ?columnColPair]
                             ["newcolumnCell" :columnCell/sheet ?sheet]]))




(er/defrule !row-cells
  "for a given version, column, and sheet create an array of cell values for comparison purposes"
  [[?rowEid :row/row ?row1]]
  [[?rowEid :row/sheet ?sheet]]
  [[?rowEid :row/version ?version]]
  [[?e :cell/row ?row1]]
  [[?e :cell/sheet ?sheet]]
  [[?e :cell/version ?version]]
  [[?e :cell/value ?value]]
  [[?e :cell/match? ?match]]
  [[?e :cell/row-col-pair ?rowColPair]]
  =>
  (er/upsert-unconditional! [["newRowCell" :rowCell/parentEid ?rowEid]
                             ["newRowCell" :rowCell/cellEid ?e]
                             ["newRowCell" :rowCell/value ?value]
                             ["newRowCell" :rowCell/match? ?match]
                             ["newRowCell" :rowCell/row-col-pair ?rowColPair]
                             ["newRowCell" :rowCell/sheet ?sheet]]))

;; count number of neighbors which match
(er/defrule count-neighbors-not-matching
  "Count the number of neighboring cells that have matching values between v1 and v2"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/neighbor ?neighbors]]
  [[?e1 :cell/sheet ?sheet]]
  [?firstPass <- er/entities :from [:eav/all
                                    (= (:a this) :neighbor/parentId)
                                    (= (:v this) ?e1)]]
  =>
  (let [neighborsWithFalseMatch (count ?firstPass)]
    (er/upsert! [[?e1 :cell/count-neighbors-not-matching neighborsWithFalseMatch]])))




;; let's just get some output to play with and we can itterate from here
(er/defrule generate-cell-output-for-new-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/oldValue ?oldValue]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  [:not  [[?celMatchEdi :cellMatch/v1Eid ?e1]]]
  =>
  (er/upsert! [["newValueOutput" :output/col ?col]
               ["newValueOutput" :output/row ?row]
               ["newValueOutput" :output/sheet ?sheet]
               ["newValueOutput" :output/ref ?ref]
               ["newValueOutput" :output/value ?newValue]
               ["newValueOutput" :output/oldValue ?oldValue]
               ["newValueOutput" :output/changeType "New"]]))

(er/defrule generate-cell-output-for-change-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/oldValue ?oldValue]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  [[?celMatchEdi :cellMatch/v1Eid ?e1]]
  =>
  (er/upsert! [["newValueOutput" :output/col ?col]
               ["newValueOutput" :output/row ?row]
               ["newValueOutput" :output/sheet ?sheet]
               ["newValueOutput" :output/ref ?ref]
               ["newValueOutput" :output/value ?newValue]
               ["newValueOutput" :output/oldValue ?oldValue]
               ["newValueOutput" :output/changeType "Change"]]))





(comment

  (def testdata '({:cell/row-col-pair {:row 0, :col 2},
                   :cell/version :v1,
                   :cell/ref "C1",
                   :eav/eid 64,
                   :cell/type "STRING",
                   :cell/value "Output: Bonus",
                   :cell/sheet "Decision_Table",
                   :cell/oldValue "Output: Bonus",
                   :cell/match? true,
                   :cell/row 0,
                   :cell/col 2,
                   :cell/neighbor
                   ({:row 1, :col 1}
                    {:row 1, :col 3}
                    {:row 0, :col 3}
                    {:row 1, :col 2}
                    {:row 0, :col 1})}
                  {:cell/row-col-pair {:row 1, :col 2},
                   :cell/version :v1,
                   :cell/ref "C2",
                   :eav/eid 68,
                   :cell/type "NUMERIC",
                   :cell/value 2500,
                   :cell/sheet "Decision_Table",
                   :cell/oldValue 2500,
                   :cell/match? false,
                   :cell/row 1,
                   :cell/col 2,
                   :cell/neighbor
                   ({:row 2, :col 2}
                    {:row 2, :col 3}
                    {:row 1, :col 1}
                    {:row 1, :col 3}
                    {:row 0, :col 3}
                    {:row 0, :col 2}
                    {:row 2, :col 1}
                    {:row 0, :col 1})}
                  {:cell/row-col-pair {:row 1, :col 3},
                   :cell/version :v1,
                   :cell/ref "D2",
                   :eav/eid 69,
                   :cell/type "STRING",
                   :cell/value "Junior Tech",
                   :cell/sheet "Decision_Table",
                   :cell/oldValue "Junior Tech",
                   :cell/match? true,
                   :cell/row 1,
                   :cell/col 3,
                   :cell/neighbor
                   ({:row 2, :col 2}
                    {:row 2, :col 3}
                    {:row 1, :col 4}
                    {:row 0, :col 3}
                    {:row 2, :col 4}
                    {:row 0, :col 2}
                    {:row 0, :col 4}
                    {:row 1, :col 2})}))

  ;; count the number of records with :match? falsae
  (count (filter #(= (:cell/match? %) false) testdata))
  (some #(= % {:row 1 :col 2}) testdata)

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
  (require '[portal.api :as p])


  ;; === ONE-STEP TEST FUNCTION ===
  ;; Run this function to execute the entire test pipeline in one go
  (def p (p/open))
  (add-tap #'p/submit)

  (def v1-data (xlsx/extract-data "test/sample_data.xlsx"))
  (def v2-data (xlsx/extract-data "test/sample_data_2.xlsx"))
  (def eav-v1 (custom-eav/xlsx->eav v1-data :version :v1))
  (def eav-v2 (custom-eav/xlsx->eav v2-data :version :v2))
  (defn run-diff-test!
    "Executes the complete clara-xlsx-diff test pipeline.
    Returns a map with :session, :cell-records, and other useful data for inspection."
    []
    ;; Setup portal if not already done
    (when-not (bound? #'p)
      (def p (p/open))
      (add-tap #'p/submit))




    (let [diff-session (er/defsession diff-session 'clara-xlsx-diff.rules)
          session (-> diff-session
                      (er/upsert eav-v1)
                      (er/upsert eav-v2)
                      (rules/fire-rules))

          cell-records (->> (get-in session [:store :eav-index])
                            (filter (fn [[_entity-id data]]
                                      (and (map? data)
                                           (some #(= "output" (namespace %)) (keys data)))))
                            (into []))

          ;; Send to portal for inspection
          ;; _ (tap> cell-records)

          ;; Report results
          _ (println "✅ Test complete!")
          _ (println "Cell records found:" (count cell-records))
          _ (println "Sample cell record:" (first cell-records))]

      ;; Return useful data for further inspection
      {:session session
       ;;  filter out all cells except those with :version :v1
       :cell-records cell-records
       :v1-data v1-data
       :v2-data v2-data
       :eav-v1 eav-v1
       :eav-v2 eav-v2}))

  (tap> (run-diff-test!))

  ;; === QUICK TEST - Just run this! ===
  ;; (def test-results (run-diff-test!))




  (println "Cell records count:" (count cell-records))
  (println "Sample cell records:" (take 3 cell-records))

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


  (def test-cells [[105
                    {:cell/row-col-pair {:row 2, :col 3},
                     :cell/version :v1,
                     :cell/ref "D3",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Near Target",
                     :cell/sheet "Metrics",
                     :cell/oldValue "Near Target",
                     :cell/match? true,
                     :cell/row 2,
                     :cell/col 3,
                     :cell/neighbor
                     '({:row 2, :col 2}
                       {:row 3, :col 3}
                       {:row 3, :col 4}
                       {:row 1, :col 4}
                       {:row 1, :col 3}
                       {:row 2, :col 4}
                       {:row 1, :col 2}
                       {:row 3, :col 2})}]
                   [185
                    {:cell/sheet "Decision_Table",
                     :cell/ref "B1",
                     :cell/row 0,
                     :cell/col 1,
                     :cell/value "Input: Department",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [52
                    {:cell/row-col-pair {:row 3, :col 0},
                     :cell/version :v1,
                     :cell/ref "A4",
                     :cell/neighbor-count 3,
                     :cell/type "STRING",
                     :cell/value "R004",
                     :cell/sheet "Rules",
                     :cell/oldValue "R003",
                     :cell/match? false,
                     :cell/row 3,
                     :cell/col 0,
                     :cell/neighbor
                     '({:row 4, :col 1}
                       {:row 2, :col 0}
                       {:row 3, :col 1}
                       {:row 2, :col 1}
                       {:row 4, :col 0})}]
                   [114
                    {:cell/row-col-pair {:row 5, :col 0},
                     :cell/version :v1,
                     :cell/ref "A6",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Retention Rate",
                     :cell/sheet "Metrics",
                     :cell/oldValue "Retention Rate",
                     :cell/match? true,
                     :cell/row 5,
                     :cell/col 0,
                     :cell/neighbor
                     '({:row 4, :col 1}
                       {:row 5, :col 1}
                       {:row 6, :col 1}
                       {:row 6, :col 0}
                       {:row 4, :col 0})}]
                   [209
                    {:cell/sheet "Decision_Table",
                     :cell/ref "B7",
                     :cell/row 6,
                     :cell/col 1,
                     :cell/value "Any",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [147
                    {:cell/sheet "Employees",
                     :cell/ref "F5",
                     :cell/row 4,
                     :cell/col 5,
                     :cell/value "2024-01-05",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [67
                    {:cell/row-col-pair {:row 1, :col 1},
                     :cell/version :v1,
                     :cell/ref "B2",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Engineering",
                     :cell/sheet "Decision_Table",
                     :cell/oldValue "Engineering",
                     :cell/match? true,
                     :cell/row 1,
                     :cell/col 1,
                     :cell/neighbor
                     '({:row 2, :col 2}
                       {:row 0, :col 0}
                       {:row 1, :col 0}
                       {:row 0, :col 2}
                       {:row 2, :col 0}
                       {:row 2, :col 1}
                       {:row 1, :col 2}
                       {:row 0, :col 1})}]
                   [161
                    {:cell/sheet "Rules",
                     :cell/ref "C2",
                     :cell/row 1,
                     :cell/col 2,
                     :cell/value "senior_discount = true",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [71
                    {:cell/row-col-pair {:row 2, :col 1},
                     :cell/version :v1,
                     :cell/ref "B3",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Engineering",
                     :cell/sheet "Decision_Table",
                     :cell/oldValue "Engineering",
                     :cell/match? true,
                     :cell/row 2,
                     :cell/col 1,
                     :cell/neighbor
                     '({:row 2, :col 2}
                       {:row 1, :col 0}
                       {:row 1, :col 1}
                       {:row 3, :col 0}
                       {:row 2, :col 0}
                       {:row 3, :col 1}
                       {:row 1, :col 2}
                       {:row 3, :col 2})}]
                   [42
                    {:cell/row-col-pair {:row 1, :col 0},
                     :cell/version :v1,
                     :cell/ref "A2",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "R001",
                     :cell/sheet "Rules",
                     :cell/oldValue "R001",
                     :cell/match? true,
                     :cell/row 1,
                     :cell/col 0,
                     :cell/neighbor
                     '({:row 0, :col 0}
                       {:row 1, :col 1}
                       {:row 2, :col 0}
                       {:row 2, :col 1}
                       {:row 0, :col 1})}]
                   [80
                    {:cell/row-col-pair {:row 4, :col 2},
                     :cell/version :v1,
                     :cell/ref "C5",
                     :cell/neighbor-count 0,
                     :cell/type "NUMERIC",
                     :cell/value 3200,
                     :cell/sheet "Decision_Table",
                     :cell/oldValue 3200,
                     :cell/match? true,
                     :cell/row 4,
                     :cell/col 2,
                     :cell/neighbor
                     '({:row 4, :col 3}
                       {:row 3, :col 3}
                       {:row 5, :col 3}
                       {:row 4, :col 1}
                       {:row 5, :col 2}
                       {:row 5, :col 1}
                       {:row 3, :col 1}
                       {:row 3, :col 2})}]
                   [199
                    {:cell/sheet "Decision_Table",
                     :cell/ref "D4",
                     :cell/row 3,
                     :cell/col 3,
                     :cell/value "Junior Marketing",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [37
                    {:cell/row-col-pair {:row 0, :col 0},
                     :cell/version :v1,
                     :cell/ref "A1",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Rule ID",
                     :cell/sheet "Rules",
                     :cell/oldValue "Rule ID",
                     :cell/match? true,
                     :cell/row 0,
                     :cell/col 0,
                     :cell/neighbor
                     ({:row 1, :col 0} {:row 1, :col 1} {:row 0, :col 1})}]
                   [183
                    {:cell/sheet "Rules",
                     :cell/ref "E6",
                     :cell/row 5,
                     :cell/col 4,
                     :cell/value "Active",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [63
                    {:cell/row-col-pair {:row 0, :col 1},
                     :cell/version :v1,
                     :cell/ref "B1",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Input: Department",
                     :cell/sheet "Decision_Table",
                     :cell/oldValue "Input: Department",
                     :cell/match? true,
                     :cell/row 0,
                     :cell/col 1,
                     :cell/neighbor
                     '({:row 0, :col 0}
                       {:row 1, :col 0}
                       {:row 1, :col 1}
                       {:row 0, :col 2}
                       {:row 1, :col 2})}]
                   [212
                    {:cell/sheet "Decision_Table",
                     :cell/ref "A8",
                     :cell/row 7,
                     :cell/col 0,
                     :cell/value ">= 35",
                     :cell/type "STRING",
                     :cell/version :v2}]
                   [94
                    {:cell/row-col-pair {:row 0, :col 0},
                     :cell/version :v1,
                     :cell/ref "A1",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "Metric",
                     :cell/sheet "Metrics",
                     :cell/oldValue "Metric",
                     :cell/match? true,
                     :cell/row 0,
                     :cell/col 0,
                     :cell/neighbor
                     '({:row 1, :col 0} {:row 1, :col 1} {:row 0, :col 1})}]
                   [8
                    {:cell/row-col-pair {:row 1, :col 1},
                     :cell/version :v1,
                     :cell/ref "B2",
                     :cell/neighbor-count 0,
                     :cell/type "NUMERIC",
                     :cell/value 29,
                     :cell/sheet "Employees",
                     :cell/oldValue 29,
                     :cell/match? true,
                     :cell/row 1,
                     :cell/col 1,
                     :cell/neighbor
                     '({:row 2, :col 2}
                       {:row 0, :col 0}
                       {:row 1, :col 0}
                       {:row 0, :col 2}
                       {:row 2, :col 0}
                       {:row 2, :col 1}
                       {:row 1, :col 2}
                       {:row 0, :col 1})}]
                   [190
                    {:cell/sheet "Decision_Table",
                     :cell/ref "C2",
                     :cell/row 1,
                     :cell/col 2,
                     :cell/value 2500,
                     :cell/type "NUMERIC",
                     :cell/version :v2}]
                   [177
                    {:cell/sheet "Rules",
                     :cell/ref "D5",
                     :cell/row 4,
                     :cell/col 3,
                     :cell/value 2,
                     :cell/type "NUMERIC",
                     :cell/version :v2}]
                   [49
                    {:cell/row-col-pair {:row 2, :col 2},
                     :cell/version :v1,
                     :cell/ref "C3",
                     :cell/neighbor-count 3,
                     :cell/type "STRING",
                     :cell/value "tech_bonus = 5000",
                     :cell/sheet "Rules",
                     :cell/oldValue "tech_bonus = 5000",
                     :cell/match? true,
                     :cell/row 2,
                     :cell/col 2,
                     :cell/neighbor
                     '({:row 2, :col 3}
                       {:row 3, :col 3}
                       {:row 1, :col 1}
                       {:row 1, :col 3}
                       {:row 3, :col 1}
                       {:row 2, :col 1}
                       {:row 1, :col 2}
                       {:row 3, :col 2})}]
                   [84
                    {:cell/row-col-pair {:row 5, :col 2},
                     :cell/version :v1,
                     :cell/ref "C6",
                     :cell/neighbor-count 0,
                     :cell/type "STRING",
                     :cell/value "=IF(D2>70000,4500,2500)",
                     :cell/sheet "Decision_Table",
                     :cell/oldValue "=IF(D2>70000,4500,2500)",
                     :cell/match? true,
                     :cell/row 5,
                     :cell/col 2,
                     :cell/neighbor
                     '({:row 4, :col 3}
                       {:row 6, :col 3}
                       {:row 4, :col 2}
                       {:row 5, :col 3}
                       {:row 4, :col 1}
                       {:row 5, :col 1}
                       {:row 6, :col 1}
                       {:row 6, :col 2})}]])

  ;; filter out test-cells to only include :cell/version :v1 and :cell/neighbors-not-matching > 0
  (def filtered-cells
    (filter #(and (= (:cell/version (second %)) :v1)
                  (> (:cell/neighbors-not-matching (second %)) 0))
            test-cells))
  (println "Filtered cells with neighbors not matching:" (count filtered-cells))


  :rtf)
