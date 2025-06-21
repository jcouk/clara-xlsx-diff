(ns clara-xlsx-diff-cljs.test-runner
  "Main test runner for ClojureScript tests"
  (:require [cljs.test :refer [run-tests]]
            [clara-xlsx-diff-cljs.eav-test]
            [clara-xlsx-diff-cljs.xlsx-test]))

(defn ^:export run-all-tests []
  (run-tests 'clara-xlsx-diff-cljs.eav-test
             'clara-xlsx-diff-cljs.xlsx-test))

;; Run tests when this namespace is loaded in Node.js
(defn -main []
  (run-all-tests))
