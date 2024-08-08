package com.example.biogassimulation;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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


public class Digester extends AppCompatActivity {
    private static final String TAG = "Digester";
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";

    // Declare class-level variable for device address

    private EditText digesterDiameterEditText;
    private EditText digesterHeightEditText;
    private EditText plantVolumeEditText;
    private EditText digesterVolumeEditText;
    private EditText gasStorageVolumeEditText;
    private Spinner spinner; // Declare the Spinner variable

    // Declare class-level variables
    private double totalPlantVolume;
    private double plantVolume;
    private double digesterVolume;
    private double gasStorageVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digester);


        digesterDiameterEditText = findViewById(R.id.Diamin);
        digesterHeightEditText = findViewById(R.id.DHin);
        plantVolumeEditText = findViewById(R.id.DiVp);
        digesterVolumeEditText = findViewById(R.id.DiVd);
        gasStorageVolumeEditText = findViewById(R.id.DiVg);

        // Initialize the spinner by finding it in the layout XML
        spinner = findViewById(R.id.spinner);

        // Replace "formula.xls" with your actual file name
        populateSpinnerFromExcel("formula.xls");


        Button calculateButton = findViewById(R.id.DigesterButton);
        findViewById(R.id.DigesterButton).setEnabled(true);



// Define a Handler instance
        Handler mHandler = new Handler(Looper.getMainLooper());

