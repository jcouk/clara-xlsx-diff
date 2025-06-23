#!/usr/bin/env node

/**
 * Demo: How to call the Clara XLSX Diff library from JavaScript
 * 
 * This demonstrates two ways to use the compiled ClojureScript code:
 * 1. As a library module (clara-xlsx-diff.js)
 * 2. As an executable script (clara-xlsx-diff-exe.js)
 */

console.log("Clara XLSX Diff - JavaScript Demo");
console.log("==================================\n");

// Method 1: Using the library module
console.log("Method 1: Using the library module");
console.log("-----------------------------------");

try {
    const claraLib = require('./dist/clara-xlsx-diff.js');
    
    // Initialize the library
    if (claraLib.init) {
        claraLib.init();
    }
    
    console.log("Library loaded successfully!");
    console.log("Available functions:", Object.keys(claraLib));
    
    // Example usage with mock files (since we don't have real XLSX files set up)
    console.log("\n--- Testing with mock file paths ---");
    
    // Call the main function
    if (claraLib.main) {
        console.log("Calling main function...");
        try {
            const result = claraLib.main("file1.xlsx", "file2.xlsx");
            console.log("Main function result:", result);
        } catch (error) {
            console.log("Expected error (mock XLSX reader):", error.message);
        }
    }
    
    // Call the compareXlsx function directly
    if (claraLib.compareXlsx) {
        console.log("\nCalling compareXlsx function...");
        try {
            const result = claraLib.compareXlsx("file1.xlsx", "file2.xlsx");
            console.log("Compare result:", result);
        } catch (error) {
            console.log("Expected error (mock XLSX reader):", error.message);
        }
    }
    
} catch (error) {
    console.error("Error loading library:", error.message);
}

console.log("\n" + "=".repeat(50));

// Method 2: Using the executable script
console.log("\nMethod 2: Using the executable script");
console.log("--------------------------------------");

const { spawn } = require('child_process');

console.log("Running the executable script...");

const executable = spawn('node', ['./dist/clara-xlsx-diff-exe.js', 'file1.xlsx', 'file2.xlsx'], {
    stdio: ['pipe', 'pipe', 'pipe']
});

executable.stdout.on('data', (data) => {
    console.log('stdout:', data.toString().trim());
});

executable.stderr.on('data', (data) => {
    console.log('stderr:', data.toString().trim());
});

executable.on('close', (code) => {
    console.log(`Executable finished with exit code: ${code}`);
    
    console.log("\n" + "=".repeat(50));
    console.log("\nDemo completed!");
    console.log("\nNotes:");
    console.log("- The errors are expected because we're using mock XLSX files");
    console.log("- In a real scenario, you'd provide paths to actual XLSX files");
    console.log("- The ClojureScript implementation currently uses a mock XLSX reader");
    console.log("- For production use, integrate a JavaScript XLSX library like 'xlsx' or 'exceljs'");
});

executable.on('error', (err) => {
    console.error('Error running executable:', err.message);
});
