# Clara XLSX Diff - Project Summary

## Overview

This is a complete XLSX comparison tool which identifies and analyzes differences between two versions of the same xlsx file. The library will be used by external users and compiled into either a jar file or javascript package. To organize our analysis of the xlsx files we will first extract the contents of each xlsx, transform that data into EAV triples which can then be loaded into a clara-rules engine using clara-eav.

**Key Innovation**: We will identify if each cell is Modified, New, Deleted or Moved. We will return either an HTML representation of this data or a summary.

## Architecture

### Core Components

1. **clara-rules** (`.clara-rules/README.md`)
   - Powerful forward-chaining rules engine for Clojure, enabling complex logic processing and decision-making
   - Used to analyze XLSX data patterns and determine cell changes

2. **clara-eav** (`.clara-eav/README.md`)
   - Provides a consistent and reusable way of loading and reasoning with complex data structures using EAV triples
   - Transforms XLSX cell data into Entity-Attribute-Value format for rules processing

3. **XLSX Processing**
   - Extract and parse XLSX files into structured data
   - Transform spreadsheet data into EAV triples for rules engine consumption

4. **Joyride** (`.joyride/README.md`)
   - ClojureScript scripting runtime for VS Code, enabling interactive programming and AI-assisted development
   - Used for development workflow and testing


## Key File Paths & Descriptions

### Source Code
- `src/clara_xlsx_diff/` - Main library source code
- `src/clara_xlsx_diff/core.clj` - Core XLSX comparison logic
- `src/clara_xlsx_diff/xlsx.clj` - XLSX file parsing and extraction
- `src/clara_xlsx_diff/eav.clj` - EAV transformation utilities
- `src/clara_xlsx_diff/rules.clj` - Clara rules for diff analysis
- `src/clara_xlsx_diff/output.clj` - HTML and summary output generation

### Configuration
- `shadow-cljs.edn` - ClojureScript build configuration
- `deps.edn` - Clojure dependencies and source paths
- `package.json` - Node.js dependencies (xlsx processing)

### Project Documentation
- `docs/PROJECT_SUMMARY.md` - This comprehensive project overview
- `docs/examples/` - Usage examples and sample outputs
- `docs/api/` - API documentation


## Dependencies & Versions

### Clojure Dependencies
- `com.cerner/clara-rules` - Rules engine for pattern matching and inference
- `clyfe/clara-eav` - EAV integration layer for Clara Rules
- `org.apache.poi/poi` - Java library for Excel file processing
- `org.apache.poi/poi-ooxml` - OOXML format support for Excel files

### Node.js Dependencies (for JS package)
- `xlsx` - Excel file manipulation and parsing

### Development Dependencies
- `shadow-cljs` - ClojureScript compiler and build tool
- `thheller/shadow-cljs` - Development server and hot reload

### VS Code Extensions Required
- **Joyride** - ClojureScript scripting runtime
- **GitHub Copilot** - AI assistant with LM Tools
- **Calva** (recommended) - Clojure REPL integration


## Available APIs & Functions

### Core API
```clojure
;; Main comparison function
(ns clara-xlsx-diff.core)

(defn compare-xlsx
  "Compare two XLSX files and return difference analysis"
  [file1-path file2-path & {:keys [output-format]
                             :or {output-format :summary}}]
  ;; Returns: {:changes [...] :summary {...} :html "..."}
  )

;; XLSX processing
(ns clara-xlsx-diff.xlsx)

(defn extract-data
  "Extract structured data from XLSX file"
  [xlsx-path]
  ;; Returns: structured data representation
  )

;; EAV transformation
(ns clara-xlsx-diff.eav)

(defn xlsx->eav
  "Transform XLSX data into EAV triples"
  [xlsx-data]
  ;; Returns: sequence of [entity attribute value] triples
  )
```

### Change Detection Types
- `:modified` - Cell value changed between versions
- `:new` - Cell exists in new version only  
- `:deleted` - Cell exists in old version only
- `:moved` - Cell content moved to different location

## Implementation Patterns

### EAV-Based Data Model
- Transform XLSX cells into Entity-Attribute-Value triples
- Enable flexible querying and pattern matching with Clara Rules
- Support for complex change detection logic

### Rules-Based Analysis
- Define change detection patterns as Clara rules
- Separate business logic from data processing
- Extensible rule system for different comparison strategies

### Functional Data Processing
- Immutable data structures throughout the pipeline
- Pure functions for data transformation
- Composable processing steps

## Development Workflow

### REPL-Driven Development
1. Start Clojure REPL: `Calva: Start a Project REPL`
2. Start Joyride REPL for VS Code integration: `Calva: Start Joyride REPL and Connect`
3. Evaluate code interactively in `.clj` and `.cljs` files
4. Use `comment` blocks for REPL experiments
5. Test functions immediately with sample XLSX files

### AI-Assisted Development
1. Switch AI mood using system prompts:
   - `architect` - System design and architecture decisions
   - `joyride-hacker` - Interactive development and VS Code automation
   - `reviewer` - Code review and optimization
2. Use project summary for context in AI conversations
3. Iterate on rules and data transformations with immediate feedback

## Technical Architecture Highlights

### XLSX Processing Pipeline
1. **File Parsing** - Extract raw data from XLSX files using Apache POI
2. **Data Normalization** - Convert to standard internal representation
3. **EAV Transformation** - Convert cells to Entity-Attribute-Value triples
4. **Rules Analysis** - Apply Clara rules to detect changes
5. **Output Generation** - Produce HTML reports or summary data

### Change Detection Strategy
- Compare EAV representations of both XLSX versions
- Use Clara rules to identify patterns and classify changes
- Track cell coordinates, values, and metadata
- Generate comprehensive diff reports with visual highlighting

## Usage Examples

### Basic Usage
```clojure
(require '[clara-xlsx-diff.core :as diff])

;; Compare two XLSX files
(def result (diff/compare-xlsx "old-file.xlsx" "new-file.xlsx"))

;; Get summary of changes
(:summary result)
;; => {:modified 15 :new 8 :deleted 3 :moved 2}

;; Get detailed change list
(:changes result)
;; => [{:type :modified :cell "A1" :old-value "Hello" :new-value "Hi"}
;;     {:type :new :cell "B5" :value "New data"}
;;     ...]

;; Generate HTML report
(diff/compare-xlsx "old.xlsx" "new.xlsx" :output-format :html)
```

### AI-Assisted Workflow
Use GitHub Copilot in the appropriate mood:
- **Architect**: "Design the EAV transformation for XLSX cell data"
- **Joyride-hacker**: "Create a VS Code command to run XLSX comparison"
- **Reviewer**: "Review the change detection rules for accuracy"

