(ns clara-xlsx-diff.rules
  "Clara-EAV Rules for analyzing XLSX differences using EAV data.
  
  IMPORTANT: Due to clara-eav macro constraints, rules must be defined at the top level 
  of a namespace and loaded via (er/defsession session-name 'namespace) pattern.
  
  See the Rich Test Fixture (RTF) comment at the bottom for usage instructions."

  (:require
   [clara-xlsx-diff.util :as util]
   [clara-xlsx-diff.xlsx :as xlsx]
   [clara-xlsx-diff.eav :as eav]
   #?@(:clj [[clara.rules :as rules]
             [clara-eav.rules :as er]
             [portal.api :as p]
             ]
       :cljs [[clara.rules :as rules :include-macros true]
              [clara-eav.rules :as er :include-macros true]])))

;; Simple rule to find cells that have matching values between v1 and v2 versions  
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
                               [?e1 :cell/matchValue ?value2]
                               [?e2 :cell/match? matching?]
                               [?e2 :cell/matchValue ?value1]])))

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
  "For a given version, column, and sheet create an array of cell values for comparison purposes"
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
  [[?e1 :cell/matchValue ?matchValue]]
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
               ["newValueOutput" :output/matchValue ?matchValue]
               ["newValueOutput" :output/changeType "New"]
               ["newValueOutput" :output/version :v1]
               ]))


(er/defrule generate-cell-output-for-deleted-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version :v2]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/matchValue ?matchValue]]
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
               ["newValueOutput" :output/matchValue ?matchValue]
               ["newValueOutput" :output/changeType "Deleted"]
               ["newValueOutput" :output/version :v2]]))


(er/defrule generate-cell-output-for-change-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version ?version]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/matchValue ?matchValue]]
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
               ["newValueOutput" :output/matchValue ?matchValue]
               ["newValueOutput" :output/changeType "Change"]
               ["newValueOutput" :output/version ?version]
               ]))


(er/defrule generate-the-remaining-cell-output
  "generate an output for all the unchanged cells"
  [[?e1 :cell/version ?version]]
  [[?e1 :cell/match? true]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/matchValue ?matchValue]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  =>
  (er/upsert! [["newValueOutput" :output/col ?col]
               ["newValueOutput" :output/row ?row]
               ["newValueOutput" :output/sheet ?sheet]
               ["newValueOutput" :output/ref ?ref]
               ["newValueOutput" :output/value ?newValue]
               ["newValueOutput" :output/matchValue ?matchValue]
               ["newValueOutput" :output/changeType "None"]
               ["newValueOutput" :output/version ?version]
               ]))



(er/defquery get-all-outputs
  "Get all output cells generated by the rules"
  []
  [[?e :output/changeType]]
  [?output <- er/entities :from [[?e]]])

(comment
  ;; let [{:keys [eav-v1 eav-v2]} (process-to-eav)
  (def eav-v1 (eav/xlsx->eav (xlsx/extract-data "test/sample_data.xlsx") :version :v1))
  (def eav-v2 (eav/xlsx->eav (xlsx/extract-data "test/sample_data_2.xlsx") :version :v2))
  ;; Session is now defined above, outside the comment block
  ;; INCORRECT: Explicit rule vectors don't work properly in Clara-EAV
  (er/defsession test-session 'clara-xlsx-diff.rules)

  ;; Clara-EAV expects raw EAV records, not transformed vectors
  (def results (-> test-session
                   (er/upsert eav-v1)
                   (er/upsert eav-v2)
                   (rules/fire-rules)))
  
(def p (p/open))
(add-tap #'p/submit)
  
  (tap> (get-in results [:store :eav-index]))
 (tap> results)

  (get-in results [:store])

  (tap> (rules/query results get-all-ouputs))

  (rules/fire-rules result-session)

  ;; display complete result_session in the console
  ;; (tap> result-session)
  (def cell-records (->> (get-in results [:store :eav-index])
                         (filter (fn [[_entity-id data]]
                                   (and (map? data)
                                        (some #(= "cell" (namespace %)) (keys data)))))
                         (into [])))



  (er/defsession eav-session)
  (def test-analysis (run-diff-analysis))





  (def testdata '({:cell/row-col-pair {:row 0, :col 2},
                   :cell/version :v1,
                   :cell/ref "C1",
                   :eav/eid 64,
                   :cell/type "STRING",
                   :cell/value "Output: Bonus",
                   :cell/sheet "Decision_Table",
                   :cell/matchValue "Output: Bonus",
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
                   :cell/matchValue 2500,
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
                   :cell/matchValue "Junior Tech",
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
  ;; === ONE-STEP TEST FUNCTION ===
  ;; Run this function to execute the entire test pipeline in one go
  ;; (require '[portal.api :as p])
  ;; (require '[clara-xlsx-diff.xlsx :as xlsx])
  ;; (require '[clara-xlsx-diff.eav :as custom-eav])
  ;; (require '[clara-eav.rules :as er :include-macros true])
  ;; (require '[clara.rules :as rules :include-macros true])
  ;; (def p (p/open))
  ;; (add-tap #'p/submit)

  ;; Now you can use the functions defined above:
  ;; Example usage (use let bindings instead of def):
  ;; (let [result (run-diff-analysis)]
  ;;   (tap> (:eav-v1 result))
  ;;   (tap> (:eav-v2 result))
  ;;   (tap> (:session result)))



  ;; (let [complete-conversion (map eav-record->vec eav-v1)] ...)



  ;; Use the new functional approach instead:
  ;; (clara-xlsx-diff.rules/run-diff-analysis)

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

  ;; Use let bindings instead of def for local testing:
  ;; (let [filtered-cells
  ;;       (filter #(and (= (:cell/version (second %)) :v1)
  ;;                     (> (:cell/neighbors-not-matching (second %)) 0))
  ;;               test-cells)]
  ;;   (println "Filtered cells with neighbors not matching:" (count filtered-cells)))


  :rtf)
