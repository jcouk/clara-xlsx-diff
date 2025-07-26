# Clara XLSX Diff

A TypeScript/JavaScript library for comparing XLSX files using Entity-Attribute-Value (EAV) models and Clara Rules engine for intelligent difference detection.

[![npm version](https://badge.fury.io/js/clara-xlsx-diff.svg)](https://badge.fury.io/js/clara-xlsx-diff)
[![License: EPL-2.0](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://opensource.org/licenses/EPL-2.0)

**✨ Features:**
- 📦 **Optimized Bundle**: Self-contained 2.9MB package with simple optimizations
- 🚀 **Zero Runtime Dependencies**: No Google Closure Library files required
- 💾 **Small Package Size**: 338.9 kB compressed download
- 🔷 **Full TypeScript Support**: Complete type definitions and utilities

## Installation

```bash
npm install clara-xlsx-diff
```

> **Note:** This package is designed for Node.js environments (≥14.0.0). For browser usage, you'll need a bundler that can handle Node.js modules.

## Quick Start

```typescript
import { compareXlsxBuffers, init } from 'clara-xlsx-diff';
import * as fs from 'fs';

// Initialize the library
const info = init();
console.log(info); // { version: '0.1.2', description: '...' }

// Compare two Excel files
const file1Buffer = fs.readFileSync('file1.xlsx');
const file2Buffer = fs.readFileSync('file2.xlsx');

const result = compareXlsxBuffers(file1Buffer, file2Buffer, 'file1.xlsx', 'file2.xlsx');

if (result.success) {
  console.log(`Found ${result.cells.length} cell records`);
  console.log(`Summary:`, result.summary);
} else {
  console.error('Comparison failed:', result.error);
}
```

## TypeScript Import Patterns

### 1. Main Package Imports (Recommended)

```typescript
import { 
  // Core functions
  compareXlsxBuffers, 
  init,
  
  // Utility functions
  filterChangesByType,
  groupChangesBySheet,
  getSummaryCount,
  summarizeChanges,
  
  // Types
  ComparisonResult,
  ChangeRecord,
  ChangeType,
  SummaryRecord
} from 'clara-xlsx-diff';
```

### 2. Specific File Imports

```typescript
// Import only types
import type { ChangeRecord, SummaryRecord, ChangeType } from 'clara-xlsx-diff/types';

// Import specific utilities
import { isSuccessfulComparison, getChangeStatistics } from 'clara-xlsx-diff/utils';

// Import example functions
import { compareExcelFiles } from 'clara-xlsx-diff/example-usage';
```

### 3. Namespace Imports

```typescript
// Import everything under a namespace
import * as ClaraXlsx from 'clara-xlsx-diff';

const result = ClaraXlsx.compareXlsxBuffers(buffer1, buffer2);
const changes = ClaraXlsx.filterChangesByType(result.cells, "New");
```

### 4. CommonJS (Node.js without TypeScript)

```javascript
// CommonJS import
const { compareXlsxBuffers, filterChangesByType } = require('clara-xlsx-diff');

// Or import everything
const ClaraXlsx = require('clara-xlsx-diff');
```

## API Reference

### Core Functions

#### `compareXlsxBuffers(file1Buffer, file2Buffer, file1Name?, file2Name?)`
Compare two XLSX files from buffers.

- **Parameters:**
  - `file1Buffer`: Buffer | Uint8Array - First file buffer
  - `file2Buffer`: Buffer | Uint8Array - Second file buffer  
  - `file1Name`: string (optional) - Label for first file
  - `file2Name`: string (optional) - Label for second file
- **Returns:** `ComparisonResult` - Success/failure with comparison data

#### `init()`
Initialize the Clara XLSX library.

- **Returns:** `{ version: string, description: string }`

### Utility Functions

The package also exports utility functions for working with comparison results:

```typescript
import { 
  filterChangesByType, 
  groupChangesBySheet, 
  getSummaryCount,
  summarizeChanges 
} from 'clara-xlsx-diff';

// Filter changes by type
const newCells = filterChangesByType(result.cells, "New");
const changedCells = filterChangesByType(result.cells, "Change");

// Group changes by sheet
const bySheet = groupChangesBySheet(result.cells);

// Get summary statistics
const stats = summarizeChanges(result.summary, result.cells);
```

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
├── shadow-cljs.edn          # ClojureScript build configuration (non-Clara-EAV)
├── project.clj              # Leiningen configuration (Clara-EAV rules)
├── package.json             # NPM dependencies for ClojureScript
├── src/
│   └── clara_xlsx_diff/
│       ├── core.clj         # Main Clojure namespace
│       ├── xlsx.clj         # JVM XLSX parsing (Apache POI)
│       ├── eav.clj          # JVM EAV transformation
│       ├── rules.cljc       # Clara-EAV rules (compile with Leiningen)
│       └── cljs/
│           ├── xlsx.cljs    # ClojureScript XLSX parsing (SheetJS)
│           └── eav.cljs     # ClojureScript EAV transformation
├── test/
│   ├── clara_xlsx_diff/     # Clojure tests
│   ├── clara_xlsx_diff_cljs/ # ClojureScript tests (Shadow CLJS)
│   └── clara_xlsx_diff/
│       └── rules_test.cljc  # Clara-EAV tests (compile with Leiningen)
├── target/                  # Leiningen build output
│   └── test.js             # Compiled Clara-EAV rules
└── public/                  # Static files for browser demo
    └── index.html
```

## Prerequisites

- **Java 17+** (for Clojure)
- **Node.js 18+** (for ClojureScript)
- **Clojure CLI tools** (latest version)
- **Leiningen** (for Clara-EAV rules compilation - see [Build Process](#build-process))
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

**Leiningen (required for Clara-EAV rules):**
```bash
# Install Leiningen if not already installed
# See: https://leiningen.org/
```

## Build Process

### ⚠️ Important: Clara-EAV Compilation Requirements

**Clara-EAV rules require Leiningen for compilation** due to macro expansion order differences between Shadow CLJS and Leiningen's ClojureScript compiler. 

**✅ Works:** Leiningen + ClojureScript  
**❌ Fails:** Shadow CLJS compilation

This is a **fundamental incompatibility** - not a configuration issue that can be fixed. Clara-EAV's macro system requires compile-time namespace resolution that Shadow CLJS cannot provide.

### Building Clara-EAV Rules

Use Leiningen to compile Clara-EAV rules:

```bash
# Compile Clara-EAV rules to JavaScript
lein cljsbuild once test

# Run compiled rules in Node.js
node target/test.js

# Expected output:
# Running Clara-EAV rules tests...
# Test passed! Found 239 cell records
# Test passed! Found 0 output records and 239 cell records
# All tests completed successfully!
```

**Files compiled with Leiningen:**
- `src/clara_xlsx_diff/rules.cljc` - Clara-EAV rule definitions
- `test/clara_xlsx_diff/rules_test.cljc` - Clara-EAV rule tests

### Building Other Components

For non-Clara-EAV components, you can use Shadow CLJS:

```bash
# Build library for Node.js
npx shadow-cljs compile :lib

# Build browser demo
npx shadow-cljs compile :browser

# Build development version
npx shadow-cljs compile :dev

# Build test version (non-Clara-EAV tests only)
npx shadow-cljs compile :test
```

**Files compiled with Shadow CLJS:**
- `src/clara_xlsx_diff/cljs/xlsx.cljs` - XLSX parsing
- `src/clara_xlsx_diff/cljs/eav.cljs` - EAV transformation
- `test/clara_xlsx_diff_cljs/eav_test.cljs` - EAV tests

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

## Testing

### Clara-EAV Rules Testing

**✅ Recommended approach using Leiningen:**

```bash
# 1. Compile Clara-EAV rules
lein cljsbuild once test

# 2. Run the compiled rules
node target/test.js
```

**Output:**
```
Running Clara-EAV rules tests...
Test passed! Found 239 cell records
Test passed! Found 0 output records and 239 cell records
All tests completed successfully!
```

### Standard ClojureScript Tests (Non-Clara-EAV)

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

### Clara-EAV Compilation Issues

**❌ Problem:** Clara-EAV rules fail to compile with Shadow CLJS

```
ERROR: failed to require macro-ns "clara-eav.rules"
Exception: No namespace: your-namespace found
Execution error (ExceptionInfo) at shadow.cljs.devtools.errors/compilation-error
```

**Additional symptoms:**
- `Cannot read properties of undefined (reading 'cljs$core$IFn$_invoke$arity$1')` in browser
- Tests run successfully when compiled with Leiningen but fail with Shadow CLJS
- Macroexpansion errors during compilation

**✅ Solution:** Use Leiningen instead of Shadow CLJS for Clara-EAV compilation

```bash
# ❌ This fails:
npx shadow-cljs compile test

# ✅ This works:
lein cljsbuild once test
node target/test.js
```

**📋 Root Cause:** Clara-EAV macros require a specific macro expansion order that Leiningen provides but Shadow CLJS does not. This is a fundamental incompatibility between Clara-EAV's macro system and Shadow CLJS's compilation strategy.

**🔧 Dual Build System:** 
- Use **Leiningen for Clara-EAV rules compilation** (`project.clj`)
- Use **Shadow CLJS for other ClojureScript code** (`shadow-cljs.edn`)
- Both build configurations are provided in this project

**⚠️ What doesn't work:**
- `:require-macros` fixes
- Namespace reorganization
- Build configuration changes
- Shadow CLJS advanced compilation settings

**✅ Validated solution:**
1. `lein cljsbuild once test` - compiles successfully
2. `node target/test.js` - runs Clara-EAV rules without errors
3. All Clara-EAV sessions, rules, and queries work correctly

### REPL Development vs Production Builds

**REPL-based development:** Use Shadow CLJS for interactive development, but avoid loading Clara-EAV rules namespaces in the ClojureScript REPL.

**Production builds:** Always use Leiningen for any code that includes Clara-EAV rules.

### REPL Connection Issues

1. **Check shadow-cljs server is running:**
   ```bash
   npx shadow-cljs info
   ```

2. **Verify nREPL port:**
   ```bash
   # Should show nREPL on port 7002
   npx shadow-cljs server
   ```

3. **VS Code connection:**
   - Use `localhost:7002` for connection
   - Select `shadow-cljs` project type
   - Choose appropriate build (`:dev`, `:test`, etc.)

### Build Issues

**ClojureScript compilation errors:**
```bash
# Clean compiled files
npx shadow-cljs clean

# Restart with fresh compilation
npx shadow-cljs watch :dev
```

**Clojure dependency issues:**
```bash
# Refresh dependencies
clojure -P
```

**Node.js module issues:**
```bash
# Clean npm cache and reinstall
rm -rf node_modules package-lock.json
npm install
```
## Contributing

1. **Use REPL-driven development** - test functions interactively before committing
2. **Check processes carefully** before starting/stopping services  
3. **Test both Clojure and ClojureScript** versions of functionality
4. **Update tests incrementally** and validate in REPL first

## License

[Add your license here]

---

## Usage

### Buffer-Based Comparison (Recommended for Git Workflows)

The library provides buffer-based comparison functions that are ideal for git workflows, file uploads, and scenarios where you have file data in memory:

```javascript
const claraXlsx = require('clara-xlsx-diff');

// Compare two buffers directly
const result = claraXlsx.compareXlsxBuffers(
  buffer1,           // Uint8Array or Buffer
  buffer2,           // Uint8Array or Buffer  
  'file1.xlsx',      // Optional label
  'file2.xlsx'       // Optional label
);

// Git-specific helper
const gitResult = claraXlsx.compareGitVersions(
  currentBuffer,
  previousBuffer,
  'current',
  'previous'
);
```

### File Path Comparison

For traditional file-based comparison:

```javascript
const result = claraXlsx.compareXlsxFiles('path/to/file1.xlsx', 'path/to/file2.xlsx');
```

### Git Integration Examples

**Compare working directory with HEAD:**
```javascript
const { execSync } = require('child_process');
const fs = require('fs');

const currentBuffer = fs.readFileSync('data.xlsx');
const headBuffer = execSync('git show HEAD:data.xlsx');

const result = claraXlsx.compareXlsxBuffers(currentBuffer, headBuffer);
```

**Compare two commits:**
```javascript
const commit1Buffer = execSync('git show commit1:data.xlsx');
const commit2Buffer = execSync('git show commit2:data.xlsx');

const result = claraXlsx.compareGitVersions(commit2Buffer, commit1Buffer, 'commit2', 'commit1');
```

### Response Format

All comparison functions return a consistent JavaScript object:

```javascript
{
  success: true,
  file1: "file1.xlsx",
  file2: "file2.xlsx", 
  summary: {
    totalCells1: 150,
    totalCells2: 155,
    sheetsCompared: 3,
    changesFound: 12
  },
  changes: [
    {
      entity: "v1:Sheet1:A1",
      attribute: "cell/value", 
      oldValue: "Name",
      newValue: "Full Name",
      changeType: "MODIFIED"
    }
    // ... more changes
  ],
  data1: { /* XLSX data structure */ },
  data2: { /* XLSX data structure */ }
}
```
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

---

## Summary of Clara-EAV Integration

This project successfully integrates Clara-EAV with ClojureScript, but requires a **dual build system** approach:

**Key Findings:**
1. **Clara-EAV + Shadow CLJS = Incompatible** - Macro expansion order issues cannot be resolved
2. **Clara-EAV + Leiningen ClojureScript = Works perfectly** - All rules, sessions, and queries function correctly
3. **Solution:** Use both build systems side-by-side for different purposes

**Validated Workflow:**
- Clara-EAV rules and tests: `lein cljsbuild once test && node target/test.js` ✅
- Other ClojureScript code: `npx shadow-cljs compile :lib` ✅  
- Interactive development: Shadow CLJS REPL (avoiding Clara-EAV namespaces) ✅

**Project Impact:**
- Clara-EAV rules successfully process XLSX data and generate EAV triples
- Rule-based difference detection works as designed
- Cross-platform compatibility maintained (Clojure JVM + ClojureScript)
- REPL-driven development workflow preserved for non-Clara-EAV code
