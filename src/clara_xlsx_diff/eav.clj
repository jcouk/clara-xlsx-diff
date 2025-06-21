(ns clara-xlsx-diff.eav
  "EAV (Entity-Attribute-Value) transformation utilities for XLSX data")

;; Simple EAV record for our use case
(defrecord EAV [e a v])

(defn cell->eav
  "Transform a single cell into EAV triples.
  
  Each cell becomes an entity with attributes for its properties."
  [sheet-name cell version]
  (let [entity-id (str version ":" sheet-name ":" (:cell-ref cell))]
    [;; Core cell data as EAV records
     (->EAV entity-id :cell/sheet sheet-name)
     (->EAV entity-id :cell/ref (:cell-ref cell))
     (->EAV entity-id :cell/row (:row cell))
     (->EAV entity-id :cell/col (:col cell))
     (->EAV entity-id :cell/value (:value cell))
     (->EAV entity-id :cell/type (:type cell))
     (->EAV entity-id :cell/version version)]))

(defn sheet->eav
  "Transform a single sheet into EAV triples"
  [sheet version]
  (let [sheet-name (:sheet-name sheet)]
    (mapcat #(cell->eav sheet-name % version) (:cells sheet))))

(defn xlsx->eav
  "Transform XLSX data structure into EAV triples.
  
  Each cell becomes an entity with attributes describing its properties,
  making it easy to query and compare using Clara Rules."
  [xlsx-data & {:keys [version] :or {version :v1}}]
  (mapcat #(sheet->eav % version) (:sheets xlsx-data)))

(defn eav->cell-map
  "Convert EAV triples back to a cell-centric map for easier processing"
  [eav-triples]
  (reduce (fn [acc {:keys [e a v]}]
            (assoc-in acc [e a] v))
          {}
          eav-triples))

(defn get-cells-by-sheet
  "Get all cell entities grouped by sheet name from EAV triples"
  [eav-triples]
  (let [cell-map (eav->cell-map eav-triples)]
    (group-by #(get-in cell-map [% :cell/sheet]) (keys cell-map))))

(comment
  ;; REPL experiments
  (require '[clara-xlsx-diff.xlsx :as xlsx])
  
  ;; Transform sample data
  (def sample-data (xlsx/extract-data "test/resources/sample.xlsx"))
  (def eav-triples (xlsx->eav sample-data :version :original))
  
  ;; Inspect EAV structure
  (take 10 eav-triples)
  
  ;; Convert back to map for inspection
  (def cell-map (eav->cell-map eav-triples))
  (keys cell-map)
  
  ;; Group by sheet
  (get-cells-by-sheet eav-triples)
  )
