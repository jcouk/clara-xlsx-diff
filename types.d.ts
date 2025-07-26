// TypeScript definitions for clara-xlsx-diff library

/// <reference types="node" />

/**
 * The version of a cell for comparison (v1 = original, v2 = new)
 */
export type CellVersion = "v1" | "v2";

/**
 * The type of change detected in a cell
 */
export type ChangeType = "New" | "Deleted" | "Change" | "None";

/**
 * Cell data type as detected from XLSX
 */
export type CellType = "STRING" | "NUMERIC" | "BOOLEAN" | "FORMULA" | "BLANK";

/**
 * Row/column pair for position tracking
 */
export interface RowColPair {
  row: number;
  col: number;
}

/**
 * Hyperlink information from a cell
 */
export interface CellHyperlink {
  address: string;
  label?: string;
  type?: string;
}

/**
 * Cell comment information
 */
export interface CellComment {
  text: string;
  author?: string;
  visible?: boolean;
}

/**
 * Font styling information
 */
export interface FontInfo {
  fontName?: string;
  fontSize?: number;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strikeout?: boolean;
  color?: string;
}

/**
 * Cell style information
 */
export interface CellStyle {
  alignment?: string;
  verticalAlignment?: string;
  wrapText?: boolean;
  indention?: number;
  rotation?: number;
  fillPattern?: string;
  fillForegroundColor?: string;
  fillBackgroundColor?: string;
  borderTop?: string;
  borderRight?: string;
  borderBottom?: string;
  borderLeft?: string;
  borderTopColor?: string;
  borderRightColor?: string;
  borderBottomColor?: string;
  borderLeftColor?: string;
  dataFormat?: number;
  dataFormatString?: string;
  hidden?: boolean;
  locked?: boolean;
}

/**
 * Individual cell data from XLSX extraction
 */
export interface XlsxCell {
  cellRef: string;           // e.g., "A1", "B5"
  row: number;               // 0-based row index
  col: number;               // 0-based column index
  value: any;                // Cell value (string, number, boolean, etc.)
  type: CellType;            // Cell data type
  formattedText?: string;    // Formatted display text
  formula?: string;          // Formula (if any)
  styleIndex?: number;       // Style index
  numberFormat?: string;     // Number format
  hyperlink?: CellHyperlink; // Hyperlink info
  comment?: CellComment;     // Cell comment
  html?: string;             // HTML representation
  style?: CellStyle;         // Comprehensive style info (JVM only)
  font?: FontInfo;           // Font info (JVM only)
}

/**
 * Worksheet data
 */
export interface XlsxSheet {
  sheetName: string;
  cells: XlsxCell[];
}

/**
 * Complete XLSX file data
 */
export interface XlsxData {
  filePath: string;
  sheets: XlsxSheet[];
  sheetCount: number;
  totalCells: number;
}

/**
 * Clara-EAV change record output
 */
export interface ChangeRecord {
  row: number;                    // Row index
  eid: number;                    // Entity ID from Clara-EAV
  sheetAndVersionIdentif: string; // Sheet and version identifier
  ref: string;                    // Cell reference (e.g., "A1")
  sheet: string;                  // Sheet name
  value: any;                     // Current cell value
  col: number;                    // Column index
  changeType: ChangeType;         // Type of change
  matchValue: any;               // Matching value from other version
  version: CellVersion;          // Which version this record is from
}

/**
 * Clara-EAV summary record output
 * 
//  * Example Summary Record
{:Decision_Table
 {:sheetName "Decision_Table",
  :maxCol 3,
  :maxRow 7,
  :v1Results {:None 32, :Change 0, :New 0},
  :v2Results {:None 32, :Change 0, :Deleted 0}},
 :Rules
 {:sheetName "Rules",
  :maxCol 13,
  :maxRow 9,
  :v1Results {:None 0, :Change 16, :New 9},
  :v2Results {:None 0, :Change 16, :Deleted 21}},
 :Metrics
 {:sheetName "Metrics",
  :maxCol 3,
  :maxRow 5,
  :v1Results {:None 24, :Change 0, :New 0},
  :v2Results {:None 24, :Change 0, :Deleted 0}},
 :Employees
 {:sheetName "Employees",
  :maxCol 5,
  :maxRow 5,
  :v1Results {:None 36, :Change 0, :New 0},
  :v2Results {:None 36, :Change 0, :Deleted 0}}}
 */
export interface SummaryRecord {
[key: string]: {
    sheetName: string;          // Name of the sheet
    maxCol: number;            // Maximum column index
    maxRow: number;            // Maximum row index
    v1Results: Record<ChangeType, number>; // Change counts for version 1
    v2Results: Record<ChangeType, number>; // Change counts for version 2
  }
}

/**
 * Successful comparison result
 */
