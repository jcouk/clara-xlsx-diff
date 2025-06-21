# Test Resources

This directory contains sample XLSX files for testing the Clara XLSX Diff functionality.

## Files

- `sample1.xlsx` - Original version for comparison testing
- `sample2.xlsx` - Modified version for comparison testing
- `empty.xlsx` - Empty spreadsheet for edge case testing
- `complex.xlsx` - Multi-sheet spreadsheet with various data types

## Creating Test Files

To create test XLSX files for development:

1. Use Excel, LibreOffice Calc, or Google Sheets
2. Create simple spreadsheets with known differences
3. Save as .xlsx format in this directory
4. Update test cases to reference these files

## Sample Data Patterns

For consistent testing, use these patterns:

- Simple text values: "Hello", "World", "Test"
- Numbers: 1, 2, 3, 42, 3.14
- Formulas: =SUM(A1:A3), =A1+B1
- Mixed data types in different cells
