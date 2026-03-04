package pageObjects;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class InvokeColumns {
    public List<String> getColumnDataByTitle(String filePath, String sheetName, String columnTitle) throws IOException {
        List<String> columnData = new ArrayList<>();

        FileInputStream fis = new FileInputStream(new File(filePath));
        Workbook workbook = WorkbookFactory.create(fis);
        Sheet sheet = workbook.getSheet(sheetName);

        Row headerRow = sheet.getRow(0);
        int colIndex = -1;

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && cell.getStringCellValue().equalsIgnoreCase(columnTitle)) {
                colIndex = i;
                break;
            }
        }

        if (colIndex == -1) {
            workbook.close();
            throw new IllegalArgumentException("Column with title '" + columnTitle + "' not found");
        }

        // Loop through the rest of the rows and get the data
        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(colIndex);
                columnData.add(cell != null ? cell.toString() : "");
            }
        }

        workbook.close();
        return columnData;
    }

}
