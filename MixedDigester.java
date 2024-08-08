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

public class MixedDigester extends AppCompatActivity {
    private Spinner spinnerLive;
    private Spinner spinnerFud;
    private static final String TAG = "MixedDigester";
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private EditText digesterDiameterEditText;
    private EditText digesterHeightEditText;
    private EditText plantVolumeEditText;
    private EditText digesterVolumeEditText;
    private EditText gasStorageVolumeEditText;

    // Declare class-level variables
    private double totalPlantVolume;
    private double plantVolume;
    private double digesterVolume;
    private double gasStorageVolume;
    private EditText digesterDiameterEditText2;
    private EditText digesterHeightEditText2;
    private EditText plantVolumeEditText2;
    private EditText digesterVolumeEditText2;
    private EditText gasStorageVolumeEditText2;

    // Declare class-level variables
    private double totalPlantVolume2;
    private double plantVolume2;
    private double digesterVolume2;
    private double gasStorageVolume2;
    // Declare spinner items lists
    private List<String> spinnerItemsLive;
    private List<String> spinnerItemsFud;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_digester);

        digesterDiameterEditText = findViewById(R.id.Diamin);
        digesterHeightEditText = findViewById(R.id.DHin);
        plantVolumeEditText = findViewById(R.id.DiVp);
        digesterVolumeEditText = findViewById(R.id.DiVd);
        gasStorageVolumeEditText = findViewById(R.id.DiVg);

        digesterDiameterEditText2 = findViewById(R.id.Diamin2);
        digesterHeightEditText2 = findViewById(R.id.DHin2);
        plantVolumeEditText2 = findViewById(R.id.DiVp2);
        digesterVolumeEditText2 = findViewById(R.id.DiVd2);
        gasStorageVolumeEditText2 = findViewById(R.id.DiVg2);



        // Initialize UI components
        spinnerLive = findViewById(R.id.spinner2);
        spinnerFud = findViewById(R.id.spinner);
        Button calculate = findViewById(R.id.dmButton);

