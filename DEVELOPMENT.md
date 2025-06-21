# Clara XLSX Diff - Development Cheat Sheet

## Quick Start Commands

### Setup (One-time)
```bash
./setup.sh                    # Automated setup script
# OR manually:
npm install                   # Install Node.js deps
npx shadow-cljs server        # Start server with nREPL
```

### Development Builds
```bash
npx shadow-cljs watch :dev    # Browser development (localhost:8081)
npx shadow-cljs watch :test   # Test runner (Node.js)
```

### REPL Connection (VS Code/Calva)
1. `Ctrl+Shift+P` → "Calva: Connect to Running REPL"
2. `localhost:7002` → `shadow-cljs` → `:dev`

### Process Management
```bash
# Check what's running
ps aux | grep -E "(shadow|clj|java)" | grep -v grep
npx shadow-cljs clj-eval "(shadow.cljs.devtools.api/active-builds)"

# Stop everything cleanly
npx shadow-cljs stop
```

## REPL Testing Workflow

### Test Individual Functions
```clojure
;; Clojure REPL (clj)
(require '[clara-xlsx-diff.eav :as eav] :reload)
(eav/cell->eav "Sheet1" {:cell-ref "A1" :value "test"} :v1)

;; ClojureScript REPL (cljs)  
(require '[clara-xlsx-diff.cljs.eav :as eav] :reload)
(eav/cell->eav "Sheet1" {:cell-ref "A1" :value "test"} :v1)
```

### Test Namespaces
```clojure
;; Load test namespace (check compilation)
(require '[clara-xlsx-diff.eav-test :as test])

;; Run specific tests
(clojure.test/run-tests 'clara-xlsx-diff.eav-test)      ; Clojure
(cljs.test/run-tests 'clara-xlsx-diff-cljs.eav-test)   ; ClojureScript
```

## Common Issues & Solutions

### REPL Won't Connect
```bash
npx shadow-cljs stop
npx shadow-cljs server
# Wait for "nREPL server started on port 7002"
```

### Infinite Test Loop
1. Stop shadow-cljs immediately: `npx shadow-cljs stop`
2. Check test files for recursive calls
3. Test functions individually in REPL first

### Build Errors
```bash
npx shadow-cljs clean
rm -rf node_modules/.cache
npm install
npx shadow-cljs server
```

## File Structure Quick Reference

```
src/clara_xlsx_diff/
├── core.clj          # Main Clojure namespace
├── xlsx.clj          # JVM XLSX (Apache POI)
├── eav.clj           # JVM EAV transformation
└── cljs/
    ├── xlsx.cljs     # Browser XLSX (SheetJS)
    └── eav.cljs      # Browser EAV transformation

test/
├── clara_xlsx_diff/      # Clojure tests
└── clara_xlsx_diff_cljs/ # ClojureScript tests
```

## Development Principles

✅ **DO:**
- Test functions in REPL before running test suites
- Check running processes before starting/stopping
- Use `:reload` when requiring namespaces after changes
- Commit working code frequently

❌ **DON'T:**
- Use `pkill` unless absolutely necessary
- Run full test suites without REPL validation first
- Ignore infinite loops or stuck processes
- Skip process checks when troubleshooting
