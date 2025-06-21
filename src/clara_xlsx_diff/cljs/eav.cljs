(ns clara-xlsx-diff.cljs.eav
  "ClojureScript EAV (Entity-Attribute-Value) transformation utilities for XLSX data")

;; Simple EAV record for ClojureScript
(defrecord EAV [e a v])

(defn cell->eav
  "Transform a single cell into EAV triples.
  
  Each cell becomes an entity with attributes for its properties."
  [sheet-name cell version]
  (let [entity-id (str (name version) ":" sheet-name ":" (:cell-ref cell))]
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

(defn filter-eav-by-attribute
  "Filter EAV triples by attribute"
  [eav-triples attribute]
  (filter #(= (:a %) attribute) eav-triples))

(defn get-entities-with-value
  "Get all entities that have a specific attribute-value pair"
  [eav-triples attribute value]
  (->> eav-triples
       (filter #(and (= (:a %) attribute) (= (:v %) value)))
       (map :e)
       set))

(comment
  ;; ClojureScript REPL experiments
  
  ;; Convert XLSX data to EAV
  ;; (let [eav-data (xlsx->eav xlsx-data :version :v1)]
  ;;   (js/console.log "EAV triples count:" (count eav-data)))
  
  ;; Find all cells in Engineering sheet
  ;; (let [engineering-cells (get-entities-with-value eav-data :cell/sheet "Engineering")]
  ;;   (js/console.log "Engineering cells:" engineering-cells))
  )
