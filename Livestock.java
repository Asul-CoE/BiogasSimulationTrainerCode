package com.example.biogassimulation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Livestock extends AppCompatActivity {

    private static final String TAG = "Livestock";
    private Spinner spinner;
    private EditText livestockInputEditText;
    private EditText livestockInputVolatile;
    private EditText totalWasteEditText;
    private EditText totalVolatileSolidsEditText;
    private List<String> spinnerItems;
    private boolean hasProceededToDigester = false; // Flag to track whether user has proceeded to Digester activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livestock);

        // Initialize UI components
        spinner = findViewById(R.id.spinnerL);
        livestockInputEditText = findViewById(R.id.LivestockAin);
        livestockInputVolatile = findViewById(R.id.LivestockWin);
        totalWasteEditText = findViewById(R.id.TotalWasteEditText);
        totalVolatileSolidsEditText = findViewById(R.id.TotalVolatileSolidsEditText);

        // Initialize spinner items list
        spinnerItems = new ArrayList<>();

        // Populate spinner from Excel file
        populateSpinnerFromExcel("formula.xls");

        Handler mHandler = new Handler(getMainLooper());

        // Setup Calculate button click listener
        Button calculateButton = findViewById(R.id.CalculateLivestock);
        calculateButton.setOnClickListener(v -> {
            calculateTotalVolatileSolids();
            calculateFeedstockAmount();

            // Check if any fields have values
            if (fieldsHaveValues()) {
                // Post a delayed message to the Handler after 5 seconds to show confirmation dialog
                mHandler.postDelayed(() -> {
                    showConfirmationDialog();
                }, 2000);
            } else {
                // If no fields have values, show a toast message indicating no data to save
                Toast.makeText(this, "No data to save.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Method to check if any fields have values
    private boolean fieldsHaveValues() {
        String livestockInput = livestockInputEditText.getText().toString().trim();
        String livestockVolatileInput = livestockInputVolatile.getText().toString().trim();
        String totalWasteInput = totalWasteEditText.getText().toString().trim();
        String totalVolatileSolidsInput = totalVolatileSolidsEditText.getText().toString().trim();

        return !livestockInput.isEmpty() || !livestockVolatileInput.isEmpty() || !totalWasteInput.isEmpty() || !totalVolatileSolidsInput.isEmpty();
    }

    // Method to show confirmation dialog
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to proceed to the Digester?");
        builder.setPositiveButton("Proceed", (dialog, which) -> {
            // If Proceed is chosen, save data to Excel and initiate Digester activity
            writeDataToExcel("formula.xls");
            Intent intent = new Intent(Livestock.this, Digester.class);
            startActivity(intent);
            hasProceededToDigester = true; // Update flag
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // If Cancel is chosen, do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to populate spinner with items from Excel file
    private void populateSpinnerFromExcel(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        FileInputStream fileInputStream = null;
        Workbook workbook = null;

        try {
            if (!file.exists()) {
                // If the file does not exist, create it
                createExcelFile(fileName);
            }

            fileInputStream = new FileInputStream(file);
            Log.e(TAG, "Reading from Excel: ");
            workbook = new HSSFWorkbook(fileInputStream);

            // Fetch sheet at position '0' from the workbook
            Sheet sheet = workbook.getSheetAt(0);

            // Iterate through rows H3 to H10
            for (int i = 2; i <= 9; i++) {
                Row row = sheet.getRow(i);

                // Fetch cell H (index 7) from each row
                Cell cell = row.getCell(7);

                // Check for null cell values
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    spinnerItems.add(cell.getStringCellValue());
                }
            }

            // Populate spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

        } catch (IOException e) {
            Log.e(TAG, "Error Reading Exception: ", e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (workbook != null) {
                    workbook.setHidden(true); // Close the workbook
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    // Method to create Excel file with sample data
    private void createExcelFile(String fileName) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        String[] data = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(data[i]);
        }

        FileOutputStream outputStream = null;
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            Log.e(TAG, "New file created: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error creating saved file: ", e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                workbook.setHidden(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Method to calculate total volatile solids
    private void calculateTotalVolatileSolids() {
        String selectedItem = spinner.getSelectedItem().toString();
        String livestockInputStr = livestockInputEditText.getText().toString().trim();

        // Check if input field is empty or null
        if (livestockInputStr.isEmpty()) {
            return; // If field is empty, return without calculation
        }

        double livestockNum = Double.parseDouble(livestockInputStr);
        double livestockVolatile = 0; // Initialize livestockVolatile variable
        double resultV = 0; // Initialize resultV variable

        // Calculate based on selected item
        switch (selectedItem) {
            case "Buffalo Waste":
                livestockVolatile = 1.94;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Cow Waste":
                livestockVolatile = 1.42;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Calf Waste":
                livestockVolatile = 0.50;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Sheep/Goat Waste":
                livestockVolatile = 0.44;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Pig Waste":
                livestockVolatile = 1;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Horse Waste":
                livestockVolatile = 2.24;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Human Waste":
                livestockVolatile = 0.03;
                resultV = livestockNum * livestockVolatile;
                break;
            case "Chicken Waste":
                livestockVolatile = 2.77;
                resultV = (livestockNum / 100) * livestockVolatile; // Adjusted formula for chicken waste
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItem);
        }

        // Round off the result to four decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.####");

        // Set the value of livestockInputVolatile
        livestockInputVolatile.setText(String.valueOf(livestockVolatile));

        // Calculate result based on livestockNum and livestockVolatile
        resultV = Double.parseDouble(decimalFormat.format(resultV));

        // Update UI with result
        totalVolatileSolidsEditText.setText(String.valueOf(resultV));
    }


    // Method to calculate feedstock amount
    private void calculateFeedstockAmount() {
        String selectedItem = spinner.getSelectedItem().toString();
        String livestockInputStr = livestockInputEditText.getText().toString().trim();

        // Check if input field is empty or null
        if (livestockInputStr.isEmpty()) {
            return; // If field is empty, return without calculation
        }

        double livestockNum = Double.parseDouble(livestockInputStr);
        double resultL;

        // Calculate based on selected item
        switch (selectedItem) {
            case "Buffalo Waste":
                resultL = calculateFeedstockAmountBuffalo(livestockNum);
                break;
            case "Cow Waste":
                resultL = calculateFeedstockAmountCow(livestockNum);
                break;
            case "Calf Waste":
                resultL = calculateFeedstockAmountCalf(livestockNum);
                break;
            case "Sheep/Goat Waste":
                resultL = calculateFeedstockAmountSheepGoat(livestockNum);
                break;
            case "Pig Waste":
                resultL = calculateFeedstockAmountPig(livestockNum);
                break;
            case "Chicken Waste":
                resultL = calculateFeedstockAmountHens(livestockNum);
                break;
            case "Horse Waste":
                resultL = calculateFeedstockAmountHorse(livestockNum);
                break;
            case "Human Waste":
                resultL = calculateFeedstockAmountHuman(livestockNum);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItem);
        }
        // Round off the result to four decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        resultL = Double.parseDouble(decimalFormat.format(resultL));

        // Update UI with result
        totalWasteEditText.setText(String.valueOf(resultL));
    }

    // Methods to calculate feedstock amount for each livestock type
    private double calculateFeedstockAmountBuffalo(double livestockNum) {
        // Formula for Buffalo
        double wBuff = 14;
        return livestockNum * wBuff;
    }

    private double calculateFeedstockAmountCow(double livestockNum) {
        // Formula for Cow
        double wCow = 10;
        return livestockNum * wCow;
    }

    private double calculateFeedstockAmountCalf(double livestockNum) {
        // Formula for Calf
        double wCalf = 5; // Assuming a value of 3 for wCalf
        return livestockNum * wCalf;
    }

    private double calculateFeedstockAmountSheepGoat(double livestockNum) {
        // Formula for Sheep/Goat
        double wShp = 2;
        return livestockNum * wShp;
    }

    private double calculateFeedstockAmountPig(double livestockNum) {
        // Formula for Pig
        double wPg = 5; // Assuming a value of 5 for wPg
        return livestockNum * wPg;
    }

    private double calculateFeedstockAmountHens(double livestockNum) {
        // Formula for Chicken, base 100
        double wChk = 7.5;
        return wChk* (livestockNum / 100);
    }

    private double calculateFeedstockAmountHorse(double livestockNum) {
        // Formula for Horse
        double wHors = 10;
        return livestockNum * wHors;
    }

    private double calculateFeedstockAmountHuman(double livestockNum) {
        // Formula for Human waste
        double wHum = 0.2;
        return livestockNum * wHum;
    }

    // Method to write data to Excel
    private void writeDataToExcel(String fileName) {
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(1);

            // Find the last row with non-empty data in columns 1-5
            int lastRowNum = getLastDataRowIndex(sheet);

            // Create a new row after the last row with data
            Row newRow = sheet.createRow(lastRowNum + 1);

            // Get the current date and time
            String dateTime = getCurrentDateTime();

            // Get the selected animal type from the spinner
            String animalType = spinner.getSelectedItem().toString();
            // Get the volatile solids input from the EditText
            double livestocknum = Double.parseDouble(livestockInputEditText.getText().toString());
            // Get the volatile solids input from the EditText
            double volatileSolids = Double.parseDouble(livestockInputVolatile.getText().toString());
            // Get the total production input from the EditText
            double totalProduction = Double.parseDouble(totalWasteEditText.getText().toString());
            // Get the total volatile solids calculated
            double result = Double.parseDouble(totalVolatileSolidsEditText.getText().toString());

            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(animalType); // Column B
            newRow.createCell(2).setCellValue(livestocknum); // Column C
            newRow.createCell(3).setCellValue(volatileSolids); // Column D
            newRow.createCell(4).setCellValue(totalProduction); // Column E
            newRow.createCell(5).setCellValue(result); // Column F

            // Save the workbook with the updated data to a temporary file
            File tempFile = new File(getExternalFilesDir(null), "temp_" + System.currentTimeMillis() + ".xls");
            FileOutputStream tempOutputStream = new FileOutputStream(tempFile);
            workbook.write(tempOutputStream);
            tempOutputStream.close();

            // Close the workbook
            workbook.close();

            // Replace the original file with the temporary file
            File originalFile = new File(getExternalFilesDir(null), fileName);
            if (originalFile.exists()) {
                originalFile.delete(); // Delete the original file
            }
            if (tempFile.renameTo(originalFile)) {
                Toast.makeText(this, "Data saved to file", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error saving data to file", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file: ", e);
        }
    }
    // Method to get current date and time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    // Method to find the last row index with data
    private int getLastDataRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                boolean hasData = false;
                for (int j = 1; j <= 4; j++) { // Check columns 1 to 5 for data
                    Cell cell = row.getCell(j);
                    if (cell != null && !cell.getStringCellValue().isEmpty()) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    return i;
                }
            }
        }
        // If no non-empty row is found, return -1 (indicating the first row)
        return -1;
    }
    @Override
    protected void onStop() {
        super.onStop();

    }
}

