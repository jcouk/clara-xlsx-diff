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
             [clara.rules.accumulators :as accum]
             [portal.api :as p]]
       :cljs [[clara.rules :as rules :include-macros true]
              [clara.rules.accumulators :as accum :include-macros true]
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
  [[?e2 :cell/value ?value2]]    ; possibly different value
  =>
  (let [matching? (= ?value1 ?value2)
        cell-sheet (keyword (str ?cell-ref "-" ?sheet))
        sheet-version-v1 (keyword (str ?sheet "-" :v1))
        sheet-version-v2 (keyword (str ?sheet "-" :v2))]
        
    (er/upsert-unconditional! [[?e1 :cell/match? matching?]
                               [?e1 :cell/matchValue ?value2]
                               [?e1 :cell/cellSheet cell-sheet]
                               [?e1 :cell/sheetVersion sheet-version-v1]
                               [?e2 :cell/match? matching?]
                               [?e2 :cell/matchValue ?value1]
                               [?e2 :cell/cellSheet cell-sheet]
                               [?e2 :cell/sheetVersion sheet-version-v2]])))
                               


(er/defrule !sheet-version-eids 
  ;; [[?e1 :cell/sheetVersion ?sheetVersion]]
  [?rowEids <- er/entities :from [:eav/all 
                                  ;; (= (:e this) ?e1)
                                  (= (:a this) :cell/sheet)
                                  (= (:v this)  ?sheet)]]
  =>(let [extractedEid (mapv :eav/eid ?rowEids)]
     (er/upsert! [(keyword ?sheet) :sheetInfo/Eids extractedEid])))


