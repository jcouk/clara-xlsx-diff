(ns clara-xlsx-diff.dev
  "Development namespace for ClojureScript REPL"
  (:require [clara-xlsx-diff.browser :as browser]
            [clara-xlsx-diff.cljs.xlsx :as xlsx]
            [clara-xlsx-diff.cljs.eav :as eav]))

(defn ^:export init!
  "Initialize development environment"
  []
  (js/console.log "Clara XLSX Diff - Development mode")
  (browser/init!))

(defn ^:export test-eav-creation
  "Test EAV creation with sample data"
  []
  (let [sample-cell {:cell-ref "A1" :row 0 :col 0 :value "Test" :type "STRING"}
        eav-triples (eav/cell->eav "TestSheet" sample-cell :v1)]
    (js/console.log "Sample EAV triples:" eav-triples)
    eav-triples))

(defn ^:export test-cell-reference
  "Test cell reference generation"
  []
  (let [refs (for [r (range 3) c (range 3)]
               [r c (xlsx/cell-reference r c)])]
    (js/console.log "Cell references:" refs)
    refs))

(comment
  ;; Development REPL commands:
  ;; (clara-xlsx-diff.dev/test-eav-creation)
  ;; (clara-xlsx-diff.dev/test-cell-reference)
  )
