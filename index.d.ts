// Type declarations for clara-xlsx-diff main exports

// Re-export all types
export * from './types';

// Re-export utility functions (declarations only, implementations in utils.ts)
export { 
  isSuccessfulComparison,
  isFailedComparison,
  filterChangesByType,
  getChangesForSheet,
  groupChangesBySheet,
  getSummaryForSheet,
  getSummaryCount,
  getTotalCountByType,
  summarizeChanges,
  getChangeIcon,
  findFormulaChanges,
  findSignificantNumericChanges,
  getChangeStatistics,
  getSheetNames,
  getSheetDimensions,
  getSheetsWithChanges,
  hasChanges,
  getVersionResults
} from './utils';

import { ComparisonResult } from './types';

/**
 * Compare two XLSX files from buffers
 */
export function compareXlsxBuffers(
  file1Buffer: Buffer | Uint8Array,
  file2Buffer: Buffer | Uint8Array,
  file1Name?: string,
  file2Name?: string
): ComparisonResult;

/**
 * Initialize the Clara XLSX library
 */
export function init(): {
  description: string;
  version: string;
};

/**
 * Compare two Excel files by file path
 */
export function compareExcelFiles(file1Path: string, file2Path: string): Promise<void>;