(er/defrule !sheet-version-max-row
  [[?e1 :sheetInfo/Eids ?sheetInfoEids]]
  [?sheetInfoEntities <- er/entities :from [:eav/all
                                            (some #(= (:e this) %) ?sheetInfoEids)]]
  =>(let [maxRow (apply max (mapv #(get-in % [:cell/row]) ?sheetInfoEntities))
          maxCol (apply max (mapv #(get-in % [:cell/col]) ?sheetInfoEntities))]
           
     (er/upsert! [
                  [?e1 :sheetInfo/maxRow maxRow]
                  [?e1 :sheetInfo/maxCol maxCol]])))


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
  [[?e2 :cell/ref ?cell-ref]]
  [[?e2 :cell/row-col-pair ?rowColPair2]]
  =>
 (let [cell-sheet (str ?cell-ref "-" ?sheet)]
    (er/upsert! [[(keyword cell-sheet) :cellMatch/v1Eid ?e1]
                 [(keyword cell-sheet) :cellMatch/v1-row-col-pair ?rowColPair1]
                 [(keyword cell-sheet) :cellMatch/v2Eid ?e2]
                 [(keyword cell-sheet) :cellMatch/sheet ?sheet]
                 [(keyword cell-sheet) :cellMatch/value ?value]
                 [(keyword cell-sheet) :cellMatch/v2-row-col-pair ?rowColPair2]])))


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



;; (er/defrule !neighor-entities
;;   "Create entites for each cell which map attributes we care about from the neighbor cells"
;;   [[?e1 :cell/version :v1]]
;;   [[?e1 :cell/neighbor ?neighbors]]
;;   [[?e1 :cell/sheet ?sheet]]
;;   [[?e2 :cell/version :v1]]
;;   [[?e2 :cell/match? false]]
;;   [[?e2 :cell/sheet ?sheet]]
;;   [[?e2 :cell/row-col-pair ?rowColPair2]]
;;   [[?e2 :cell/match? ?matchValue]]
;;   [:test (some #(= % ?rowColPair2) ?neighbors)]
;;   =>
;;   (er/upsert-unconditional! [["newNeighborEntity" :neighbor/source ?e2]
;;                              ["newNeighborEntity" :neighbor/parentId ?e1]
;;                              ["newNeighborEntity" :neighbor/match? false]
;;                              ["newNeighborEntity" :neighbor/row-col-pair ?rowColPair2]
;;                              ;;  add other values as needed
;;                              ]))





;; (er/defrule !column-entities
;;   "Create entites for each cell on the same column"
;;   [[?e :cell/col ?col1]]
;;   [[?e :cell/sheet ?sheet]]
;;   [[?e :cell/version ?version]]
;;   [:not (:and [[?matchEid :column/col ?col1]] [[?matchEid :column/sheet ?sheet]] [[?matchEid :column/version ?version]])]
;;   =>
;;   (er/upsert-unconditional! [["newColumnEntityStart" :column/col ?col1]
;;                              ["newColumnEntityStart" :column/sheet ?sheet]
;;                              ["newColumnEntityStart" :column/version ?version]]))



;; (er/defrule !row-entities
;;   "Create entites for each cell on the same row"
;;   [[?e :cell/row ?row1]]
;;   [[?e :cell/sheet ?sheet]]
;;   [[?e :cell/version ?version]]
;;   [:not (:and [[?matchEid :column/row ?row1]] [[?matchEid :column/sheet ?sheet]] [[?matchEid :column/version ?version]])]
;;   =>
;;   (er/upsert-unconditional! [["newColumnEntityStart" :row/row ?row1]
;;                              ["newColumnEntityStart" :row/sheet ?sheet]
;;                              ["newColumnEntityStart" :row/version ?version]]))


;; (er/defrule !col-cells
;;   "for a given version, column, and sheet create an array of cell values for comparison purposes"
;;   [[?columnEid :column/col ?col1]]
;;   [[?columnEid :column/sheet ?sheet]]
;;   [[?columnEid :column/version ?version]]
;;   [[?e :cell/col ?col1]]
;;   [[?e :cell/sheet ?sheet]]
;;   [[?e :cell/version ?version]]
;;   [[?e :cell/value ?value]]
;;   ;; [[?e :cell/match? ?match]]
;;   [[?e :cell/row-col-pair ?columnColPair]]
;;   =>
;;   (er/upsert-unconditional! [["newcolumnCell" :columnCell/parentEid ?columnEid]
;;                              ["newcolumnCell" :columnCell/cellEid ?e]
;;                              ["newcolumnCell" :columnCell/value ?value]
;;                              ;;  ["newcolumnCell" :columnCell/match? ?match]
;;                              ["newcolumnCell" :columnCell/row-col-pair ?columnColPair]
;;                              ["newcolumnCell" :columnCell/sheet ?sheet]]))




;; (er/defrule !row-cells
;;   "For a given version, column, and sheet create an array of cell values for comparison purposes"
;;   [[?rowEid :row/row ?row1]]
;;   [[?rowEid :row/sheet ?sheet]]
;;   [[?rowEid :row/version ?version]]
;;   [[?e :cell/row ?row1]]
;;   [[?e :cell/sheet ?sheet]]
;;   [[?e :cell/version ?version]]
;;   [[?e :cell/value ?value]]
;;   [[?e :cell/match? ?match]]
;;   [[?e :cell/row-col-pair ?rowColPair]]
;;   =>
;;   (er/upsert-unconditional! [["newRowCell" :rowCell/parentEid ?rowEid]
;;                              ["newRowCell" :rowCell/cellEid ?e]
;;                              ["newRowCell" :rowCell/value ?value]
;;                              ["newRowCell" :rowCell/match? ?match]
;;                              ["newRowCell" :rowCell/row-col-pair ?rowColPair]
;;                              ["newRowCell" :rowCell/sheet ?sheet]]))

;; ;; count number of neighbors which match
;; (er/defrule count-neighbors-not-matching
;;   "Count the number of neighboring cells that have matching values between v1 and v2"
;;   [[?e1 :cell/version :v1]]
;;   [[?e1 :cell/neighbor ?neighbors]]
;;   [[?e1 :cell/sheet ?sheet]]
;;   [?firstPass <- er/entities :from [:eav/all
;;                                     (= (:a this) :neighbor/parentId)
;;                                     (= (:v this) ?e1)]]
;;   =>
;;   (let [neighborsWithFalseMatch (count ?firstPass)]
;;     (er/upsert! [[?e1 :cell/count-neighbors-not-matching neighborsWithFalseMatch]])))





(er/defrule generate-cell-output-for-new-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version :v1]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?value]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  ;; [:not [[?celMatchEdi :cellMatch/v1Eid ?e1]]] 
  [:not [:eav/all
         (= (:e this) ?e1)
         (= (:a this) :cell/match?)]]
  =>
  (let [sheetAndVersionIdentif (str ?ref "-" ?sheet "-" :v1 "-New")]
    (er/upsert! [[(keyword sheetAndVersionIdentif) :output/col ?col]
                 [(keyword sheetAndVersionIdentif) :output/row ?row]
                 [(keyword sheetAndVersionIdentif) :output/sheet ?sheet]
                 [(keyword sheetAndVersionIdentif) :output/ref ?ref]
                 [(keyword sheetAndVersionIdentif) :output/value ?value]
                 [(keyword sheetAndVersionIdentif) :output/matchValue "N/A"]
                 [(keyword sheetAndVersionIdentif) :output/changeType "New"]
                 [(keyword sheetAndVersionIdentif) :output/version :v1]])))


(er/defrule generate-cell-output-for-deleted-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version :v2]] 
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?value]] 
  [:not [:eav/all
         (= (:e this) ?e1)
         (= (:a this) :cell/match?)]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  =>
  (let [sheetAndVersionIdentif (str ?ref "-" ?sheet "-" :v2 "-Deleted")]
    (er/upsert! [[(keyword sheetAndVersionIdentif) :output/col ?col]
                 [(keyword sheetAndVersionIdentif) :output/row ?row]
                 [(keyword sheetAndVersionIdentif) :output/sheet ?sheet]
                 [(keyword sheetAndVersionIdentif) :output/ref ?ref]
                 [(keyword sheetAndVersionIdentif) :output/value ?value]
                 [(keyword sheetAndVersionIdentif) :output/matchValue "N/A"]
                 [(keyword sheetAndVersionIdentif) :output/changeType "Deleted"]
                 [(keyword sheetAndVersionIdentif) :output/version :v2]])))


(er/defrule generate-cell-output-for-change-value
  "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
  [[?e1 :cell/version ?version]]
  [[?e1 :cell/match? false]]
  [[?e1 :cell/sheet ?sheet]]
  [[?e1 :cell/value ?newValue]]
  [[?e1 :cell/matchValue ?matchValue]]
  [[?e1 :cell/cellSheet ?cellSheet]]
  [[?e1 :cell/col ?col]]
  [[?e1 :cell/row ?row]]
  [[?e1 :cell/ref ?ref]]
  =>
  (let [sheetAndVersionIdentif (str ?ref "-" ?sheet "-" ?version "-Changed")]
    (er/upsert! [[(keyword sheetAndVersionIdentif) :output/col ?col]
                 [(keyword sheetAndVersionIdentif) :output/row ?row]
                 [(keyword sheetAndVersionIdentif) :output/sheet ?sheet]
                 [(keyword sheetAndVersionIdentif) :output/ref ?ref]
                 [(keyword sheetAndVersionIdentif) :output/value ?newValue]
                 [(keyword sheetAndVersionIdentif) :output/matchValue ?matchValue]
                 [(keyword sheetAndVersionIdentif) :output/changeType "Change"]
                 [(keyword sheetAndVersionIdentif) :output/version ?version]])))


;; (er/defrule generate-cell-output-for-change-value_v2
;;   "for each cell in v1 where v1 is the latest and v2 is the new file, output a cell for each diff"
;;   {:salience -200}
;;   [[?e1 :cell/version :v2]]
;;   [[?e1 :cell/match? false]]
;;   [[?e1 :cell/sheet ?sheet]]
;;   [[?e1 :cell/value ?newValue]]
;;   [[?e1 :cell/matchValue ?matchValue]]
;;   [[?e1 :cell/cellSheet ?cellSheet]]
;;   [[?e1 :cell/col ?col]]
;;   [[?e1 :cell/row ?row]]
;;   [[?e1 :cell/ref ?ref]]
;;   [[?cellSheet :cellMatch/v2Eid ?e1]]
;;   =>
;;   (let [sheetAndVersionIdentif (str ?ref "-" ?sheet "-" :v2 "-Changed")]
;;     (er/upsert! [[(keyword sheetAndVersionIdentif) :output/col ?col]
;;                  [(keyword sheetAndVersionIdentif) :output/row ?row]
;;                  [(keyword sheetAndVersionIdentif) :output/sheet ?sheet]
;;                  [(keyword sheetAndVersionIdentif) :output/ref ?ref]
;;                  [(keyword sheetAndVersionIdentif) :output/value ?newValue]
;;                  [(keyword sheetAndVersionIdentif) :output/matchValue ?matchValue]
;;                  [(keyword sheetAndVersionIdentif) :output/changeType "Change"]
;;                  [(keyword sheetAndVersionIdentif) :output/version :v2]])))


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
  (let [sheetAndVersionIdentif (str ?ref "-" ?sheet "-" ?version "Identical")]
    (er/upsert! [[(keyword sheetAndVersionIdentif) :output/col ?col]
                 [(keyword sheetAndVersionIdentif) :output/row ?row]
                 [(keyword sheetAndVersionIdentif) :output/sheet ?sheet]
                 [(keyword sheetAndVersionIdentif) :output/ref ?ref]
                 [(keyword sheetAndVersionIdentif) :output/value ?newValue]
                 [(keyword sheetAndVersionIdentif) :output/matchValue ?matchValue]
                 [(keyword sheetAndVersionIdentif) :output/changeType "None"]
                 [(keyword sheetAndVersionIdentif) :output/version ?version]])))





(er/defrule !sheet-version-output-eids
  ;; [[?e1 :cell/sheetVersion ?sheetVersion]]
  [?rowEids <- er/entities :from [:eav/all
                                  ;; (= (:e this) ?e1)
                                  (= (:a this) :output/sheet)
                                  (= (:v this)  ?sheet)]]
  => (let [extractedEid (mapv :eav/eid ?rowEids)]
       (er/upsert! [(keyword ?sheet) :sheetInfo/outputEids extractedEid])))



(er/defrule !sheet-version-output-counts
  [[?e1 :sheetInfo/outputEids ?sheetInfoEids]]
  [?sheetInfoEntities <- er/entities :from [:eav/all
                                            (some #(= (:e this) %) ?sheetInfoEids)]]
  => (let [None-Change-Type_v1 (count (filter #(and (= (:output/changeType %) "None") (= (:output/version %) :v1)) ?sheetInfoEntities))
           Change-Change-Type_v1 (count (filter #(and (= (:output/changeType %) "Change") (= (:output/version %) :v1)) ?sheetInfoEntities))
           New-Change-Type_v1 (count (filter #(and (= (:output/changeType %) "New") (= (:output/version %) :v1)) ?sheetInfoEntities))
           Deleted-Change-Type_v2 (count (filter #(and (= (:output/changeType %) "Deleted") (= (:output/version %) :v2)) ?sheetInfoEntities))
           Change-Change-Type_v2 (count (filter #(and (= (:output/changeType %) "Change") (= (:output/version %) :v2)) ?sheetInfoEntities))
           None-Change-Type_v2 (count (filter #(and (= (:output/changeType %) "None") (= (:output/version %) :v2)) ?sheetInfoEntities))
           v1-results-object {:None None-Change-Type_v1 
                              :Change Change-Change-Type_v1
                              :New New-Change-Type_v1}
           v2-results-object {:None None-Change-Type_v2
                              :Change Change-Change-Type_v2
                              :Deleted Deleted-Change-Type_v2}]

       (er/upsert! [[?e1 :sheetInfo/v1Results v1-results-object]
                    [?e1 :sheetInfo/v2Results v2-results-object]])))

;; Learned that if you can identify a keyword- the best thing to do is to use the keyword as the entity id
;; i think the truth value logic works really hard otherwise if the entity being created is the same record you're checking

;; (er/defrule create-summary-results-records
;;   "Create a new record type of summary to help organize the results"
;;   [[?e1 :output/sheetAndVersionIdentif ?sheetAndVersionIdentif]]
;;   [[?e1 :output/changeType ?changeType]]
;;   [[?e1 :output/version ?version]]
;;   [[?e1 :output/sheet ?sheet]] 
;;   [?eids <- er/entities :from [[?e1 :output/sheetAndVersionIdentif ?sheetAndVersionIdentif]]] 
;;   [?cells <- er/entities :from [:eav/all
;;                                 (contains? (util/extract-Eid-from-entities ?eids) (:e this))]]
;;   ;; [?counts <- (accum/count) :from [:eav/all
;;   ;;                                   (contains? (util/extract-Eid-from-entities ?eids) (:e this))]]
;;   ;; [?values <- er/entities :from [[?e1]]]  ;; something interesting is happening here where the count accumulator is working fine but the er/entites accumulator is only returning the one value
;;   =>
;;   (er/upsert! [[(keyword ?sheetAndVersionIdentif) :summary/sheet ?sheet]
;;                [(keyword ?sheetAndVersionIdentif) :summary/version ?version]
;;                [(keyword ?sheetAndVersionIdentif) :summary/changeType ?changeType]]))
              ;;  [(keyword ?sheetAndVersionIdentif) :summary/count ?counts]
              ;;  [(keyword ?sheetAndVersionIdentif) :summary/cells ?cells]
              ;; [(keyword ?sheetAndVersionIdentif) :summary/values ?values]
                
              




;; (er/defrule create-test-results-records
;;   "Create a new record type of summary to help organize the results"
;;   [:not [[:first :wackyValue/sheet]]]
;;   [[?e1 :output/changeType ?changeType]]
;;   [[?e1 :output/version ?version]]
;;   [[?e1 :output/sheet ?sheet]]
;;   =>
;;   (er/upsert! [[:first  :wackyValue/triggerRule "simpleNot"]
;;                [:first  :wackyValue/sheet ?sheet]
;;                [:first  :wackyValue/version ?version]
;;                [:first  :wackyValue/changeType ?changeType]]))



;; (er/defrule extract-sheet-data
;;   "Extract the sheet name and version from the output entity" 
;;   [[?e1 :cell/version ?version]]
;;   [[?e1 :cell/sheet ?sheet]]
;;   [[?e1 :cell/sheetVerfsion ?sheetVersion]]
;;   [?allSheetVersionsEids <- er/entities :from [:eav/all
;;                                      (= (:a this) :cell/sheetVersion)
;;                                      (= (:v this) ?sheetVersion)]]
;;   [?maxCol <- (accum/max :col) :from [[?e1 :cell/col]]]
;;   [?maxRow <- (accum/max :row) :from [[?e1 :cell/row]]]

;;   =>
;;   (let [sheet-version-identif (str ?sheet "-" ?version)]
;;     (er/upsert! [[(keyword sheet-version-identif) :sheetData/sheet ?sheet]
;;                  [(keyword sheet-version-identif) :sheetData/version ?version]
;;                  [(keyword sheet-version-identif) :sheetData/maxCol ?maxCol]
;;                  [(keyword sheet-version-identif) :sheetData/maxRow ?maxRow]])))


(comment
  ;; let [{:keys [eav-v1 eav-v2]} (process-to-eav)
  (def eav-v1 (eav/xlsx->eav (xlsx/extract-data "test/sample_data.xlsx") :version :v1))
  (def eav-v2 (eav/xlsx->eav (xlsx/extract-data "test/sample_data_2.xlsx") :version :v2))  ;; Session is now defined above, outside the comment block  ;; INCORRECT: Explicit rule vectors don't work properly in Clara-EAV
  (er/defsession test-session 'clara-xlsx-diff.rules)

  (count eav-v1)
  (count eav-v2)

  (+ (count eav-v1) (count eav-v2))

  ;; Clara-EAV expects raw EAV records, not transformed vectors
  (def results (-> test-session
                   (er/upsert eav-v1)
                   (er/upsert eav-v2)
                   (rules/fire-rules)))

  (def p (p/open))
  (add-tap #'p/submit)

  (tap> results)

  (tap> (get-in results [:store :eav-index]))
  (tap> results)

  (get-in results [:store])

  (tap> (rules/query results get-all-outputs))

  (tap> (rules/query results get-all-summary-results))

  (def query-results  (rules/query results get-all-outputs))


  (def test-summary (->> (get-in results [:store :eav-index])
                         (filter (fn [[_entity-id data]]
                                   (and (map? data)
                                        (some #(= "sheetInfo" (namespace %)) (keys data)))))
                         (into [])
                        ;;  return a a single object with {:key {:maxCol, :maxRow, :v1Results, :v2Results}}
                          (map (fn [[_entity-id data]] ;; _entity-id should be the parent key
                                  (let [v1-results (get data :sheetInfo/v1Results)
                                        v2-results (get data :sheetInfo/v2Results)]
                                    {:sheetName (name _entity-id)
                                      :maxCol (get data :sheetInfo/maxCol)
                                      :maxRow (get data :sheetInfo/maxRow)
                                      :v1Results v1-results
                                      :v2Results v2-results})))
                         
                        ;;  put all the results together in a map
                          (into {} (map (fn [m] [(keyword (:sheetName m)) m])))
                         )  ; convert sheetName to keyword for easier access
                         )
  
                         
    ;; remove any keys with "Eids" in them
                        ;;  (map (fn [m]
                        ;;         (into {} (remove (fn [[k _]] (clojure.string/includes? (name k) "Eids")) m))))
                        ;;  (map (fn [m]
                        ;;         (update m :sheetInfo/v1Results #(dissoc % :values)))
                        ;;  )


  (tap> test-summary)

  (tap> (map (fn [{:keys [?output]}]
               (first ?output))  ; output is a sequence, so take the first (and only) item
             query-results))
  ;; Example output query
  ;; ({:?e 3450,
  ;;  :?output
  ;;  ({:output/row 3,
  ;;    :eav/eid 3450,
  ;;    :output/ref "B4",
  ;;    :output/sheet "Rules",
  ;;    :output/value "department = \"Sales\"",
  ;;    :output/col 1,
  ;;    :output/changeType "New",
  ;;    :output/matchValue "R002",
  ;;    :output/version :v1})})
  ;; turn the example output into a sequence of maps with the result of :?output



  (rules/fire-rules result-session)

  ;; display complete result_session in the console
  ;; (tap> result-session)
  (def cell-records (->> (get-in results [:store :eav-index])
                         (filter (fn [[_entity-id data]]
                                   (and (map? data)
                                        (some #(= "output" (namespace %)) (keys data)))))
                         (into [])
                         (map second)))  ; get the value part of the map



  (def grouped-by-sheet (group-by :output/sheet cell-records))

  ;; add to group-by-sheet meta-data including 
  ;; :v1-summary, :v2-summary, :highest-col, :highest-row, :values
  (defn add-sheet-meta-data [sheet-records]
    (let [v1-values (filter #(= (:output/version %) :v1) sheet-records)
          v2-values (filter #(= (:output/version %) :v2) sheet-records)
          num-v1-changes (count (filter #(= (:output/changeType %) "Change") v1-values))
          num-v2-changes (count (filter #(= (:output/changeType %) "Change") v2-values))
          highest-col (apply max (map :output/col sheet-records))
          highest-row (apply max (map :output/row sheet-records))]

      {:v1-values {:num-changes num-v1-changes
                   :values v1-values}
       :v2-values {:num-changes num-v2-changes
                   :values v2-values}
       :highest-col highest-col
       :highest-row highest-row}))





  (er/defsession eav-session)
  (def test-analysis (run-diff-analysis))



  :rtf
  )
  
  