export interface SuccessfulComparison {
  success: true;
  file1: string;                    // File 1 name/identifier
  file2: string;                    // File 2 name/identifier
  summary: SummaryRecord;           // Summary record organized by sheet
  cells: ChangeRecord[];            // Array of detected changes (renamed from 'changes')
}

/**
 * Failed comparison result
 */
export interface FailedComparison {
  success: false;
  error: string;                    // Error message
  file1: string;                    // File 1 name/identifier
  file2: string;                    // File 2 name/identifier
}

/**
 * Union type for comparison results
 */
export type ComparisonResult = SuccessfulComparison | FailedComparison;

/**
 * Library initialization result
 */
export interface InitResult {
  version: string;
  description: string;
}

/**
 * Main library interface
 */
export interface ClaraXlsxDiff {
  /**
   * Compare two XLSX files from buffers
   * @param file1Buffer - Buffer or Uint8Array of first XLSX file
   * @param file2Buffer - Buffer or Uint8Array of second XLSX file
   * @param file1Name - Optional name/identifier for first file
   * @param file2Name - Optional name/identifier for second file
   * @returns Promise-like result with comparison data
   */
  compareXlsxBuffers(
    file1Buffer: Buffer | Uint8Array,
    file2Buffer: Buffer | Uint8Array,
    file1Name?: string,
    file2Name?: string
  ): ComparisonResult;

  /**
   * Initialize the library
   * @returns Library information
   */
  init(): InitResult;
}

/**
 * Type guard to check if comparison was successful
 */
export declare function isSuccessfulComparison(result: ComparisonResult): result is SuccessfulComparison;

/**
 * Type guard to check if comparison failed
 */
export declare function isFailedComparison(result: ComparisonResult): result is FailedComparison;

/**
 * Helper to filter changes by type
 */
export declare function filterChangesByType(changes: ChangeRecord[], changeType: ChangeType): ChangeRecord[];

/**
 * Helper to get changes for a specific sheet
 */
export declare function getChangesForSheet(changes: ChangeRecord[], sheetName: string): ChangeRecord[];

/**
 * Helper to group changes by sheet
 */
export declare function groupChangesBySheet(changes: ChangeRecord[]): Record<string, ChangeRecord[]>;

/**
 * Helper to filter summary records by sheet
 */
export declare function getSummaryForSheet(summary: SummaryRecord, sheetName: string): SummaryRecord[string] | undefined;

/**
 * Helper to get summary count for a specific sheet/version/changeType
 */
export declare function getSummaryCount(summary: SummaryRecord, sheet: string, version: "v1" | "v2", changeType: ChangeType): number;

/**
 * Helper to get total counts across all sheets for a changeType
 */
export declare function getTotalCountByType(summary: SummaryRecord, changeType: ChangeType): number;

/**
 * Convert Clara-EAV summary to traditional summary statistics
 */
export declare function summarizeChanges(summary: SummaryRecord, cells: ChangeRecord[]): {
  totalCells: number;
  sheetsCompared: number;
  totalNew: number;
  totalDeleted: number;
  totalChanged: number;
  totalUnchanged: number;
  bySheet: Record<string, {new: number, deleted: number, changed: number, unchanged: number}>;
};

/**
 * Get icon for change type
 */
export declare function getChangeIcon(changeType: ChangeType): string;

/**
 * Find all formula changes
 */
export declare function findFormulaChanges(changes: ChangeRecord[]): ChangeRecord[];

/**
 * Find significant numeric changes (>threshold% difference)
 */
export declare function findSignificantNumericChanges(changes: ChangeRecord[], threshold?: number): ChangeRecord[];

/**
 * Get summary statistics from changes
 */
export declare function getChangeStatistics(cells: ChangeRecord[]): {
  total: number;
  new: number;
  deleted: number;
  changed: number;
  unchanged: number;
  bySheet: Record<string, number>;
};

/**
 * Get all sheet names from summary
 */
export declare function getSheetNames(summary: SummaryRecord): string[];

/**
 * Get sheet dimensions from summary
 */
export declare function getSheetDimensions(summary: SummaryRecord, sheetName: string): {maxRow: number, maxCol: number} | null;

/**
 * Get version-specific results for a sheet
 */
export declare function getVersionResults(summary: SummaryRecord, sheetName: string, version: "v1" | "v2"): Record<ChangeType, number> | null;

/**
 * Check if a sheet has any changes
 */
export declare function hasChanges(summary: SummaryRecord, sheetName: string): boolean;

/**
 * Get sheets with changes only
 */
export declare function getSheetsWithChanges(summary: SummaryRecord): string[];

// Default export for CommonJS compatibility
declare const claraXlsxDiff: ClaraXlsxDiff;
export default claraXlsxDiff;
