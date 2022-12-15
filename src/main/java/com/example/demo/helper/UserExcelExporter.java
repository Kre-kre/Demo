package com.example.demo.helper;

import com.example.demo.entity.SparePart;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.List;

@Slf4j
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@AllArgsConstructor
public class UserExcelExporter {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<SparePart> spareParts;

    public UserExcelExporter(List<SparePart> spareParts) {
        this.spareParts = spareParts;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("SpareParts");

        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);

        createCell(row, 0, "Cost", style);
        createCell(row, 1, "Description", style);
        createCell(row, 2, "Url", style);

    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        for (SparePart sparePart : spareParts) {
            int columnCount = 0;
            Row row = sheet.createRow(rowCount++);
            createCell(row, columnCount++, sparePart.getCost(), style);
            createCell(row, columnCount++, sparePart.getDescription(), style);
            createCell(row, columnCount++, sparePart.getUrl(), style);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    public InputStream export() throws IOException {
        writeHeaderLine();
        writeDataLines();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(spareParts.toString().getBytes().length);
        workbook.write(outputStream);
        workbook.close();
        byte[] bytes = outputStream.toByteArray();
        return new ByteArrayInputStream(bytes);
    }
}