// Inside onCreate method or wherever appropriate
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the values from digesterHeightEditText and digesterDiameterEditText fields
                String digesterHeightStr = digesterHeightEditText.getText().toString();
                String digesterDiameterStr = digesterDiameterEditText.getText().toString();

                // Check if the fields have values
                if (!digesterHeightStr.isEmpty() && !digesterDiameterStr.isEmpty()) {
                    // Parse the values from EditText fields
                    double digesterHeight = Double.parseDouble(digesterHeightStr);
                    double digesterDiameter = Double.parseDouble(digesterDiameterStr);

                    // Get the selected spinner item
                    String spinnerSelection = spinner.getSelectedItem().toString();

                    // Calculate volumes and update EditText fields
                    calculateVolumes(digesterHeight, digesterDiameter, spinnerSelection);

                    // Post a delayed message to the Handler after 5 seconds to show confirmation dialog

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showConfirmationDialog();
//                            calculateButton.setEnabled(false);

                        }
                    }, 3000);
                } else {
                    // If no fields have values, show a toast message indicating no data to save
                    Toast.makeText(Digester.this, "No data to save.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    // Method to show confirmation dialog
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Digester.this);
        builder.setMessage("Do you want to proceed?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the "Yes" button click event
                // Start BiogasProduction activity
                Intent intent = new Intent(Digester.this, BiogasProduction.class);
                intent.putExtra(EXTRA_DEVICE_ADDRESS, "98:DA:50:02:F1:99"); // Include the Bluetooth device address
                startActivity(intent);
                writeDataToExcel("formula.xls");
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the "No" button click event
                findViewById(R.id.DigesterButton).setEnabled(true);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        // Update the message of the dialog after showing it
        dialog.setMessage("Recorded");
    }



    // Method to populate Spinner from Excel file
    private void populateSpinnerFromExcel(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        FileInputStream fileInputStream = null;
        Workbook workbook = null;

        try {
            fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);

            // Get the first sheet (assuming it's the only sheet in the workbook)
            Sheet sheet = workbook.getSheetAt(0);

            // Initialize list to store spinner items
            List<String> spinnerItems = new ArrayList<>();

            // Read spinner selections from cells I3 to I6 and add them to the list
            for (int i = 2; i <= 5; i++) { // Rows I3-I6
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(8); // Column I (index 8)
                    if (cell != null) {
                        spinnerItems.add(cell.getStringCellValue());
                    }
                }
            }

            // Populate Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

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

    // Method to handle result from Calculator Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Retrieve calculated volumes from Calculator Activity
                double calculatedPlantVolume = data.getDoubleExtra("calculatedPlantVolume", 0.0);
                double calculatedDigesterVolume = data.getDoubleExtra("calculatedDigesterVolume", 0.0);
                double calculatedGasStorageVolume = data.getDoubleExtra("calculatedGasStorageVolume", 0.0);

                // Update EditText fields with calculated volumes
                plantVolumeEditText.setText(String.valueOf(calculatedPlantVolume));
                digesterVolumeEditText.setText(String.valueOf(calculatedDigesterVolume));
                gasStorageVolumeEditText.setText(String.valueOf(calculatedGasStorageVolume));

                }
            }
        }


    // Method to calculate volumes
    private void calculateVolumes(double digesterHeight, double digesterDiameter, String spinnerSelection) {
        DecimalFormat decimalFormat = new DecimalFormat("#.####");

        // Determine the formula to use based on the Spinner selection
        if (spinnerSelection.equals("FIXED DOME PLANT (HEMISPHERE DESIGN)")) {
            plantVolume = calculatePlantVolumeFixedDomeHemisphere(digesterDiameter);
            digesterVolume = calculateDigesterVolume(plantVolume);
            gasStorageVolume = calculateGasStorageVolume(plantVolume);

        } else if (spinnerSelection.equals("FIXED DOME PLANT (CHINESE DESIGN)")) {
            plantVolume = calculatePlantVolumeFixedDomeChinese(digesterDiameter);
            digesterVolume = calculateDigesterVolume(plantVolume);
            gasStorageVolume = calculateGasStorageVolume(plantVolume);

        } else if (spinnerSelection.equals("BALLOON DIGESTER")) {
            plantVolume = calculatePlantVolumeBalloonDigester(digesterDiameter, digesterHeight);
            digesterVolume = calculateGasStorageVolumeBalloon(plantVolume);
            gasStorageVolume = calculateDigesterVolumeBalloon(plantVolume);

        } else if (spinnerSelection.equals("FLOATING DRUM PLANT")) {
            digesterVolume = calculateDigesterVolumeFloatingDrum(digesterDiameter, digesterHeight);
            gasStorageVolume = calculateGasStorageVolumeFloatingDrum(digesterVolume);
            totalPlantVolume = calculateTotalPlantVolumeFloatingDrum(digesterVolume, gasStorageVolume);

        } else {
            // Default to a standard formula if no specific option is selected
            plantVolume = calculatePlantVolumeStandard(digesterDiameter);
            digesterVolume = calculateDigesterVolume(plantVolume);
            gasStorageVolume = calculateGasStorageVolume(plantVolume);
        }

        // Format volumes to four decimal places
        plantVolume = Double.parseDouble(decimalFormat.format(plantVolume));
        digesterVolume = Double.parseDouble(decimalFormat.format(digesterVolume));
        gasStorageVolume = Double.parseDouble(decimalFormat.format(gasStorageVolume));
        totalPlantVolume = Double.parseDouble(decimalFormat.format(totalPlantVolume));
        // Update EditText fields with calculated volumes
        if (spinnerSelection.equals("FLOATING DRUM PLANT")) {
            plantVolumeEditText.setText(String.valueOf(totalPlantVolume));
            digesterVolumeEditText.setText(String.valueOf(gasStorageVolume));
            gasStorageVolumeEditText.setText(String.valueOf(digesterVolume));
        } else {
            plantVolumeEditText.setText(String.valueOf(plantVolume));
            digesterVolumeEditText.setText(String.valueOf(digesterVolume));
            gasStorageVolumeEditText.setText(String.valueOf(gasStorageVolume));
        }
    }


    // Method to calculate Plant Volume using a standard formula
    private double calculatePlantVolumeStandard(double diameter) {
        return (2.0 / 3.0) * (Math.PI * Math.pow(diameter / 2, 3));

    }

    // Method to calculate Plant Volume for Fixed Dome Hemisphere design
    private double calculatePlantVolumeFixedDomeHemisphere(double diameter) {
        return (2.0 / 3.0) * (Math.PI * Math.pow(diameter / 2, 3));
    }

    // Method to calculate Digester Volume (Vd)
    private double calculateDigesterVolume(double plantVolume) {
        return plantVolume * 0.2;
    }

    // Method to calculate Gas Storage Volume (Vg)
    private double calculateGasStorageVolume(double plantVolume) {
        return plantVolume * 0.8;
    }

    // Method to calculate Plant Volume for other designs
    private double calculatePlantVolumeFixedDomeChinese(double diameter) {
        // Provided the appropriate formula for other designs
        return (Math.pow(diameter, 3)) / 2.2368;

    }

    // Method to calculate Digester Volume for other designs
