package com.seleniumboot.testdata;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads one data row from an XLSX workbook via Apache POI.
 * Separated from {@link TestDataLoader} so POI classes are only loaded
 * when this class is actually referenced (lazy JVM class loading).
 */
final class ExcelDataReader {

    private ExcelDataReader() {}

    /**
     * @param is        InputStream of the XLSX file
     * @param sheetName sheet name — uses the first sheet when blank
     * @param rowIndex  zero-based data-row index (0 = first row after the header row)
     */
    static Map<String, Object> read(InputStream is, String sheetName, int rowIndex)
            throws Exception {
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = (sheetName == null || sheetName.isEmpty())
                ? wb.getSheetAt(0)
                : wb.getSheet(sheetName);

            if (sheet == null) {
                throw new IllegalArgumentException(
                    "[TestData] Excel sheet '" + sheetName + "' not found in workbook."
                );
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("[TestData] Excel sheet is empty.");
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cellStringValue(cell).trim());
            }

            // data rows start at index 1 in the sheet; rowIndex is 0-based among data rows
            int sheetRowIndex = rowIndex + 1;
            Row dataRow = sheet.getRow(sheetRowIndex);
            if (dataRow == null) {
                throw new IllegalArgumentException(
                    "[TestData] Excel row " + rowIndex + " not found " +
                    "(sheet has fewer data rows than requested)."
                );
            }

            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = dataRow.getCell(i);
                result.put(headers.get(i), cell == null ? "" : cellValue(cell));
            }
            return result;
        }
    }

    private static Object cellValue(Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
        switch (type) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) return (long) d;
                return d;
            case BOOLEAN: return cell.getBooleanCellValue();
            case BLANK:   return "";
            default:      return cell.getStringCellValue();
        }
    }

    private static String cellStringValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
        if (type == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            if (d == Math.floor(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        return cell.getStringCellValue();
    }
}