// Inside onCreate method or wherever appropriate
        spinnerItemsLive = new ArrayList<>();
        spinnerItemsFud = new ArrayList<>();


        // Populate spinners from Excel
        populateSpinnerFromExcel("formula.xls", spinnerLive, spinnerItemsLive);
        populateSpinnerFromExcel("formula.xls", spinnerFud, spinnerItemsFud);

        // Inside onCreate method or wherever appropriate
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the values from digesterHeightEditText and digesterDiameterEditText fields
                String digesterHeightStr = digesterHeightEditText.getText().toString();
                String digesterDiameterStr = digesterDiameterEditText.getText().toString();

                String digesterHeightStr2 = digesterHeightEditText.getText().toString();
                String digesterDiameterStr2 = digesterDiameterEditText.getText().toString();

                // Check if any of the fields are empty
                if (digesterHeightStr.isEmpty() || digesterDiameterStr.isEmpty()){
                    // Show a toast or message indicating that all fields are required
                    Toast.makeText(getApplicationContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
                    return; // Exit the onClick method to prevent further processing
                }
                    // Parse the values from EditText fields
                    double digesterHeight = Double.parseDouble(digesterHeightStr);
                    double digesterDiameter = Double.parseDouble(digesterDiameterStr);


                    // Get the selected spinner item
                    String spinnerSelectionFud = spinnerFud.getSelectedItem().toString();
                    String spinnerSelectionLive = spinnerFud.getSelectedItem().toString();

                    // Calculate volumes and update EditText fields
                    calculateVolumes(digesterHeight, digesterDiameter, spinnerSelectionFud);
                    writeDataToExcel("formula.xls");
                    writeDataToExcel("formula.xls");



                    // Perform calculations for the second set of input fields
//                    calculateVolumes2(digesterHeight2, digesterDiameter2, spinnerSelectionLive);

                    // Delay the start of the Bluetooth-related activity or any other activity
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MixedDigester.this, BiogasProduction.class);
                            intent.putExtra(BiogasProduction.INTENT_SWITCH_KEY, true);
                            intent.putExtra(EXTRA_DEVICE_ADDRESS, "98:DA:50:02:F1:99"); // Include the Bluetooth device address
                            startActivity(intent);
                        }
                    }, 3000);
                }

        });
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
                startRow = 2;
                endRow = 5;
            } else if (spinner == spinnerFud) {
                startRow = 2;
                endRow = 5;
            } else {
                return; // Return if spinner is neither spinnerLive nor spinnerFud
            }

            // Iterate through rows
            for (int i = startRow; i <= endRow; i++) {
                Row row = sheet.getRow(i);

                // Fetch cell based on spinner type
                Cell cell;
                if (spinner == spinnerLive) {
                    cell = row.getCell(8); // H column
                } else { // spinnerFud
                    cell = row.getCell(8); // G column
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


    private void calculateVolumes(double digesterHeight, double digesterDiameter, String spinnerSelection) {
        // Format volumes to four decimal places
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
        } else if (spinnerSelection.equals("FLOATING DRUM PLANT")) {
            digesterVolume = calculateDigesterVolumeFloatingDrum(digesterDiameter, digesterHeight);
            gasStorageVolume = calculateGasStorageVolumeFloatingDrum(digesterVolume);
            totalPlantVolume = calculateTotalPlantVolumeFloatingDrum(digesterVolume, gasStorageVolume);
        } else if (spinnerSelection.equals("BALLOON DIGESTER")) {
            plantVolume = calculatePlantVolumeBalloonDigester(digesterDiameter, digesterHeight);
            digesterVolume = calculateGasStorageVolumeBalloon(plantVolume);
            gasStorageVolume = calculateDigesterVolumeBalloon(plantVolume);
        } else {
            // Default to a standard formula if no specific option is selected
            plantVolume = calculatePlantVolumeStandard(digesterDiameter);
            digesterVolume = calculateDigesterVolume(plantVolume);
            gasStorageVolume = calculateGasStorageVolume(plantVolume);
        }

//        plantVolume = Double.parseDouble(decimalFormat.format(plantVolume));
//        digesterVolume = Double.parseDouble(decimalFormat.format(digesterVolume));
//        gasStorageVolume = Double.parseDouble(decimalFormat.format(gasStorageVolume));

        // Update EditText fields with calculated volumes
        plantVolumeEditText.setText(String.valueOf(plantVolume));
        digesterVolumeEditText.setText(String.valueOf(digesterVolume));
        gasStorageVolumeEditText.setText(String.valueOf(gasStorageVolume));

        // Update EditText fields with calculated volumes
        plantVolumeEditText2.setText(String.valueOf(plantVolume));
        digesterVolumeEditText2.setText(String.valueOf(digesterVolume));
        gasStorageVolumeEditText2.setText(String.valueOf(gasStorageVolume));
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

            // Update EditText fields with calculated volumes
            plantVolumeEditText2.setText(String.valueOf(plantVolume));
            digesterVolumeEditText2.setText(String.valueOf(digesterVolume));
            gasStorageVolumeEditText2.setText(String.valueOf(gasStorageVolume));
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
        return (30.0 / 70.0) * digesterVolume;
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
            String digestertype = spinnerFud.getSelectedItem().toString();

            // Retrieve values from EditText fields
            double digesterVolume = Double.parseDouble(digesterVolumeEditText.getText().toString());
            double plantVolume = Double.parseDouble(plantVolumeEditText.getText().toString());
            double gasStorageVolume = Double.parseDouble(gasStorageVolumeEditText.getText().toString());
            double dH = Double.parseDouble((digesterHeightEditText.getText().toString()));
            double dD = Double.parseDouble((digesterDiameterEditText.getText().toString()));

            // Update the cell values with the new inputs
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


