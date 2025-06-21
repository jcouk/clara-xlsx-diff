# Clara XLSX Diff

A Clojure/ClojureScript library for comparing XLSX files using Entity-Attribute-Value (EAV) models and Clara Rules engine for intelligent difference detection.

## Overview

This project transforms XLSX file data into EAV triples, making it easy to:
- Compare spreadsheets semantically using Clara Rules
- Track changes across different versions of XLSX files  
- Query spreadsheet data using logic-based rules
- Support both JVM Clojure and ClojureScript (browser/Node.js) environments

## Project Structure

```
clara-xlsx-diff/
├── deps.edn                 # Clojure dependencies and aliases
├── shadow-cljs.edn          # ClojureScript build configuration
├── package.json             # NPM dependencies for ClojureScript
├── src/
│   └── clara_xlsx_diff/
│       ├── core.clj         # Main Clojure namespace
│       ├── xlsx.clj         # JVM XLSX parsing (Apache POI)
│       ├── eav.clj          # JVM EAV transformation
│       └── cljs/
│           ├── xlsx.cljs    # ClojureScript XLSX parsing (SheetJS)
│           └── eav.cljs     # ClojureScript EAV transformation
├── test/
│   ├── clara_xlsx_diff/     # Clojure tests
│   └── clara_xlsx_diff_cljs/ # ClojureScript tests
└── public/                  # Static files for browser demo
    └── index.html
```

## Prerequisites

- **Java 17+** (for Clojure)
- **Node.js 18+** (for ClojureScript)
- **Clojure CLI tools** (latest version)
- **VS Code with Calva extension** (recommended for REPL-driven development)

## Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd clara-xlsx-diff
```

### 2. Install Dependencies

**Clojure dependencies:**
```bash
# Dependencies are managed via deps.edn - no separate install needed
clojure -P  # Pre-download dependencies (optional)
```

**Node.js dependencies for ClojureScript:**
```bash
npm install
```

## Development Workflow (REPL-Driven)

This project follows a **REPL-driven development** approach for interactive testing and debugging.

### 1. Start Shadow-CLJS Server with nREPL

```bash
# Start shadow-cljs server with nREPL support
npx shadow-cljs server
```

This starts:
- Shadow-CLJS server on port 9630
- nREPL server on port 7002

### 2. Start Watch Processes

**For ClojureScript development:**
```bash
# Start development build (browser)
npx shadow-cljs watch :dev

