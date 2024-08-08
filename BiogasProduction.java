package com.example.biogassimulation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

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
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public class BiogasProduction extends AppCompatActivity {
    private static final String TAG = "BiogasProduction";
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private TextView mConnectionStatusTextView;

    private TextView mTempInputTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    // Define the key for the switch activation intent
    public static final String INTENT_SWITCH_KEY = "intent_switch";


    private EditText waterratioInput, waterResult, totalFeedstockVolumeInput, digesterVolumeInput,
            retentionTimeResult, volatileSolidResult, initialConcentrationResult, biogasResult,
            temperatureInput, yieldInput;

    // Declare totalFeedstockValue as a class member
    private double totalFeedstockValue;
    // Excel data loading task
    private boolean shouldReadTemperature = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biogas_production);


        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Snackbar.make(findViewById(android.R.id.content), "Device does not support Bluetooth", Snackbar.LENGTH_SHORT).show();
            return;
        }

        waterratioInput = findViewById(R.id.watertowasteratioINPUT);
        waterResult = findViewById(R.id.waterresult);
        totalFeedstockVolumeInput = findViewById(R.id.TotalFeedstockVolumeINPUT);
        digesterVolumeInput = findViewById(R.id.DigesterVolumeFInput);
        retentionTimeResult = findViewById(R.id.RetentionResult);
        volatileSolidResult = findViewById(R.id.VolatileSolidResult);
        initialConcentrationResult = findViewById(R.id.ICVSTitle);
        biogasResult = findViewById(R.id.biogasproductionresult);
        ////////////////////////////////////////////////
        temperatureInput = findViewById(R.id.TempInput);
        yieldInput = findViewById(R.id.YieldINPUT);
        mConnectionStatusTextView = findViewById(R.id.TempInput);
        mTempInputTextView = findViewById(R.id.TempInput);

        // Retrieve the Bluetooth device address from the intent
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        if (deviceAddress == null) {
            // Handle null address, show an error message or take appropriate action
//            Toast.makeText(this, "Please connect to your Bluetooth Module", Toast.LENGTH_SHORT).show();
            // Finish the activity or handle it in another way
            finish(); // Example: This will finish the activity if the address is null
            return;
        }
        // Start background task to load data from Excel
        new LoadDataFromExcelTask().execute();


        // Find the button by ID
        Button calculateButton = findViewById(R.id.calculateButton);
        findViewById(R.id.calculateButton).setEnabled(false);
        //calculate biogas after retention only
        Button calculateRetention = findViewById(R.id.calculateRetention);//retentiontime button



        if (deviceAddress == null) {
            // Device address not provided, finish the activity
            Toast.makeText(this, "Device address not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish(); // Finish the activity
            return;
        }

        calculateRetention.setOnClickListener(v ->{
            calculateretentionTime(totalFeedstockValue);
            findViewById(R.id.calculateButton).setEnabled(true);
            findViewById(R.id.calculateRetention).setEnabled(false);

        });

// Set an OnClickListener to the button
        calculateButton.setOnClickListener(v -> {
            findViewById(R.id.calculateRetention).setEnabled(true);
            findViewById(R.id.calculateButton).setEnabled(false);

            // Get the temperature input value
            String temperatureStr = temperatureInput.getText().toString().trim();
            String yieldFactorStr = yieldInput.getText().toString().trim();
            // Retrieve the intent that started this activity
            Intent intentSwitch = getIntent();

            // Check if the temperature field is empty
            if (temperatureStr.isEmpty()|| yieldFactorStr.isEmpty()) {
                // Show an error message or handle it appropriately
                Toast.makeText(this, "Please enter a temperature value", Toast.LENGTH_SHORT).show();
                return; // Exit the OnClickListener without proceeding
            }

            // Log to check if the switch activation intent was received
            Log.d("SwitchActivation", "MixedDigester Intent Received: " + intentSwitch);

            if(intentSwitch.hasExtra(INTENT_SWITCH_KEY)) {
                // The intent signifies switch activation
                boolean switchActivated = intentSwitch.getBooleanExtra(INTENT_SWITCH_KEY, false);

                // Log to check if the switch activation is turned on or off
                Log.d("SwitchActivation", "Switch activated: " + switchActivated);
                calculate(totalFeedstockValue); // Pass totalFeedstockValue as a parameter

                ///
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(BiogasProduction.this, BiogasProduction2forMix.class);
                        intent.putExtra(BiogasProduction2forMix.INTENT_SWITCH_KEY2, true);
                        intent.putExtra(EXTRA_DEVICE_ADDRESS, "98:DA:50:02:F1:99"); // Include the Bluetooth device address
                        writeDataToExcel("formula.xls");
                        startActivity(intent);
                    }
                }, 3000);

            } else {
                // Log to indicate that switch activation intent is null
                Log.d("SwitchActivation", "Switch activation intent is null");

                // Call the calculate method when the button is clicked
                calculate(totalFeedstockValue); // Pass totalFeedstockValue as a parameter

                // Create a dialog for confirming whether to proceed to simulation
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Proceed to Simulation?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Proceed to the next activity

                                // After writing to Excel and proceeding, show another dialog
                                AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(BiogasProduction.this);
                                secondDialogBuilder.setMessage("Choose simulation type:")
                                        .setPositiveButton("ELECTRICAL OUTPUT", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                writeDataToExcel("formula.xls");
                                                // Stop reading temperature after writing to Excel
//                                                stopReadingTemperature();
                                                // Disconnect from Bluetooth
//                                                disconnectFromBluetooth();
                                                // Handle the positive action for the second dialog
//                                                finish();
                                                startBluetoothActivityElec();
                                            }
                                        })
                                        .setNegativeButton("COOK!", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

//                                                stopReadingTemperature();
                                                // Disconnect from Bluetooth
//                                                disconnectFromBluetooth();
                                                AlertDialog.Builder thirdDialogBuilder = new AlertDialog.Builder(BiogasProduction.this);
                                                thirdDialogBuilder.setMessage("Proceeding to COOKING SIMULATION:");
                                                writeDataToExcel("formula.xls");
                                                // Handle the positive action for the second dialog
                                                startBluetoothActivitybt();
                                            }
                                        });

                                // Create and show the second dialog
                                AlertDialog secondDialog = secondDialogBuilder.create();
                                secondDialog.show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog, do nothing or handle it accordingly
                            }
                        });
                // Create the AlertDialog object and show it
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });



    }

    private class LoadDataFromExcelTask extends AsyncTask<Void, Void, Void> {
        private double volatileSolidResultValue;
        private double digesterVolume;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                readExcelFromStorage(BiogasProduction.this, "formula.xls");
            } catch (IOException e) {
                e.printStackTrace();
                // Handle file loading errors
            }
            return null;
        }

        //M---READ DATA PRINTS UPON INITIATION
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // Set volatileSolidResultValue and digesterVolumeInput
            volatileSolidResult.setText(String.valueOf(volatileSolidResultValue));
            digesterVolumeInput.setText(String.valueOf(digesterVolume));

            // Start the activity upon successful data loading
            Intent intent = new Intent(BiogasProduction.this, BiogasProduction.class);
            startActivity(intent);
        }

        //A---READING DATA OPERATION
        private void readExcelFromStorage(Context context, String fileName) throws IOException {
            File file = new File(context.getExternalFilesDir(null), fileName);
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                Log.e("TAG", "Reading from Excel: " + file);

                // Create instance having reference to .xls file
                Workbook workbook = new HSSFWorkbook(fileInputStream);

                // Fetch sheet at position 'i' from the workbook
                Sheet sheet = workbook.getSheetAt(1);

                boolean volatileSolidValueObtained = false; // Flag to track if volatile solid value has been obtained

                // Iterate through rows in reverse order
                for (int i = sheet.getLastRowNum(); i >= 0; i--) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        // Iterate through all the cells in a row
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            // Check cell type and format accordingly
                            switch (cell.getCellType()) {
                                case NUMERIC:
                                    // Assuming volatile solid result is in column 5 (0-indexed) of sheet index 1
                                    if (sheet.getWorkbook().getSheetIndex(sheet) == 1 && cell.getColumnIndex() == 5) {
                                        volatileSolidResultValue = cell.getNumericCellValue();
                                        volatileSolidValueObtained = true; // Set flag to true
                                    }
                                    break;
                                case STRING:
                                    // Handle string values if needed
                                    break;
                            }
                            switch (cell.getCellType()) {
                                case NUMERIC:
                                    // Assuming volatile solid result is in column 5 (0-indexed) of sheet index 1
                                    if (sheet.getWorkbook().getSheetIndex(sheet) == 1 && cell.getColumnIndex() == 4) {
                                        totalFeedstockValue = cell.getNumericCellValue();
                                        volatileSolidValueObtained = true; // Set flag to true
                                    }
                                    break;
                                case STRING:
                                    // Handle string values if needed
                                    break;
                            }
                        }
                        // If volatile solid value obtained, switch to sheet index 2
                        if (volatileSolidValueObtained) {
                            sheet = workbook.getSheetAt(2);
                            break; // Exit the loop after switching sheets
                        }
                    }
                }

                // Read digester volume from the new sheet (index 2)
                for (Row row : sheet) {
                    if (row.getRowNum() > 0) { // Exclude header row
                        // Iterate through all the cells in a row
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            // Check cell type and format accordingly
                            switch (cell.getCellType()) {
                                case NUMERIC:
                                    // Assuming digester volume is in column 5 (0-indexed) of sheet index 2
                                    if (sheet.getWorkbook().getSheetIndex(sheet) == 2 && cell.getColumnIndex() == 5) {
                                        digesterVolume = cell.getNumericCellValue();
                                    }
                                    break;
                                case STRING:
                                    // Handle string values if needed
                                    break;
                            }
                        }
                    }
                }

            }
        }
    }
    private void calculateretentionTime(double totalFeedstockValue) {
        // Get input values
        String waterratioStr = waterratioInput.getText().toString().trim();
        String volatileSolidResultTotal = volatileSolidResult.getText().toString().trim();
        String digesterVolumeStr = digesterVolumeInput.getText().toString().trim();
        String temperatureStr = temperatureInput.getText().toString().trim(); // Add temperature input

        // Check if any input values are empty
        if (waterratioStr.isEmpty() || volatileSolidResultTotal.isEmpty() || digesterVolumeStr.isEmpty() || temperatureStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid input values", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with calculations if any input is missing
        }

        try {
            double waterratio = Double.parseDouble(waterratioStr);
            double volatileSolidResultValue = Double.parseDouble(volatileSolidResultTotal);
            double digesterVolume = Double.parseDouble(digesterVolumeStr);
            double temperature = Double.parseDouble(temperatureStr); // Parse temperature

            // Calculate waterResult
            double waterResultValue = waterratio * totalFeedstockValue;
            waterResult.setText(String.valueOf(waterResultValue));

            // Calculate totalFeedstockVolume
            double totalFeedstockVolumeValue = (totalFeedstockValue + waterResultValue) / 1000;
            totalFeedstockVolumeInput.setText(String.valueOf(totalFeedstockVolumeValue));

            // Calculate retentionTimeResult
            double retentionTimeResultValue = digesterVolume / totalFeedstockVolumeValue;
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValue));

            // Round off the calculated values to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            waterResultValue = Double.parseDouble(decimalFormat.format(waterResultValue));
            totalFeedstockVolumeValue = Double.parseDouble(decimalFormat.format(totalFeedstockVolumeValue));
            retentionTimeResultValue = Double.parseDouble(decimalFormat.format(retentionTimeResultValue));

            // Set the rounded values to respective TextViews
            waterResult.setText(String.valueOf(waterResultValue));
            totalFeedstockVolumeInput.setText(String.valueOf(totalFeedstockVolumeValue));
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValue));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input values", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            // Handle parsing errors if necessary
        }
    }

    private void calculate(double totalFeedstockValue) {
        // Get input values
        String volatileSolidResultTotal = volatileSolidResult.getText().toString().trim();
        String digesterVolumeStr = digesterVolumeInput.getText().toString().trim();
        String yieldFactorStr = yieldInput.getText().toString().trim();
        String retentionTimeResultValue = retentionTimeResult.getText().toString().trim();
        String totalFeedstockVolumeValue = totalFeedstockVolumeInput.getText().toString().trim();
       // Check if any input values are empty
        if (volatileSolidResultTotal.isEmpty() || digesterVolumeStr.isEmpty() || yieldFactorStr.isEmpty() || retentionTimeResultValue.isEmpty()) {
            Toast.makeText(this, "Please enter valid input values", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with calculations if any input is missing
        }

        try {
            double volatileSolidResultValue = Double.parseDouble(volatileSolidResultTotal);
            double digesterVolume = Double.parseDouble(digesterVolumeStr);
            double yieldFactor = Double.parseDouble(yieldFactorStr);
            double totalFeedstockVolume = Double.parseDouble(totalFeedstockVolumeValue);

            // Calculate initialConcentrationResult
            double initialConcentrationResultValue = volatileSolidResultValue / totalFeedstockVolume;
            initialConcentrationResult.setText(String.valueOf(initialConcentrationResultValue));

            // Calculate biogasResult
            double biogasResultValue = (digesterVolume * initialConcentrationResultValue * yieldFactor) / 1000;
            biogasResult.setText(String.valueOf(biogasResultValue));

            // Round off the calculated values to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            initialConcentrationResultValue = Double.parseDouble(decimalFormat.format(initialConcentrationResultValue));
            biogasResultValue = Double.parseDouble(decimalFormat.format(biogasResultValue));

            // Set the rounded values to respective TextViews
            initialConcentrationResult.setText(String.valueOf(initialConcentrationResultValue));
            biogasResult.setText(String.valueOf(biogasResultValue));

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input values", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            // Handle parsing errors if necessary
        }
    }


    // Method to find the last row with non-empty data in BiogasProduction
    private int getLastDataRowIndex(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(0); // Checking column A (index 0)
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    return i;
                }
            }
        }
        return -1; // Sheet is empty
    }


    private void startBluetoothActivitybt() {
        // Start the bttesttwo activity with the Bluetooth device address as an extra
        Intent intent = new Intent(BiogasProduction.this, bttesttwo.class);
        String deviceAddress = "98:DA:50:02:F1:99"; // Replace this with your actual Bluetooth device address
        intent.putExtra(bttesttwo.EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }
    private void startBluetoothActivityElec() {
        // Start the bttesttwo activity with the Bluetooth device address as an extra
        Intent intent = new Intent(BiogasProduction.this, Results.class);
        String deviceAddress = "98:DA:50:02:F1:99"; // Replace this with your actual Bluetooth device address
        intent.putExtra(bttesttwo.EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);


    }

    private void writeDataToExcel(String fileName) {
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(3); // Assuming sheet 2 is used for BiogasProduction data

            // Find the last row with non-empty data in columns 7-12 (G-L)
            int lastRowNum = getLastDataRowIndex(sheet);

            // Create a new row after the last row with data
            Row newRow = sheet.createRow(lastRowNum + 1);
            // Get the current date and time
            String dateTime = getCurrentDateTime();


            // Get the selected temperature from the field
            double temperature = Double.parseDouble(temperatureInput.getText().toString().trim());
            // Get the selected yieldfactor from the field
            double yieldfactor = Double.parseDouble(yieldInput.getText().toString().trim());
            // Retrieve values from the rest of the fields
            double waterR = Double.parseDouble(waterratioInput.getText().toString());
            double waterInKG = Double.parseDouble(waterResult.getText().toString());
            double totalFVm3 = Double.parseDouble(volatileSolidResult.getText().toString());
            double retentionT = Double.parseDouble((retentionTimeResult.getText().toString()));
            double concentration = Double.parseDouble((initialConcentrationResult.getText().toString()));
            double biogasT = Double.parseDouble(biogasResult.getText().toString());


            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(temperature);
            newRow.createCell(2).setCellValue(yieldfactor);
            newRow.createCell(3).setCellValue(waterR);
            newRow.createCell(4).setCellValue(waterInKG);
            newRow.createCell(5).setCellValue(totalFVm3);
            newRow.createCell(6).setCellValue(retentionT);
            newRow.createCell(7).setCellValue(concentration);
            newRow.createCell(8).setCellValue(biogasT);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the Bluetooth socket and release resources
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
