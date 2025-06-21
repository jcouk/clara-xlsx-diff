(ns clara-xlsx-diff.dev.commands
  "VS Code commands for Clara XLSX Diff development"
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn show-project-summary
  "Open and display the project summary"
  []
  (p/let [doc (vscode/workspace.openTextDocument 
               (vscode/Uri.file (str vscode/workspace.rootPath "/docs/PROJECT_SUMMARY.md")))]
    (vscode/window.showTextDocument doc)))

(defn run-sample-comparison
  "Run a sample XLSX comparison and show results"
  []
  (vscode/window.showInformationMessage 
   "Sample XLSX comparison - this would run the core comparison logic"))

(defn switch-ai-mood
  "Quick picker for switching AI mood/system prompt"
  []
  (p/let [moods ["architect" "joyride-hacker" "reviewer"]
          selected (vscode/window.showQuickPick 
                    (clj->js moods)
                    #js {:placeHolder "Select AI mood for current task"})]
    (when selected
      (vscode/window.showInformationMessage 
       (str "Switched to " selected " mode - update your system prompt accordingly")))))

(defn create-sample-xlsx-files
  "Create sample XLSX files for testing"
  []
  (vscode/window.showInformationMessage 
   "This would create sample XLSX files in test/resources/"))

(comment
  ;; Test commands in REPL
  (show-project-summary)
  (run-sample-comparison)
  (switch-ai-mood)
  )
