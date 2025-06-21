(ns clara-xlsx-diff.dev.utils
  "Development utilities for Clara XLSX Diff"
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [promesa.core :as p]))

(defn workspace-file
  "Get absolute path to file in workspace"
  [relative-path]
  (path/join vscode/workspace.rootPath relative-path))

(defn read-workspace-file
  "Read contents of file in workspace"
  [relative-path]
  (p/let [uri (vscode/Uri.file (workspace-file relative-path))
          doc (vscode/workspace.openTextDocument uri)]
    (.-text doc)))

(defn show-in-editor
  "Show content in a new editor tab"
  [content & {:keys [language] :or {language "markdown"}}]
  (p/let [doc (vscode/workspace.openTextDocument 
               #js {:content content :language language})]
    (vscode/window.showTextDocument doc)))

(defn run-terminal-command
  "Run command in integrated terminal"
  [command]
  (let [terminal (or (.-activeTerminal vscode/window)
                     (vscode/window.createTerminal))]
    (.show terminal)
    (.sendText terminal command)))

(defn log-to-output
  "Log message to Clara XLSX Diff output channel"
  [message]
  (let [channel (vscode/window.createOutputChannel "Clara XLSX Diff")]
    (.appendLine channel (str (js/Date.) ": " message))
    (.show channel)))

(comment
  ;; Test utilities
  (workspace-file "docs/PROJECT_SUMMARY.md")
  (log-to-output "Testing output channel")
  (run-terminal-command "clj -M:dev -e '(+ 1 2 3)'")
  )
