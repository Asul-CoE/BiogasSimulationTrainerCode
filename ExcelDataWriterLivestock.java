package com.example.biogassimulation;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExcelDataWriterLivestock {

    private static final String TAG = "ExcelDataWriterLivestock";
    private Context context;
    private Spinner spinner;
    private EditText livestockInputEditText;
    private EditText livestockInputVolatile;
    private EditText totalWasteEditText;
    private EditText totalVolatileSolidsEditText;

    public ExcelDataWriterLivestock(Context context, Spinner spinner, EditText livestockInputEditText, EditText livestockInputVolatile, EditText totalWasteEditText, EditText totalVolatileSolidsEditText) {
        this.context = context;
        this.spinner = spinner;
        this.livestockInputEditText = livestockInputEditText;
        this.livestockInputVolatile = livestockInputVolatile;
        this.totalWasteEditText = totalWasteEditText;
        this.totalVolatileSolidsEditText = totalVolatileSolidsEditText;
    }

    // Method to write data to Excel
    public void writeDataToExcelLivestock(String fileName) {
        FileInputStream fileInputStream = null;
        FileOutputStream outputStream = null;
        Workbook workbook = null;

        try {
            File file = new File(context.getExternalFilesDir(null), fileName);
            // Create a backup of the original file
            File backupFile = new File(context.getExternalFilesDir(null), fileName + ".bak");
            copyFile(file, backupFile);

            fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(1);

            // Find the index of the next empty row
            int nextEmptyRowIndex = getNextEmptyRowIndex(sheet);
            // Get the current date and time
            String dateTime = getCurrentDateTime();
            // Create a new row at the next empty index
            Row newRow = sheet.createRow(nextEmptyRowIndex);

            // Get the selected animal type from the spinner
            String animalType = spinner.getSelectedItem().toString();
            // Get the livestock input from the EditText
            double livestocknum = Double.parseDouble(livestockInputEditText.getText().toString());
            // Get the volatile solids input from the EditText
            double volatileSolids = Double.parseDouble(livestockInputVolatile.getText().toString());
            // Get the total production input from the EditText
            double totalProduction = Double.parseDouble(totalWasteEditText.getText().toString());
            // Get the total volatile solids calculated
            double result = Double.parseDouble(totalVolatileSolidsEditText.getText().toString());

            // Validate inputs before writing to Excel
            if (isValidInput(animalType, livestocknum, volatileSolids, totalProduction, result)) {

                // Update the cell values with the new inputs
                newRow.createCell(0).setCellValue(dateTime); // Column A
                newRow.createCell(1).setCellValue(animalType); // Column B
                newRow.createCell(2).setCellValue(livestocknum); // Column C
                newRow.createCell(3).setCellValue(volatileSolids); // Column D
                newRow.createCell(4).setCellValue(totalProduction); // Column E
                newRow.createCell(5).setCellValue(result); // Column F

                // Save the workbook with the updated data to a temporary file
                File tempFile = new File(context.getExternalFilesDir(null), fileName + ".tmp");
                outputStream = new FileOutputStream(tempFile);
                workbook.write(outputStream);
                outputStream.close();

                // Replace the original file with the temporary file
                if (tempFile.renameTo(file)) {
                    Toast.makeText(context, "Data saved to file", Toast.LENGTH_SHORT).show();
                } else {
                    throw new IOException("Failed to replace the original file with the temporary file");
                }
            } else {
                Toast.makeText(context, "Invalid input data", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file: ", e);
            Toast.makeText(context, "Error writing to file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            // Ensure resources are closed in case of an exception
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing FileInputStream: ", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing FileOutputStream: ", e);
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing Workbook: ", e);
                }
            }
        }
    }
    // Method to get current date and time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Method to find the index of the next empty row
    private int getNextEmptyRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row == null) {
                return i; // Return the index of the first empty row found
            } else {
                boolean isEmpty = true;
                for (int j = 1; j <= 5; j++) { // Check columns 1 to 5 for data
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    return i; // Return the index of the first row with all blank cells found
                }
            }
        }
        // If no empty row is found, return the last row index + 1
        return lastRowNum + 0;
    }


    // Method to find the last row index with data
    private int getLastDataRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) { // Start checking from the last row
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int j = 1; j <= 5; j++) { // Check columns 1 to 5 for data
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        return i;
                    }
                }
            }
        }
        // If no non-empty row is found, return -1 (indicating no data)
        return -1;
    }


    // Method to validate input data
    private boolean isValidInput(String animalType, double livestocknum, double volatileSolids, double totalProduction, double result) {
        return animalType != null && !animalType.isEmpty()
                && livestocknum >= 0
                && volatileSolids >= 0
                && totalProduction >= 0
                && result >= 0;
    }

    // Method to copy file (used for creating backups)
    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}