# Start test build (Node.js)
npx shadow-cljs watch :test
```

### 3. Connect REPL in VS Code (Calva)

1. **Command Palette** → `Calva: Connect to a Running REPL Server`
2. **Host:** `localhost`, **Port:** `7002`
3. **Project Type:** `shadow-cljs`
4. **Build:** `:dev` (for browser) or `:test` (for testing)

You'll now have both:
- **Clojure REPL** (`clj`) for JVM code
- **ClojureScript REPL** (`cljs`) for browser/Node.js code

### 4. Interactive Development

**Test functions individually in REPL:**
```clojure
;; Test Clojure EAV conversion
(require '[clara-xlsx-diff.eav :as eav])
(eav/cell->eav "Sheet1" {:cell-ref "A1" :value "test"} :v1)

;; Test ClojureScript EAV conversion  
(require '[clara-xlsx-diff.cljs.eav :as eav])
(eav/cell->eav "Sheet1" {:cell-ref "A1" :value "test"} :v1)
```

**Load and test namespaces:**
```clojure
;; Load test namespace (check for compilation errors)
(require '[clara-xlsx-diff.eav-test :as test])

;; Run specific test functions
(test/eav-record-creation-test)
```

## Building & Testing

### Clojure (JVM) Tests

**Interactive REPL testing (recommended):**
```clojure
;; In Clojure REPL
(require '[clara-xlsx-diff.eav-test :as test] :reload)
(clojure.test/run-tests 'clara-xlsx-diff.eav-test)
```

**Command line testing:**
```bash
clojure -M:test
```

### ClojureScript Tests

**Interactive REPL testing (recommended):**
```clojure
;; In ClojureScript REPL (make sure :test build is running)
(require '[clara-xlsx-diff-cljs.eav-test :as test] :reload)
(cljs.test/run-tests 'clara-xlsx-diff-cljs.eav-test)
```

**Command line testing:**
```bash
npm test
```

### Browser Demo

1. Start the development server:
   ```bash
   npx shadow-cljs watch :dev
   ```

2. Open browser to: http://localhost:8081

## Process Management Best Practices

**⚠️ Important: Always check running processes before starting/stopping**

### Check Running Processes
```bash
# Check shadow-cljs and Java processes
ps aux | grep -E "(shadow|clj|java)" | grep -v grep

# Check what builds are active
npx shadow-cljs clj-eval "(shadow.cljs.devtools.api/active-builds)"

# Check port usage
netstat -tlnp | grep -E "(7002|8080|8081|9630)"
```

### Safe Process Management
```bash
# Stop shadow-cljs cleanly
npx shadow-cljs stop

# Restart with nREPL support
npx shadow-cljs server
```

**Never use `pkill` or forceful termination unless absolutely necessary!**

## Architecture

### EAV Model

Each XLSX cell is transformed into Entity-Attribute-Value triples:

```clojure
;; Cell A1 with value "Name" becomes:
[{:e "v1:Sheet1:A1" :a :cell/sheet :v "Sheet1"}
 {:e "v1:Sheet1:A1" :a :cell/ref :v "A1"}  
 {:e "v1:Sheet1:A1" :a :cell/row :v 0}
 {:e "v1:Sheet1:A1" :a :cell/col :v 0}
 {:e "v1:Sheet1:A1" :a :cell/value :v "Name"}
 {:e "v1:Sheet1:A1" :a :cell/type :v "STRING"}
 {:e "v1:Sheet1:A1" :a :cell/version :v :v1}]
```

### Cross-Platform Support

- **JVM Clojure:** Uses Apache POI for XLSX parsing
- **ClojureScript:** Uses SheetJS for XLSX parsing
- **Unified EAV API:** Same interface for both platforms

## Troubleshooting

### REPL Connection Issues

1. **Check shadow-cljs server is running:**
   ```bash
   npx shadow-cljs info
   ```

2. **Verify nREPL port:**
   ```bash
   lsof -i :7002
   ```

3. **Restart cleanly if needed:**
   ```bash
   npx shadow-cljs stop
   npx shadow-cljs server
   ```

### Infinite Loops in Tests

If you encounter infinite test loops:

1. **Stop shadow-cljs immediately:** `npx shadow-cljs stop`
2. **Check test files** for recursive function calls
3. **Test individual functions in REPL** before running full test suite
4. **Use REPL-driven development** to catch issues early

### Build Failures

1. **Clean shadow-cljs cache:**
   ```bash
   npx shadow-cljs clean
   ```

2. **Restart with fresh dependencies:**
   ```bash
   rm -rf node_modules/.cache
   npm install
   npx shadow-cljs server
   ```

## Contributing

1. **Use REPL-driven development** - test functions interactively before committing
2. **Check processes carefully** before starting/stopping services  
3. **Test both Clojure and ClojureScript** versions of functionality
4. **Update tests incrementally** and validate in REPL first

## License

[Add your license here]

---

## Development Status

✅ **Completed:**
- Basic project structure for Clojure + ClojureScript
- EAV transformation logic for both platforms
- XLSX parsing foundations (Apache POI + SheetJS)
- REPL-driven development workflow
- Safe process management practices

🚧 **In Progress:**
- Clara Rules integration for difference detection
- Comprehensive test coverage
- Browser demo interface

📋 **Planned:**
- CLI interface for file comparison
- Performance optimization
- Advanced comparison rules
