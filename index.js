// Main entry point for clara-xlsx-diff npm package

// Import the compiled ClojureScript library
require('./target/main-simple.js');

// Import TypeScript utilities
const utils = require('./utils');

// Access the ClojureScript namespace
const claraXlsxNs = global.clara_xlsx_diff?.main;

// Create the main comparison function
function compareXlsxBuffers(file1Buffer, file2Buffer, file1Name = 'file1', file2Name = 'file2') {
  if (!claraXlsxNs?.compare_xlsx_buffers) {
    throw new Error('Clara XLSX library not loaded properly');
  }
  
  return claraXlsxNs.compare_xlsx_buffers(file1Buffer, file2Buffer, file1Name, file2Name);
}

// Initialize function
function init() {
  if (!claraXlsxNs?.init) {
    throw new Error('Clara XLSX library not loaded properly');
  }
  
  return claraXlsxNs.init();
}

// Export everything
module.exports = {
  // Core functions
  compareXlsxBuffers,
  init,
  
  // Utility functions
  ...utils,
  
  // For convenience, also export the example function
  compareExcelFiles: utils.compareExcelFiles || function(file1Path, file2Path) {
    const fs = require('fs');
    const file1Buffer = fs.readFileSync(file1Path);
    const file2Buffer = fs.readFileSync(file2Path);
    return compareXlsxBuffers(file1Buffer, file2Buffer, file1Path, file2Path);
  }
};
