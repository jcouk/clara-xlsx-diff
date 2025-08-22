"use strict";
// Utility functions for clara-xlsx-diff (implementations for the types)
Object.defineProperty(exports, "__esModule", { value: true });
exports.isSuccessfulComparison = isSuccessfulComparison;
exports.isFailedComparison = isFailedComparison;
exports.filterChangesByType = filterChangesByType;
exports.getChangesForSheet = getChangesForSheet;
exports.groupChangesBySheet = groupChangesBySheet;
exports.getSummaryForSheet = getSummaryForSheet;
exports.getSummaryCount = getSummaryCount;
exports.getTotalCountByType = getTotalCountByType;
exports.summarizeChanges = summarizeChanges;
exports.getChangeIcon = getChangeIcon;
exports.findFormulaChanges = findFormulaChanges;
exports.findSignificantNumericChanges = findSignificantNumericChanges;
exports.getChangeStatistics = getChangeStatistics;
exports.getSheetNames = getSheetNames;
exports.getSheetDimensions = getSheetDimensions;
exports.getVersionResults = getVersionResults;
exports.hasChanges = hasChanges;
exports.getSheetsWithChanges = getSheetsWithChanges;
/**
 * Type guard to check if comparison was successful
 */
function isSuccessfulComparison(result) {
    return result.success === true;
}
/**
 * Type guard to check if comparison failed
 */
function isFailedComparison(result) {
    return result.success === false;
}
/**
 * Helper to filter changes by type
 */
function filterChangesByType(changes, changeType) {
    return changes.filter(function (change) { return change.changeType === changeType; });
}
/**
 * Helper to get changes for a specific sheet
 */
function getChangesForSheet(changes, sheetName) {
    return changes.filter(function (change) { return change.sheet === sheetName; });
}
/**
 * Helper to group changes by sheet
 */
function groupChangesBySheet(changes) {
    return changes.reduce(function (acc, change) {
        var sheet = change.sheet;
        if (!acc[sheet]) {
            acc[sheet] = [];
        }
        acc[sheet].push(change);
        return acc;
    }, {});
}
/**
 * Helper to filter summary records by sheet
 */
function getSummaryForSheet(summary, sheetName) {
    return summary[sheetName];
}
/**
 * Helper to get summary count for a specific sheet/version/changeType
 */
function getSummaryCount(summary, sheet, version, changeType) {
    var sheetData = summary[sheet];
    if (!sheetData)
        return 0;
    var versionResults = version === "v1" ? sheetData.v1Results : sheetData.v2Results;
    return versionResults[changeType] || 0;
}
/**
 * Helper to get total counts across all sheets for a changeType
 */
function getTotalCountByType(summary, changeType) {
    return Object.values(summary).reduce(function (total, sheetData) {
        return total + (sheetData.v1Results[changeType] || 0) + (sheetData.v2Results[changeType] || 0);
    }, 0);
}
/**
 * Convert Clara-EAV summary to traditional summary statistics
 */
function summarizeChanges(summary, cells) {
    var sheets = Object.keys(summary);
    return {
        totalCells: cells.length,
        sheetsCompared: sheets.length,
        totalNew: getTotalCountByType(summary, "New"),
        totalDeleted: getTotalCountByType(summary, "Deleted"),
        totalChanged: getTotalCountByType(summary, "Change"),
        totalUnchanged: getTotalCountByType(summary, "None"),
        bySheet: sheets.reduce(function (acc, sheet) {
            var sheetData = summary[sheet];
            acc[sheet] = {
                new: (sheetData.v1Results.New || 0) + (sheetData.v2Results.New || 0),
                deleted: (sheetData.v1Results.Deleted || 0) + (sheetData.v2Results.Deleted || 0),
                changed: (sheetData.v1Results.Change || 0) + (sheetData.v2Results.Change || 0),
                unchanged: (sheetData.v1Results.None || 0) + (sheetData.v2Results.None || 0)
            };
            return acc;
        }, {})
    };
}
/**
 * Get icon for change type
 */
function getChangeIcon(changeType) {
    var icons = {
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
function findFormulaChanges(changes) {
    return changes.filter(function (change) {
        return typeof change.value === 'string' &&
            change.value.startsWith('=');
    });
}
/**
 * Find significant numeric changes (>threshold% difference)
 */
function findSignificantNumericChanges(changes, threshold) {
    if (threshold === void 0) { threshold = 0.1; }
    return changes.filter(function (change) {
        var oldVal = change.matchValue;
        var newVal = change.value;
        if (typeof oldVal === 'number' && typeof newVal === 'number' && oldVal !== 0) {
            var percentChange = Math.abs((newVal - oldVal) / oldVal);
            return percentChange > threshold;
        }
        return false;
    });
}
/**
 * Get summary statistics from changes
 */
function getChangeStatistics(cells) {
    var stats = {
        total: cells.length,
        new: 0,
        deleted: 0,
        changed: 0,
        unchanged: 0,
        bySheet: {}
    };
    cells.forEach(function (change) {
        var changeType = change.changeType;
        var sheet = change.sheet;
        // Count by type
        switch (changeType) {
            case "New":
                stats.new++;
                break;
            case "Deleted":
                stats.deleted++;
                break;
            case "Change":
                stats.changed++;
                break;
            case "None":
                stats.unchanged++;
                break;
        }
        // Count by sheet
        stats.bySheet[sheet] = (stats.bySheet[sheet] || 0) + 1;
    });
    return stats;
}
/**
 * Get all sheet names from summary
 */
function getSheetNames(summary) {
    return Object.keys(summary);
}
/**
 * Get sheet dimensions from summary
 */
function getSheetDimensions(summary, sheetName) {
    var sheetData = summary[sheetName];
    if (!sheetData)
        return null;
    return {
        maxRow: sheetData.maxRow,
        maxCol: sheetData.maxCol
    };
}
/**
 * Get version-specific results for a sheet
 */
function getVersionResults(summary, sheetName, version) {
    var sheetData = summary[sheetName];
    if (!sheetData)
        return null;
    return version === "v1" ? sheetData.v1Results : sheetData.v2Results;
}
/**
 * Check if a sheet has any changes
 */
function hasChanges(summary, sheetName) {
    var sheetData = summary[sheetName];
    if (!sheetData)
        return false;
    var v1Changes = (sheetData.v1Results.New || 0) + (sheetData.v1Results.Change || 0) + (sheetData.v1Results.Deleted || 0);
    var v2Changes = (sheetData.v2Results.New || 0) + (sheetData.v2Results.Change || 0) + (sheetData.v2Results.Deleted || 0);
    return v1Changes > 0 || v2Changes > 0;
}
/**
 * Get sheets with changes only
 */
function getSheetsWithChanges(summary) {
    return Object.keys(summary).filter(function (sheetName) { return hasChanges(summary, sheetName); });
}
