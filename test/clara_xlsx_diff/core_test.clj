(ns clara-xlsx-diff.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clara-xlsx-diff.core :as core]))

(deftest compare-xlsx-test
  (testing "compare-xlsx function"
    (testing "with non-existent files"
      (is (thrown? Exception 
                   (core/compare-xlsx "non-existent1.xlsx" "non-existent2.xlsx"))))
    
    (testing "basic functionality structure"
      ;; This test would need actual test files
      ;; For now, just test that the function exists
      (is (fn? core/compare-xlsx))
      ;; The function accepts variable arguments via & rest params, so just check it exists
      (is (some? (resolve 'clara-xlsx-diff.core/compare-xlsx))))))

(deftest main-function-test
  (testing "-main function"
    (testing "with insufficient arguments"
      ;; Test would capture output to verify error message
      (is (fn? core/-main)))))

;; TODO: Add tests with actual XLSX files once test resources are created
