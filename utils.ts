// Utility functions for clara-xlsx-diff (implementations for the types)

import { 
  ComparisonResult, 
  SuccessfulComparison, 
  FailedComparison, 
  ChangeRecord, 
  ChangeType,
  SummaryRecord
} from './types';

/**
 * Type guard to check if comparison was successful
 */
export function isSuccessfulComparison(result: ComparisonResult): result is SuccessfulComparison {
  return result.success === true;
}

/**
 * Type guard to check if comparison failed
 */
export function isFailedComparison(result: ComparisonResult): result is FailedComparison {
  return result.success === false;
}

/**
 * Helper to filter changes by type
 */
export function filterChangesByType(changes: ChangeRecord[], changeType: ChangeType): ChangeRecord[] {
  return changes.filter(change => change.changeType === changeType);
}

/**
 * Helper to get changes for a specific sheet
 */
export function getChangesForSheet(changes: ChangeRecord[], sheetName: string): ChangeRecord[] {
  return changes.filter(change => change.sheet === sheetName);
}

/**
 * Helper to group changes by sheet
 */
export function groupChangesBySheet(changes: ChangeRecord[]): Record<string, ChangeRecord[]> {
  return changes.reduce((acc, change) => {
    const sheet = change.sheet;
    if (!acc[sheet]) {
      acc[sheet] = [];
    }
    acc[sheet].push(change);
    return acc;
  }, {} as Record<string, ChangeRecord[]>);
}

/**
 * Helper to filter summary records by sheet
 */
export function getSummaryForSheet(summary: SummaryRecord, sheetName: string): SummaryRecord[string] | undefined {
  return summary[sheetName];
}

/**
 * Helper to get summary count for a specific sheet/version/changeType
 */
export function getSummaryCount(
  summary: SummaryRecord, 
  sheet: string, 
  version: "v1" | "v2", 
  changeType: ChangeType
): number {
  const sheetData = summary[sheet];
  if (!sheetData) return 0;
  
  const versionResults = version === "v1" ? sheetData.v1Results : sheetData.v2Results;
  return versionResults[changeType] || 0;
}

/**
 * Helper to get total counts across all sheets for a changeType
 */
export function getTotalCountByType(summary: SummaryRecord, changeType: ChangeType): number {
  return Object.values(summary).reduce((total, sheetData) => {
    return total + (sheetData.v1Results[changeType] || 0) + (sheetData.v2Results[changeType] || 0);
  }, 0);
}

/**
 * Convert Clara-EAV summary to traditional summary statistics
 */
export function summarizeChanges(summary: SummaryRecord, cells: ChangeRecord[]) {
  const sheets = Object.keys(summary);
  
  return {
    totalCells: cells.length,
    sheetsCompared: sheets.length,
    totalNew: getTotalCountByType(summary, "New"),
    totalDeleted: getTotalCountByType(summary, "Deleted"), 
    totalChanged: getTotalCountByType(summary, "Change"),
    totalUnchanged: getTotalCountByType(summary, "None"),
    bySheet: sheets.reduce((acc, sheet) => {
      const sheetData = summary[sheet];
      acc[sheet] = {
        new: (sheetData.v1Results.New || 0) + (sheetData.v2Results.New || 0),
        deleted: (sheetData.v1Results.Deleted || 0) + (sheetData.v2Results.Deleted || 0),
        changed: (sheetData.v1Results.Change || 0) + (sheetData.v2Results.Change || 0),
        unchanged: (sheetData.v1Results.None || 0) + (sheetData.v2Results.None || 0)
      };
      return acc;
    }, {} as Record<string, {new: number, deleted: number, changed: number, unchanged: number}>)
  };
}

/**
 * Get icon for change type
 */
export function getChangeIcon(changeType: ChangeType): string {
  const icons: Record<ChangeType, string> = {
    "New": "➕",
    "Deleted": "❌", 
    "Change": "🔄",
    "None": "✅"
  };
  return icons[changeType] || "❓";
}

/**
 * Find all formula changes
 */
export function findFormulaChanges(changes: ChangeRecord[]): ChangeRecord[] {
  return changes.filter(change => 
    typeof change.value === 'string' && 
    change.value.startsWith('=')
  );
}

/**
 * Find significant numeric changes (>threshold% difference)
 */
export function findSignificantNumericChanges(changes: ChangeRecord[], threshold: number = 0.1): ChangeRecord[] {
  return changes.filter(change => {
    const oldVal = change.matchValue;
    const newVal = change.value;
    
    if (typeof oldVal === 'number' && typeof newVal === 'number' && oldVal !== 0) {
      const percentChange = Math.abs((newVal - oldVal) / oldVal);
      return percentChange > threshold;
    }
    return false;
  });
}

/**
 * Get summary statistics from changes
 */
export function getChangeStatistics(cells: ChangeRecord[]) {
  const stats = {
    total: cells.length,
    new: 0,
    deleted: 0,
    changed: 0,
    unchanged: 0,
    bySheet: {} as Record<string, number>
  };

  cells.forEach(change => {
    const changeType = change.changeType;
    const sheet = change.sheet;

    // Count by type
    switch (changeType) {
      case "New": stats.new++; break;
      case "Deleted": stats.deleted++; break;
      case "Change": stats.changed++; break;
      case "None": stats.unchanged++; break;
    }

    // Count by sheet
    stats.bySheet[sheet] = (stats.bySheet[sheet] || 0) + 1;
  });

  return stats;
}

/**
 * Get all sheet names from summary
 */
export function getSheetNames(summary: SummaryRecord): string[] {
  return Object.keys(summary);
}

/**
 * Get sheet dimensions from summary
 */
export function getSheetDimensions(summary: SummaryRecord, sheetName: string): {maxRow: number, maxCol: number} | null {
  const sheetData = summary[sheetName];
  if (!sheetData) return null;
  
  return {
    maxRow: sheetData.maxRow,
    maxCol: sheetData.maxCol
  };
}

/**
 * Get version-specific results for a sheet
 */
export function getVersionResults(summary: SummaryRecord, sheetName: string, version: "v1" | "v2"): Record<ChangeType, number> | null {
  const sheetData = summary[sheetName];
  if (!sheetData) return null;
  
  return version === "v1" ? sheetData.v1Results : sheetData.v2Results;
}

/**
 * Check if a sheet has any changes
 */
export function hasChanges(summary: SummaryRecord, sheetName: string): boolean {
  const sheetData = summary[sheetName];
  if (!sheetData) return false;
  
  const v1Changes = (sheetData.v1Results.New || 0) + (sheetData.v1Results.Change || 0) + (sheetData.v1Results.Deleted || 0);
  const v2Changes = (sheetData.v2Results.New || 0) + (sheetData.v2Results.Change || 0) + (sheetData.v2Results.Deleted || 0);
  
  return v1Changes > 0 || v2Changes > 0;
}

/**
 * Get sheets with changes only
 */
export function getSheetsWithChanges(summary: SummaryRecord): string[] {
  return Object.keys(summary).filter(sheetName => hasChanges(summary, sheetName));
}