// Method to calculate Plant Volume for Balloon Digester
    private double calculatePlantVolumeBalloonDigester(double digesterDiameter, double digesterHeight) {
        double radius = digesterDiameter / 2.0;
        return Math.PI * Math.pow(radius, 2) * digesterHeight;
    }

    // Method to calculate Digester Volume for Balloon Digester
    private double calculateDigesterVolumeBalloon(double plantVolume) {
        return plantVolume * 0.75; // 75% of the plant volume
    }

    // Method to calculate Gas Storage Volume for Balloon Digester
    private double calculateGasStorageVolumeBalloon(double plantVolume) {
        return plantVolume * 0.25; // 25% of the plant volume
    }
    // Method to calculate Digester Volume for floating drum design
    private double calculateDigesterVolumeFloatingDrum(double digesterDiameter, double digesterHeight) {
        double radius = digesterDiameter / 2.0;
        double baseArea = Math.PI * Math.pow(radius, 2);
        return baseArea * digesterHeight;
    }


    // Method to calculate Gas Storage Volume for floating drum design
    private double calculateGasStorageVolumeFloatingDrum(double digesterVolume) {
        return digesterVolume * (30.0 / 70.0) ;
    }

    // Method to calculate Total Plant Volume for floating drum design
    private double calculateTotalPlantVolumeFloatingDrum(double digesterVolume, double gasStorageVolume) {
        return digesterVolume + gasStorageVolume;}




    // Method to find the last row with non-empty data in columns 7-12 (G-L)
    private int getLastDataRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(1); // Checking column B (index 1)
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    return i;
                }
            }
        }
        return -1; // Sheet is empty
    }


    // Method to write data to Excel version 3.5
    private void writeDataToExcel(String fileName) {
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(2); // Assuming sheet 2 is used for Digester data

            // Find the last row with non-empty data in columns 7-12 (G-L)
            int lastRowNum = getLastDataRowIndex(sheet);

            // Create a new row after the last row with data
            Row newRow = sheet.createRow(lastRowNum + 1);
            // Get the current date and time
            String dateTime = getCurrentDateTime();

            // Get the selected animal type from the spinner
            String digestertype = spinner.getSelectedItem().toString();

            // Retrieve values from EditText fields
            double digesterVolume = Double.parseDouble(digesterVolumeEditText.getText().toString());
            double plantVolume = Double.parseDouble(plantVolumeEditText.getText().toString());
            double gasStorageVolume = Double.parseDouble(gasStorageVolumeEditText.getText().toString());
            double dH = Double.parseDouble((digesterHeightEditText.getText().toString()));
            double dD = Double.parseDouble((digesterDiameterEditText.getText().toString()));

            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(digestertype);//type of digester
            newRow.createCell(2).setCellValue(dH);//digester height
            newRow.createCell(3).setCellValue(dD);//digester diamter
            newRow.createCell(6).setCellValue(digesterVolume); // Column G
            newRow.createCell(4).setCellValue(plantVolume);   // Column H
            newRow.createCell(5).setCellValue(gasStorageVolume);     // Column I

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





}
