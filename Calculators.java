package com.example.biogassimulation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Calculators extends AppCompatActivity {

    private static final String TAG = "Calculators";
    private Spinner spinner2;
    private EditText volatileSolidsEditText;
    private EditText totalProductionEditText;
    private EditText volatileSolidsResultEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculators);

        spinner2 = findViewById(R.id.spinnerA);
        volatileSolidsEditText = findViewById(R.id.VolatileSolids);
        totalProductionEditText = findViewById(R.id.TotalProd);
        volatileSolidsResultEditText = findViewById(R.id.VolatileResult);

        // Replace "formula.xls" with your actual file name
        populateSpinnerFromExcel("formula.xls");

        // Find the button view
        Button calculateButton = findViewById(R.id.CalculateVS);

        // Set OnClickListener to the button
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected animal type from spinner
                String selectedAnimalType = spinner2.getSelectedItem().toString();

                // Get volatile solids and total production from EditText inputs
                double volatileSolids = Double.parseDouble(volatileSolidsEditText.getText().toString());
                double totalProduction = Double.parseDouble(totalProductionEditText.getText().toString());

                // Calculate the result (total production * volatile solids)
                double result = totalProduction * volatileSolids;

                // Display the result in the EditText
                volatileSolidsResultEditText.setText(String.valueOf(result));

                // Add a delay of 5 seconds before switching to another activity
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Start the new activity here
                        Intent intent = new Intent(Calculators.this, Digester.class);
                        intent.putExtra("animalType", selectedAnimalType);
                        intent.putExtra("volatileSolids", volatileSolids);
                        intent.putExtra("totalProduction", totalProduction);
                        intent.putExtra("result", result);
                        startActivity(intent);
                    }
                }, 3000); // Delay in milliseconds (5000 milliseconds = 5 seconds)
            }
        });

    }


    private void populateSpinnerFromExcel(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        FileInputStream fileInputStream = null;
        Workbook workbook = null;

        try {
            fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);

            // Get the first sheet (assuming it's the only sheet in the workbook)
            Sheet sheet = workbook.getSheetAt(0);

            // Initialize list to store Animal types
            List<String> animalTypes = new ArrayList<>();

            // Read from cells H2 to H11 and populate Spinner
            for (int i = 1; i <= 10; i++) { // Rows H2-H11
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(7); // Column H
                    if (cell != null) {
                        animalTypes.add(cell.getStringCellValue());
                    }
                }
            }

            // Populate Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner2.setAdapter(adapter);

        } catch (IOException e) {
            Log.e(TAG, "Error reading from Excel: ", e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (workbook != null) {
                    workbook.setHidden(true);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    private void createExcelFileIfNotExists(String fileName) {
        // Your existing code for creating Excel file goes here
    }

    private void writeDataToExcel(String fileName, String animalType, double volatileSolids, double totalProduction, double result) {
        // Open the existing Excel file or create a new one if not exists
        createExcelFileIfNotExists(fileName);

        File file = new File(getExternalFilesDir(null), fileName);
        FileInputStream fileInputStream = null;
        Workbook workbook = null;

        try {
            fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);

            // Get the first sheet (assuming it's the only sheet in the workbook)
            Sheet sheet = workbook.getSheetAt(1);

            // Find the last row with data
            int lastRowNum = sheet.getLastRowNum();

            // Iterate through rows from the last row upwards until we find a row with data
            int newRowNum = lastRowNum + 1;
            for (int i = lastRowNum; i >= 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(0) != null) { // Assuming the first cell must be non-empty for a row to be considered as having data
                    newRowNum = i + 1;
                    break;
                }
            }

            // Create a new row after the last row with data
            Row newRow = sheet.createRow(newRowNum);

            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(animalType);
            newRow.createCell(1).setCellValue(volatileSolids);
            newRow.createCell(2).setCellValue(totalProduction);
            newRow.createCell(3).setCellValue(result);

            // Save the workbook with the updated data
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();

            Toast.makeText(this, "Data saved to Excel", Toast.LENGTH_SHORT).show();

            // Reload the Excel file after 3 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Reload Excel file logic
                    populateSpinnerFromExcel(fileName);
                }
            }, 3000);

        } catch (IOException e) {
            Log.e(TAG, "Error writing to Excel: ", e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (workbook != null) {
                    workbook.setHidden(true);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }




    private void calculateVolatileSolids() {
        // Get the selected animal type from spinner
        String selectedAnimalType = spinner2.getSelectedItem().toString();

        // Get volatile solids and total production from EditText inputs
        double volatileSolids = Double.parseDouble(volatileSolidsEditText.getText().toString());
        double totalProduction = Double.parseDouble(totalProductionEditText.getText().toString());

        // Calculate the result (total production * volatile solids)
        double result = totalProduction * volatileSolids;


        // Pass the data to the Digester activity
        Intent intent = new Intent(this, Digester.class);
        intent.putExtra("animalType", selectedAnimalType);
        intent.putExtra("volatileSolids", volatileSolids);
        intent.putExtra("totalProduction", totalProduction);
        intent.putExtra("result", result);
        startActivity(intent);
    }




}
