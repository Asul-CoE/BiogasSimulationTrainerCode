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

public class ExcelDataWriterFood {

    private static final String TAG = "ExcelDataWriterFood";
    private final Context context;
    private final Spinner spinnerFud;
    private final EditText FAin;
    private final EditText FoodWin;
    private final EditText totalWasteEditTextF;
    private final EditText totalVolatileSolidsEditTextF;

    public ExcelDataWriterFood(Context context, Spinner spinnerFud, EditText FAin, EditText FoodWin, EditText totalWasteEditTextF, EditText totalVolatileSolidsEditTextF) {
        this.context = context;
        this.spinnerFud = spinnerFud;
        this.FAin = FAin;
        this.FoodWin = FoodWin;
        this.totalWasteEditTextF = totalWasteEditTextF;
        this.totalVolatileSolidsEditTextF = totalVolatileSolidsEditTextF;
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
    public void writeDataToExcelFood(String fileName) {
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

            // Get the selected food type from the spinner
            String foodType = spinnerFud.getSelectedItem().toString();
            // Get the FAin input from the EditText
            double FAinValue = Double.parseDouble(FAin.getText().toString());
            // Get the FoodWin input from the EditText
            double FoodWinValue = Double.parseDouble(FoodWin.getText().toString());
            // Get the total waste input from the EditText
            double totalWasteValue = Double.parseDouble(totalWasteEditTextF.getText().toString());
            // Get the total volatile solids calculated
            double totalVolatileSolidsValue = Double.parseDouble(totalVolatileSolidsEditTextF.getText().toString());

            // Validate inputs before writing to Excel
            if (isValidInput(foodType, FAinValue, FoodWinValue, totalWasteValue, totalVolatileSolidsValue)) {
                // Update the cell values with the new inputs
                // Update the cell values with the new inputs
                newRow.createCell(0).setCellValue(dateTime); // Column A
                newRow.createCell(1).setCellValue(foodType); // Column B
                newRow.createCell(2).setCellValue(FAinValue); // Column C
                newRow.createCell(3).setCellValue(FoodWinValue); // Column D
                newRow.createCell(4).setCellValue(totalWasteValue); // Column E
                newRow.createCell(5).setCellValue(totalVolatileSolidsValue); // Column F

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
            if (isEmptyRow(row)) {
                return i; // Return the index of the first empty row found
            }
        }
        // If no empty row is found, return the last row index + 1
        return lastRowNum +0;
    }

    // Method to check if a row is empty
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
            Cell cell = row.getCell(j);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }


    // Method to validate input data
    private boolean isValidInput(String foodType, double FAin, double FoodWin, double totalWaste, double totalVolatileSolids) {
        return foodType != null && !foodType.isEmpty()
                && FAin >= 0
                && FoodWin >= 0
                && totalWaste >= 0
                && totalVolatileSolids >= 0;
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
        // If no non-empty row is found, return 0 (indicating no data)
        return 0;
    }
}
