// Example usage of clara-xlsx-diff with TypeScript
// npx tsc example-usage.ts && node example-usage.js
import { 
  ClaraXlsxDiff, 
  ComparisonResult, 
  ChangeRecord, 
  ChangeType,
  SummaryRecord
} from './types';

import {
  isSuccessfulComparison,
  filterChangesByType,
  groupChangesBySheet,
  getChangeIcon,
  findFormulaChanges,
  findSignificantNumericChanges,
  getChangeStatistics,
  getSummaryForSheet,
  getSummaryCount,
  getTotalCountByType,
  summarizeChanges
} from './utils';

const fs = require('fs');

// Load the compiled ClojureScript library
require('./target/main.js');

// Access the ClojureScript namespace
const claraXlsxNs = (global as any).clara_xlsx_diff?.main;

// Create a wrapper that matches our TypeScript interface
const claraXlsx: ClaraXlsxDiff = {
  compareXlsxBuffers: claraXlsxNs?.compare_xlsx_buffers || (() => { throw new Error('Library not loaded'); }),
  init: claraXlsxNs?.init || (() => { throw new Error('Library not loaded'); })
};

/**
 * Example function showing typed usage of clara-xlsx-diff
 */
async function compareExcelFiles(file1Path: string, file2Path: string): Promise<void> {
  try {
    // Read files as buffers
    const file1Buffer = fs.readFileSync(file1Path);
    const file2Buffer = fs.readFileSync(file2Path);
    
    // Initialize library (optional)
    const initResult = claraXlsx.init();
    console.log(`📚 Library: ${initResult.description} v${initResult.version}`);
    
    // Compare files
    console.log(`🔍 Comparing ${file1Path} vs ${file2Path}...`);
    const result: ComparisonResult = claraXlsx.compareXlsxBuffers(
      file1Buffer,
      file2Buffer,
      file1Path,
      file2Path
    );
    
    // Type-safe result handling
    if (isSuccessfulComparison(result)) {
      console.log('✅ Comparison successful!', result.summary);
      
      // Access summary with full type safety
      const { summary, cells } = result;
      
      // Debug: Check what we actually received
      console.log('🔍 Debug - Summary type:', typeof summary);
      console.log('🔍 Debug - Summary is array:', Array.isArray(summary));
      console.log('🔍 Debug - Summary keys:', Object.keys(summary));
      console.log('🔍 Debug - Summary sample:', JSON.stringify(summary).substring(0, 200) + '...');
      console.log('🔍 Debug - Cells sample:', JSON.stringify(cells.slice(0, 2), null, 2));
      console.log('🔍 Debug - Cells count:', cells.length);
      
      // Generate traditional summary statistics from Clara-EAV data
      const stats = summarizeChanges(summary, cells);
      
      console.log(`📊 Summary:
        - Total cells analyzed: ${stats.totalCells}
        - Sheets compared: ${stats.sheetsCompared}
        - Changes found: ${stats.totalNew + stats.totalDeleted + stats.totalChanged}
      `);
      
      // Filter changes by type with helper functions
      const newCells = filterChangesByType(cells, "New");
      const deletedCells = filterChangesByType(cells, "Deleted");
      const changedCells = filterChangesByType(cells, "Change");
      const unchangedCells = filterChangesByType(cells, "None");
      
      console.log(`📈 Change breakdown:
        - ➕ New: ${newCells.length} (summary total: ${stats.totalNew})
        - ❌ Deleted: ${deletedCells.length} (summary total: ${stats.totalDeleted})
        - 🔄 Changed: ${changedCells.length} (summary total: ${stats.totalChanged})
        - ✅ Unchanged: ${unchangedCells.length} (summary total: ${stats.totalUnchanged})
      `);
      
      // Get distinct sheets from summary
      // the summary results will return an object, each key will be a sheet name
      console.log(`\n📄 Distinct sheets: ${Object.keys(summary).length}`);
      const sheetSet = new Set(Object.keys(summary));
      const distinctSheets = Array.from(sheetSet);
      
      console.log('\n📋 Changes by sheet:');
      distinctSheets.forEach(sheetName => {
        console.log(`\n  📄 ${sheetName}:`);
        
        // Show v1 changes
        console.log(`    📝 v1 (Original):`);
        const v1New = getSummaryCount(summary, sheetName, "v1", "New");
        const v1Changed = getSummaryCount(summary, sheetName, "v1", "Change");
        const v1Deleted = getSummaryCount(summary, sheetName, "v1", "Deleted");
        const v1Unchanged = getSummaryCount(summary, sheetName, "v1", "None");
        
        console.log(`      ➕ New: ${v1New}`);
        console.log(`      🔄 Changed: ${v1Changed}`);
        console.log(`      ❌ Deleted: ${v1Deleted}`);
        console.log(`      ✅ Unchanged: ${v1Unchanged}`);
        
        // Show v2 changes
        console.log(`    📝 v2 (New):`);
        const v2New = getSummaryCount(summary, sheetName, "v2", "New");
        const v2Changed = getSummaryCount(summary, sheetName, "v2", "Change");
        const v2Deleted = getSummaryCount(summary, sheetName, "v2", "Deleted");
        const v2Unchanged = getSummaryCount(summary, sheetName, "v2", "None");
        
        console.log(`      ➕ New: ${v2New}`);
        console.log(`      🔄 Changed: ${v2Changed}`);
        console.log(`      ❌ Deleted: ${v2Deleted}`);
        console.log(`      ✅ Unchanged: ${v2Unchanged}`);
        
        // Show total cell changes for this sheet
        const sheetCells = cells.filter(cell => cell.sheet === sheetName);
        console.log(`    📊 Total cell records: ${sheetCells.length}`);
        
        // Show a few sample changes if any exist
        const significantChanges = sheetCells.filter(cell => 
          cell.changeType === "New" || cell.changeType === "Change" || cell.changeType === "Deleted"
        );
        
        if (significantChanges.length > 0) {
          console.log(`    🔍 Sample changes:`);
          significantChanges.slice(0, 3).forEach((change: ChangeRecord) => {
            const { ref, changeType, value, version } = change;
            console.log(`      ${getChangeIcon(changeType)} ${ref} (${version}): ${value}`);
          });
          
          if (significantChanges.length > 3) {
            console.log(`      ... and ${significantChanges.length - 3} more`);
          }
        }
      });
      
      // Show Clara-EAV summary records
      console.log('\n📊 Clara-EAV Summary Records:');
      Object.entries(summary).forEach(([sheetName, sheetData]) => {
        console.log(`  📄 ${sheetName}:`);
        console.log(`    Dimensions: ${sheetData.maxCol + 1} cols × ${sheetData.maxRow + 1} rows`);
        console.log(`    v1 Results: ${JSON.stringify(sheetData.v1Results)}`);
        console.log(`    v2 Results: ${JSON.stringify(sheetData.v2Results)}`);
      });
      
      // Detailed analysis of significant changes
      if (changedCells.length > 0) {
        console.log('\n🔍 Detailed changes:');
        changedCells.slice(0, 5).forEach((change: ChangeRecord) => {
          console.log(`  🔄 ${change.sheet}!${change.ref}: 
            ${change.matchValue} → ${change.value}`);
        });
      }
      
    } else {
      // Handle failed comparison
      console.error('❌ Comparison failed:', result.error);
      console.error(`Files: ${result.file1} vs ${result.file2}`);
    }
    
  } catch (error) {
    console.error('💥 Unexpected error:', error);
  }
}

/**
 * Example usage
 */
if (require.main === module) {
  // Run comparison if this file is executed directly
  const file1 = process.argv[2] || 'test/sample_data.xlsx';
  const file2 = process.argv[3] || 'test/sample_data_2.xlsx';
  
  compareExcelFiles(file1, file2).then(() => {
    console.log('\n🎉 Analysis complete!');
  }).catch(error => {
    console.error('💥 Analysis failed:', error);
    process.exit(1);
  });
}

// Export for use as module
export { 
  compareExcelFiles
};
