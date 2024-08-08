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
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class Mixed extends AppCompatActivity {

    private static final String TAG = "Mixed";
    private Spinner spinnerLive,spinnerFud;
    private EditText FAin;
    private EditText FoodWin;

    private EditText livestockInputEditText;
    private EditText livestockInputVolatile;
    private EditText totalWasteEditText;
    private EditText totalVolatileSolidsEditText;
    private List<String> spinnerItems, spinnerItemsF;
    private EditText totalWasteEditTextF;
    private EditText totalVolatileSolidsEditTextF;
    private ExcelDataWriterLivestock excelDataWriterLivestock;
    private ExcelDataWriterLivestock excelDataWriterFood;


    private boolean hasProceededToDigester = false; // Flag to track whether user has proceeded to Digester activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed);


        // Initialize UI components
        spinnerLive = findViewById(R.id.spinnerL);
        spinnerFud = findViewById(R.id.spinnerF);

        FAin = findViewById(R.id.FAin);
        FoodWin = findViewById(R.id.FoodWin);

        livestockInputEditText = findViewById(R.id.LivestockAin);
        livestockInputVolatile = findViewById(R.id.LivestockWin);
        totalWasteEditText = findViewById(R.id.LTotalWasteEditText);
        totalVolatileSolidsEditText = findViewById(R.id.LTotalVolatileSolidsEditText);
        totalWasteEditTextF = findViewById(R.id.FTotalWasteEditText);
        totalVolatileSolidsEditTextF = findViewById(R.id.FTotalVolatileSolidsEditText);
//
//        // Initialize instances of ExcelDataWriterLivestock and ExcelDataWriterFood
        excelDataWriterLivestock = new ExcelDataWriterLivestock(this, spinnerLive, livestockInputEditText, livestockInputVolatile, totalWasteEditText, totalVolatileSolidsEditText);
//        ExcelDataWriterFood excelDataWriterFood = new ExcelDataWriterFood(this, spinnerFud, FAin, FoodWin, totalWasteEditTextF, totalVolatileSolidsEditTextF);
//

        // Initialize spinner items lists
        spinnerItems = new ArrayList<>();
        spinnerItemsF = new ArrayList<>();


        // Populate spinners from Excel
        populateSpinnerFromExcel("formula.xls", spinnerLive, spinnerItems);
        populateSpinnerFromExcel("formula.xls", spinnerFud, spinnerItemsF);

        // Setup Calculate button click listener
        Button calculateButton = findViewById(R.id.CalculateAll);
        calculateButton.setOnClickListener(v -> {

            // Check if any fields have values
            if (fieldsHaveValues()) {
                calculateTotalVolatileSolids();
                calculateFeedstockAmount();
                calculateFoodWaste();

                // Post a delayed message to the Handler after 5 seconds to show confirmation dialog
                new Handler().postDelayed(() -> {
                    showConfirmationDialog();
                }, 3000);
            } else {
                // If no fields have values, show a toast message indicating no data to save
                Toast.makeText(this, "Missing data? Cannot save.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Method to check if any fields have values
    private boolean fieldsHaveValues() {
        String livestockInput = livestockInputEditText.getText().toString().trim();
//        String livestockVolatileInput = livestockInputVolatile.getText().toString().trim();
        String totalWasteInput = totalWasteEditText.getText().toString().trim();
        String totalVolatileSolidsInput = totalVolatileSolidsEditText.getText().toString().trim();


        return !livestockInput.isEmpty() || !totalWasteInput.isEmpty() || !totalVolatileSolidsInput.isEmpty();
    }
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to proceed to the Digester?");
        builder.setPositiveButton("Proceed", (dialog, which) -> {
            // If Proceed is chosen, write data to Excel with ExcelDataWriterLivestock
            excelDataWriterLivestock.writeDataToExcelLivestock("formula.xls");

            // After writing livestock data, proceed with writing food data
            writeFoodDataToExcel();

        }).setNegativeButton("Cancel", (dialog, which) -> {
            // If Cancel is chosen, do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Method to write food data to Excel after livestock data is written
    private void writeFoodDataToExcel() {
        ExcelDataWriterFood excelDataWriterFood = new ExcelDataWriterFood(this, spinnerFud, FAin, FoodWin, totalWasteEditTextF, totalVolatileSolidsEditTextF);
        excelDataWriterFood.writeDataToExcelFood("formula.xls");

        // After writing food data, proceed to the Digester activity
        Intent intent = new Intent(Mixed.this, MixedDigester.class);
        startActivity(intent);
        hasProceededToDigester = true;
    }


    // Method to populate spinner with items from Excel file
    private void populateSpinnerFromExcel(String fileName, Spinner spinner, List<String> spinnerItems) {
        File file = new File(getExternalFilesDir(null), fileName);
        FileInputStream fileInputStream = null;
        Workbook workbook = null;

        try {
            if (!file.exists()) {
                // If the file does not exist, create it
                return;
            }

            fileInputStream = new FileInputStream(file);
            Log.e(TAG, "Reading from Excel: ");
            workbook = new HSSFWorkbook(fileInputStream);

            // Fetch sheet at position '0' from the workbook
            Sheet sheet = workbook.getSheetAt(0);

            // Define start and end rows based on the spinner type
            int startRow, endRow;
            if (spinner == spinnerLive) {
                startRow = 2; // G3
                endRow = 9; // G10
            } else if (spinner == spinnerFud) {
                startRow = 2; // G3
                endRow = 11; // G12
            } else {
                return; // Return if spinner is neither spinnerLive nor spinnerFud
            }

            // Iterate through rows
            for (int i = startRow; i <= endRow; i++) {
                Row row = sheet.getRow(i);

                // Fetch cell based on spinner type
                Cell cell;
                if (spinner == spinnerLive) {
                    cell = row.getCell(7); // H column
                } else { // spinnerFud
                    cell = row.getCell(6); // G column
                }

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

    // Method to calculate total volatile solids for Livestock
    private void calculateTotalVolatileSolids() {
        String selectedItem = spinnerLive.getSelectedItem().toString();
        if (selectedItem == null || selectedItem.isEmpty()) { // Check if selectedItem is null or empty
            return; // Return early if no item is selected
        }
        double livestockNum = Double.parseDouble(livestockInputEditText.getText().toString());
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
        String selectedItem = spinnerLive.getSelectedItem().toString();
        double livestockNum = Double.parseDouble(livestockInputEditText.getText().toString());

        FeedstockCalculator calculator = new FeedstockCalculator();
        double resultL = calculator.calculateFeedstockAmount(selectedItem, livestockNum);
        // Round off the result to four decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        resultL = Double.parseDouble(decimalFormat.format(resultL));

        // Update UI with result
        totalWasteEditText.setText(String.valueOf(resultL));
    }

    private double calculateFoodWaste() {
        String selectedItemF = spinnerFud.getSelectedItem().toString();
        String FAinText = FAin.getText().toString().trim(); // Trim any leading or trailing whitespace

        // Declare foodNum and foodVolatile variables
        double foodNum;
        double foodVolatile;

        // Check if the FAin field is empty or null before parsing
        if (!FAinText.isEmpty()) {
            // Parse the FAin string into a numerical value
            foodNum = Double.parseDouble(FAinText);

            // Set foodVolatile based on selected item
            switch (selectedItemF) {
                case "Vegetable Waste":
                    foodVolatile = 0.16;
                    break;
                case "Rice Straw":
                    foodVolatile = 0.36;
                    break;
                case "Banana Peels (Fruit Waste)":
                    foodVolatile = 0.14;
                    break;
                case "Mixed Organic Waste":
                    foodVolatile = 0.26;
                    break;
                case "Cereal or Grains":
                    foodVolatile = 0.81;
                    break;
                case "Wheat Straw":
                    foodVolatile = 0.39;
                    break;
                case "Grass":
                    foodVolatile = 0.51;
                    break;
                case "Corn Stalk":
                    foodVolatile = 0.43;
                    break;
                case "Fat":
                    foodVolatile = 0.83;
                    break;
                case "Mixed Food Waste":
                    foodVolatile = 0.08;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + selectedItemF);
            }

            // Calculate food waste
            double resultF = foodNum * foodVolatile;
            // Round off the livestockVolatile value to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            resultF = Double.parseDouble(decimalFormat.format(resultF));

            // Update UI with food waste results
            totalVolatileSolidsEditTextF.setText(String.valueOf(resultF));

            totalWasteEditTextF.setText(String.valueOf(FAinText));

            // Set the value of foodVolatile to FoodWin EditText
            FoodWin.setText(String.valueOf(foodVolatile));

            // Return the result
            return resultF;
        } else {
            // Handle the case where the input field is empty
            // You can show a message to the user or perform any other appropriate action
            Toast.makeText(this, "Please enter a value for FAin.", Toast.LENGTH_SHORT).show();
            return 0; // Return 0 if input is empty
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        // Check if user has not proceeded to Digester activity
        return;
    }
}
