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

public class Food extends AppCompatActivity {

    private static final String TAG = "Food";
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
        setContentView(R.layout.activity_food);

        // Initialize UI components
        spinner = findViewById(R.id.spinnerF);
        livestockInputEditText = findViewById(R.id.LivestockAin);
        livestockInputVolatile = findViewById(R.id.LivestockWin);
        totalWasteEditText = findViewById(R.id.TotalWasteEditText);
        totalVolatileSolidsEditText = findViewById(R.id.TotalVolatileSolidsEditText);

        // Initialize spinner items list
        spinnerItems = new ArrayList<>();

        // Populate spinner from Excel file
        populateSpinnerFromExcel("formula.xls");

        Handler mHandler = new Handler(getMainLooper());
//        findViewById(R.id.CalculateLivestock).setEnabled(true);

        // Setup Calculate button click listener
        Button calculateButton = findViewById(R.id.CalculateLivestock);
        calculateButton.setOnClickListener(v -> {
            calculateTotalVolatileSolids();
//            calcuateFoodAmount();

            // Check if any fields have values
            if (fieldsHaveValues()) {
                // Post a delayed message to the Handler after 5 seconds to show confirmation dialog
                mHandler.postDelayed(() -> {
                    showConfirmationDialog();
//                    calculateButton.setEnabled(false);
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
            Intent intent = new Intent(Food.this, Digester.class);
            startActivity(intent);
            hasProceededToDigester = true; // Update flag
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            findViewById(R.id.CalculateLivestock).setEnabled(true);
            // If Cancel is chosen, do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

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

            // Initialize the row indexes for G3 to G12
            int startRow = 2; // G3
            int endRow = 11; // G12

            // Iterate through rows G3 to G12
            for (int i = startRow; i <= endRow; i++) {
                Row row = sheet.getRow(i);

                // Fetch cell G (index 6) from each row
                Cell cell = row.getCell(6); // G column

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

        double foodNum = Double.parseDouble(livestockInputStr);
        double livestockVolatile = 0;
        double resultF = 0;

        // Calculate based on selected item
        switch (selectedItem) {
            case "Vegetable Waste":
                livestockVolatile = 0.16;

                break;
            case "Rice Straw":
                livestockVolatile = 0.36;
                break;
            case "Fruit Waste":
                livestockVolatile = 0.14;
                break;
            case "Mixed Organic Waste":
                livestockVolatile = 0.26;
                break;
            case "Cereal or Grains":
                livestockVolatile = 0.81;
                break;
            case "Wheat Straw":
                livestockVolatile = 0.39;
                break;
            case "Grass":
                livestockVolatile = 0.51;
                break;
            case "Corn Stalk":
                livestockVolatile = 0.43;
                break;
            case "Fat":
                livestockVolatile = 0.83;
                break;
            case "Mixed Food Waste":
                livestockVolatile = 0.08;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItem);
        }
        // Round off the livestockVolatile value to four decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        livestockVolatile = Double.parseDouble(decimalFormat.format(livestockVolatile));

        // Set the value of livestockInputVolatile
        livestockInputVolatile.setText(String.valueOf(livestockVolatile));

        // Calculate result based on foodNum and livestockVolatile
        resultF = foodNum * livestockVolatile;

        // Round off the result to four decimal places
        resultF = Double.parseDouble(decimalFormat.format(resultF));

        // Update UI with result
        totalVolatileSolidsEditText.setText(String.valueOf(resultF));
        String livestockInput = livestockInputEditText.getText().toString().trim();
        // Update UI with result
        totalWasteEditText.setText(String.valueOf(livestockInput));
    }

    // Method to calculate feedstock amount
    private void calcuateFoodAmount() {
        String selectedItem = spinner.getSelectedItem().toString();
        double foodNum = Double.parseDouble(livestockInputEditText.getText().toString());
        double resultF;

        // Calculate based on selected item
        switch (selectedItem) {
            case "Vegetable Waste":
                resultF = calcuateFoodAmountEggplant(foodNum);
                break;
            case "Rice Straw":
                resultF = calcuateFoodAmountRice(foodNum);
                break;
            case "Fruit Waste":
                resultF = calcuateFoodAmountBanana(foodNum);
                break;
            case "Mixed Organic Waste":
                resultF = calcuateFoodAmountMixedOrganic(foodNum);
                break;
            case "Cereal or Grains":
                resultF = calcuateFoodAmountCereal(foodNum);
                break;
            case "Wheat Straw":
                resultF = calculateFoodAmountWheat(foodNum);
            case "Grass":
                resultF = calcuateFoodAmountGrass(foodNum);
                break;
            case "Corn Stalk":
                resultF = calculateFoodAmountCornStalk(foodNum);
                break;
            case "Fat":
                resultF = calculateFoodAmountFat(foodNum);
                break;
            case "Mixed Food Waste":
                resultF = calculateFoodAmountMixedFood(foodNum);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + selectedItem);
        }

        // Update UI with result
        totalWasteEditText.setText(String.valueOf(resultF));
    }

    // Methods to calculate feedstock amount for each food type
    private double calcuateFoodAmountEggplant(double foodNum) {
        // Formula for Vegetable Waste
        double wVeg = 0.16;
        return foodNum * wVeg;
    }

    private double calcuateFoodAmountRice(double foodNum) {
        // Formula for Rice Straw
        double wRic = 0.36;
        return foodNum * wRic;
    }

    private double calcuateFoodAmountBanana(double foodNum) {
        // Formula for Fruit Waste
        double wBan = 0.14;
        return foodNum * wBan;
    }

    private double calcuateFoodAmountMixedOrganic(double foodNum) {
        // Formula for Mixed Organic Waste
        double wOrg = 0.26;
        return foodNum * wOrg;
    }

    private double calcuateFoodAmountCereal(double foodNum) {
        // Formula for Cereal or Grains
        double wCer = 0.81;
        return foodNum * wCer;
    }

    private double calculateFoodAmountWheat(double foodNum) {
        // Formula for Wheat Straw
        double wWht = 0.39;
        return foodNum * wWht;
    }

    private double calcuateFoodAmountGrass(double foodNum) {
        // Formula for Grass
        double wGras = 0.51;
        return foodNum * wGras;
    }

    private double calculateFoodAmountCornStalk(double foodNum) {
        // Formula for Corn Stalk
        double wCS = 0.43;
        return foodNum * wCS;
    }
    private double calculateFoodAmountFat(double foodNum) {
        // Formula for Fat
        double wFat = 0.83;
        return foodNum * wFat;
    }
    private double calculateFoodAmountMixedFood(double foodNum) {
        // Formula for Mixed Food Waste
        double wMF = 0.8;
        return foodNum * wMF;
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
            String foodType = spinner.getSelectedItem().toString();
            // Get the volatile solids input from the EditText
            double foodNum = Double.parseDouble(livestockInputEditText.getText().toString());
            // Get the volatile solids input from the EditText
            double volatileSolids = Double.parseDouble(livestockInputVolatile.getText().toString());
            // Get the total production input from the EditText
            double totalProduction = Double.parseDouble(livestockInputEditText.getText().toString());//Changed to the result of the entered value
            // Get the total volatile solids calculated
            double result = Double.parseDouble(totalVolatileSolidsEditText.getText().toString());

            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(foodType); // Column B
            newRow.createCell(2).setCellValue(foodNum); // Column C
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

    // Method to delete the entry of the same rows and columns when activity is sent back
    private void deleteEntryFromExcel(String fileName) {
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(1);

            // Find the last row with data
            int lastRowNum = sheet.getLastRowNum();

            // Iterate through rows and columns to clear data
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Clear values in columns 1 to 5
                    for (int j = 1; j <= 5; j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            cell.setCellValue("");
                        }
                    }
                }
            }

            // Save the workbook with the cleared data
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();

            // Close the workbook
            workbook.setHidden(true);
        } catch (IOException e) {
            Log.e(TAG, "Error deleting entry from Database: ", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}

