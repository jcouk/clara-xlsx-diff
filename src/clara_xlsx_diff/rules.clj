(ns clara-xlsx-diff.rules
  "Clara Rules for analyzing XLSX differences using EAV data"
  (:require [clara.rules :as r]
            [clara-xlsx-diff.eav :as eav]))

;; Define a simple record for EAV triples to work with Clara Rules
(defrecord EAV [e a v])

;; Simple approach: work with cell maps instead of raw EAV for now
;; We'll optimize to pure EAV later once the basic structure is working

(defn analyze-changes
  "Analyze differences between two XLSX datasets (simplified version)"
  [eav1 eav2]
  ;; For now, let's use a simpler approach that doesn't require complex Clara rules
  ;; We'll convert EAV back to a more workable format
  (let [cells1 (eav/eav->cell-map eav1)
        cells2 (eav/eav->cell-map eav2)
        all-refs (set (concat (keys cells1) (keys cells2)))
        changes (for [ref all-refs
                      :let [cell1 (get cells1 ref)
                            cell2 (get cells2 ref)]
                      :when (not= cell1 cell2)]
                  (cond
                    (and cell1 cell2) {:change/type :modified
                                       :change/sheet (:cell/sheet cell1)
                                       :change/cell-ref (:cell/ref cell1)
                                       :change/old-value (:cell/value cell1)
                                       :change/new-value (:cell/value cell2)}
                    
                    (and (nil? cell1) cell2) {:change/type :new
                                              :change/sheet (:cell/sheet cell2)
                                              :change/cell-ref (:cell/ref cell2)
                                              :change/value (:cell/value cell2)}
                    
                    (and cell1 (nil? cell2)) {:change/type :deleted
                                              :change/sheet (:cell/sheet cell1)
                                              :change/cell-ref (:cell/ref cell1)
                                              :change/value (:cell/value cell1)}))]
    (remove nil? changes)))

(defn summarize-changes
  "Generate summary statistics from change list"
  [changes]
  (let [by-type (group-by :change/type changes)]
    {:total (count changes)
     :modified (count (:modified by-type))
     :new (count (:new by-type))
     :deleted (count (:deleted by-type))
     :moved (count (:moved by-type))
     :by-sheet (frequencies (map :change/sheet changes))}))
