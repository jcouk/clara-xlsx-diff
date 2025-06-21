(ns clara-xlsx-diff.output
  "Output generation for XLSX comparison results"
  (:require [hiccup.core :as h]
            [hiccup.page :as page]
            [clojure.string :as str]))

(defn format-change
  "Format a single change record for text output"
  [change]
  (case (:change/type change)
    :modified (format "[MODIFIED] %s!%s: '%s' -> '%s'"
                      (:change/sheet change)
                      (:change/cell-ref change)
                      (:change/old-value change)
                      (:change/new-value change))
    
    :new (format "[NEW] %s!%s: '%s'"
                 (:change/sheet change)
                 (:change/cell-ref change)
                 (:change/value change))
    
    :deleted (format "[DELETED] %s!%s: '%s'"
                     (:change/sheet change)
                     (:change/cell-ref change)
                     (:change/value change))
    
    :moved (format "[MOVED] %s: '%s' from %s to %s"
                   (:change/sheet change)
                   (:change/value change)
                   (:change/old-ref change)
                   (:change/new-ref change))))

(defn- change-css-class
  "Get CSS class for change type"
  [change-type]
  (case change-type
    :modified "change-modified"
    :new "change-new"
    :deleted "change-deleted"
    :moved "change-moved"
    "change-unknown"))

(defn- render-change-row
  "Render a single change as HTML table row"
  [change]
  [:tr {:class (change-css-class (:change/type change))}
   [:td (name (:change/type change))]
   [:td (:change/sheet change)]
   [:td (:change/cell-ref change "N/A")]
   [:td (case (:change/type change)
          :modified (str (:change/old-value change) " → " (:change/new-value change))
          :moved (str (:change/old-ref change) " → " (:change/new-ref change))
          (str (:change/value change)))]
   [:td (case (:change/type change)
          (:new :deleted) ""
          (str (:change/value change (:change/new-value change))))]])

(defn- render-summary
  "Render summary statistics as HTML"
  [summary]
  [:div.summary
   [:h2 "Summary"]
   [:div.stats
    [:div.stat [:span.label "Total Changes:"] [:span.value (:total summary)]]
    [:div.stat [:span.label "Modified:"] [:span.value (:modified summary)]]
    [:div.stat [:span.label "New:"] [:span.value (:new summary)]]
    [:div.stat [:span.label "Deleted:"] [:span.value (:deleted summary)]]
    [:div.stat [:span.label "Moved:"] [:span.value (:moved summary)]]]
   
   (when (seq (:by-sheet summary))
     [:div.by-sheet
      [:h3 "Changes by Sheet"]
      [:ul
       (for [[sheet count] (:by-sheet summary)]
         [:li (str sheet ": " count " changes")])]])])

(defn- render-changes-table
  "Render changes as HTML table"
  [changes]
  [:div.changes
   [:h2 "Detailed Changes"]
   [:table.changes-table
    [:thead
     [:tr
      [:th "Type"]
      [:th "Sheet"]
      [:th "Cell"]
      [:th "Change"]
      [:th "New Value"]]]
    [:tbody
     (map render-change-row changes)]]])

(defn- html-styles
  "CSS styles for HTML output"
  []
  [:style
   "
   body { font-family: Arial, sans-serif; margin: 20px; }
   .summary { margin-bottom: 30px; padding: 20px; background: #f5f5f5; border-radius: 5px; }
   .stats { display: flex; gap: 20px; flex-wrap: wrap; }
   .stat { display: flex; flex-direction: column; min-width: 100px; }
   .stat .label { font-weight: bold; color: #666; }
   .stat .value { font-size: 1.2em; color: #333; }
   .by-sheet { margin-top: 15px; }
   .by-sheet ul { list-style-type: none; padding: 0; }
   .by-sheet li { padding: 5px 0; }
   
   .changes-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
   .changes-table th, .changes-table td { padding: 8px 12px; text-align: left; border: 1px solid #ddd; }
   .changes-table th { background: #f8f9fa; font-weight: bold; }
   
   .change-modified { background-color: #fff3cd; }
   .change-new { background-color: #d4edda; }
   .change-deleted { background-color: #f8d7da; }
   .change-moved { background-color: #d1ecf1; }
   
   h1, h2, h3 { color: #333; }
   h1 { border-bottom: 2px solid #007bff; padding-bottom: 10px; }
   "])

(defn generate-html
  "Generate HTML report of XLSX comparison results"
  [changes data1 data2]
  (let [summary (require 'clara-xlsx-diff.rules)
        summary ((resolve 'clara-xlsx-diff.rules/summarize-changes) changes)]
    (h/html
     (page/html5
      [:head
       [:title "XLSX Comparison Report"]
       [:meta {:charset "utf-8"}]
       (html-styles)]
      [:body
       [:h1 "XLSX Comparison Report"]
       [:div.file-info
        [:p [:strong "File 1:"] (:file-path data1)]
        [:p [:strong "File 2:"] (:file-path data2)]
        [:p [:strong "Generated:"] (str (java.time.LocalDateTime/now))]]
       
       (render-summary summary)
       
       (when (seq changes)
         (render-changes-table changes))]))))

(comment
  ;; REPL experiments
  (require '[clara-xlsx-diff.core :as core])
  
  ;; Generate sample HTML
  (def result (core/compare-xlsx "test/resources/sample1.xlsx" 
                                 "test/resources/sample2.xlsx"))
  
  (def html (generate-html (:changes result) {} {}))
  
  ;; Save to file for inspection
  (spit "output.html" html)
  
  ;; Format individual changes
  (map format-change (take 3 (:changes result)))
  )
