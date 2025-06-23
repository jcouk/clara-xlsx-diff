/**
 * Production Example: Integrating Clara XLSX Diff with a JavaScript XLSX library
 * 
 * This example shows how to use the compiled ClojureScript code with a real
 * JavaScript XLSX library like 'xlsx' from SheetJS.
 * 
 * To run this example:
 * 1. npm install xlsx
 * 2. Place some XLSX files in the project directory
 * 3. Run: node production-example.js
 */

const XLSX = require('xlsx'); // npm install xlsx
const claraLib = require('./dist/clara-xlsx-diff.js');

/**
 * Read an XLSX file using the JavaScript XLSX library and convert to the format
 * expected by the Clara XLSX Diff ClojureScript code.
 */
function readXlsxFile(filePath) {
    try {
        const workbook = XLSX.readFile(filePath);
        const sheets = {};
        
        // Convert each sheet to the expected format
        workbook.SheetNames.forEach(sheetName => {
            const worksheet = workbook.Sheets[sheetName];
            const jsonData = XLSX.utils.sheet_to_json(worksheet, {
                header: 1,  // Use array of arrays format
                defval: ""  // Default value for empty cells
            });
            
            // Convert to the format expected by clara-xlsx-diff
            const cells = [];
            jsonData.forEach((row, rowIndex) => {
                row.forEach((cellValue, colIndex) => {
                    if (cellValue !== "") {
                        cells.push({
                            sheet: sheetName,
                            row: rowIndex + 1,  // 1-based indexing
                            col: colIndex + 1,  // 1-based indexing
                            value: cellValue
                        });
                    }
                });
            });
            
            sheets[sheetName] = {
                name: sheetName,
                cells: cells
            };
        });
        
        return {
            file: filePath,
            sheets: sheets
        };
    } catch (error) {
        console.error(`Error reading XLSX file ${filePath}:`, error.message);
        throw error;
    }
}

/**
 * Compare two XLSX files using Clara XLSX Diff
 */
async function compareXlsxFiles(file1Path, file2Path, options = {}) {
    console.log(`Comparing XLSX files: ${file1Path} vs ${file2Path}`);
    
    try {
        // Initialize the Clara library
        if (claraLib.init) {
            claraLib.init();
        }
        
        // For this example, we'll manually read the files and pass the data
        // In a full integration, you'd modify the ClojureScript xlsx.cljc to use
        // the JavaScript XLSX library directly
        
        console.log("Reading XLSX files...");
        const data1 = readXlsxFile(file1Path);
        const data2 = readXlsxFile(file2Path);
        
        console.log(`File 1 has ${Object.keys(data1.sheets).length} sheets`);
        console.log(`File 2 has ${Object.keys(data2.sheets).length} sheets`);
        
        // Call the Clara comparison function
        console.log("Running Clara XLSX Diff analysis...");
        const result = claraLib.compareXlsx(file1Path, file2Path, options);
        
        return {
            success: true,
            changes: result.changes || [],
            summary: result.summary || {},
            metadata: {
                file1: {
                    path: file1Path,
                    sheets: Object.keys(data1.sheets),
                    totalCells: Object.values(data1.sheets).reduce((sum, sheet) => sum + sheet.cells.length, 0)
                },
                file2: {
                    path: file2Path,
                    sheets: Object.keys(data2.sheets),
                    totalCells: Object.values(data2.sheets).reduce((sum, sheet) => sum + sheet.cells.length, 0)
                }
            }
        };
        
    } catch (error) {
        console.error("Error during comparison:", error.message);
        return {
            success: false,
            error: error.message
        };
    }
}

/**
 * Format the comparison results for display
 */
function displayResults(result) {
    console.log("\n" + "=".repeat(60));
    console.log("XLSX COMPARISON RESULTS");
    console.log("=".repeat(60));
    
    if (!result.success) {
        console.log("❌ Comparison failed:", result.error);
        return;
    }
    
    console.log("✅ Comparison completed successfully");
    
    if (result.metadata) {
        console.log("\n📊 File Metadata:");
        console.log(`  File 1: ${result.metadata.file1.path}`);
        console.log(`    Sheets: ${result.metadata.file1.sheets.join(', ')}`);
        console.log(`    Cells: ${result.metadata.file1.totalCells}`);
        
        console.log(`  File 2: ${result.metadata.file2.path}`);
        console.log(`    Sheets: ${result.metadata.file2.sheets.join(', ')}`);
        console.log(`    Cells: ${result.metadata.file2.totalCells}`);
    }
    
    console.log(`\n🔍 Changes found: ${result.changes.length}`);
    
    if (result.changes.length > 0) {
        console.log("\n📝 Change Details:");
        result.changes.slice(0, 10).forEach((change, index) => {
            console.log(`  ${index + 1}. ${JSON.stringify(change)}`);
        });
        
        if (result.changes.length > 10) {
            console.log(`  ... and ${result.changes.length - 10} more changes`);
        }
    }
    
    console.log("\n" + "=".repeat(60));
}

// Example usage
async function main() {
    // Check if files are provided as command line arguments
    const args = process.argv.slice(2);
    
    if (args.length < 2) {
        console.log("Usage: node production-example.js <file1.xlsx> <file2.xlsx>");
        console.log("\nExample files you could test with:");
        console.log("- Create test1.xlsx and test2.xlsx with some differences");
        console.log("- Or download sample XLSX files from the internet");
        return;
    }
    
    const [file1, file2] = args;
    
    console.log("Clara XLSX Diff - Production Example");
    console.log("====================================\n");
    
    try {
        const result = await compareXlsxFiles(file1, file2);
        displayResults(result);
    } catch (error) {
        console.error("Unexpected error:", error);
    }
}

// Export functions for use as a module
module.exports = {
    compareXlsxFiles,
    readXlsxFile,
    displayResults
};

// Run if called directly
if (require.main === module) {
    main();
}
