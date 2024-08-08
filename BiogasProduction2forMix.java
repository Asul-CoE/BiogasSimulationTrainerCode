package com.example.biogassimulation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import java.util.Locale;
import java.util.UUID;
public class BiogasProduction2forMix extends AppCompatActivity {
    private static final String TAG = "BiogasProduction2forMix";
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final String INTENT_SWITCH_KEY2 = "intent_switch";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
//    private ConnectThread mConnectThread;
//    private ConnectedThread mConnectedThread;

    private TextView mConnectionStatusTextView;

    private TextView mTempInputTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private EditText waterratioInput, waterResult, totalFeedstockVolumeInput, digesterVolumeInput,
            retentionTimeResult, volatileSolidResult, initialConcentrationResult, biogasResult,
            temperatureInput, yieldInput;

    // Declare totalFeedstockValue as a class member
    private double totalFeedstockValue;
    // Excel data loading task
    private boolean shouldReadTemperature = true;
    private final ActivityResultLauncher<Intent> bluetoothEnableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Bluetooth enabled, proceed
//                    startBluetoothActivity();
                } else {
                    // Bluetooth not enabled
                    Snackbar.make(findViewById(android.R.id.content), "Bluetooth not enabled", Snackbar.LENGTH_SHORT).show();
                }
            });
// Declare intent at the class level
    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biogas_production_2_for_mix);

        // Retrieve the Bluetooth device address from the intent
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        if (deviceAddress == null) {
            // Handle null address, show an error message or take appropriate action
            Toast.makeText(this, "Please connect to your Bluetooth Module", Toast.LENGTH_SHORT).show();
            // Finish the activity or handle it in another way
            finish(); // Example: This will finish the activity if the address is null
            return;
        }

        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Snackbar.make(findViewById(android.R.id.content), "Device does not support Bluetooth", Snackbar.LENGTH_SHORT).show();
            finish(); // Finish the activity
            return;
        }
        // Get the Bluetooth device object from the address
        mDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);

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
        new LoadDataFromExcelTask().execute();

        // Find the button by ID
        Button calculateButton = findViewById(R.id.calculateButton);
        findViewById(R.id.calculateButton).setEnabled(false);
        //calculate biogas after retention only
        Button calculateRetention = findViewById(R.id.calculateRetention);//retentiontime button

        calculateRetention.setOnClickListener(v ->{
            calculateretentionTime(totalFeedstockValue);
            findViewById(R.id.calculateButton).setEnabled(true);
            findViewById(R.id.calculateRetention).setEnabled(false);

        });
        intent = getIntent();
        Log.d("SwitchActivation", "1st BIOGAS PROD Intent Received: " + intent);

// Set an OnClickListener to the button
        calculateButton.setOnClickListener(v -> {
            findViewById(R.id.calculateRetention).setEnabled(true);
            findViewById(R.id.calculateButton).setEnabled(false);
            String temperatureStr = temperatureInput.getText().toString().trim();
            String yieldFactorStr = yieldInput.getText().toString().trim();


            if (temperatureStr.isEmpty()|| yieldFactorStr.isEmpty()) {
                Toast.makeText(this, "Please enter a temperature value", Toast.LENGTH_SHORT).show();
                return;
            }

            if (intent.hasExtra(INTENT_SWITCH_KEY2)) {
                boolean switchActivated = intent.getBooleanExtra(INTENT_SWITCH_KEY2, false);
                Log.d("SwitchActivation", "Switch activated: " + switchActivated);

                calculate(totalFeedstockValue);
                writeDataToExcel("formula.xls");

                new Handler().postDelayed(() -> {
                    Intent nextIntent = new Intent(BiogasProduction2forMix.this, BiogasMixedTotal.class);
                    nextIntent.putExtra(EXTRA_DEVICE_ADDRESS, "98:DA:50:02:F1:99");
                    startActivity(nextIntent);
                }, 3000);
            } else {
                Log.d("SwitchActivation", "Switch activation intent is null");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("INVALID");
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Check if all permissions are granted
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // All permissions granted, proceed with establishing the Bluetooth connection
                // Call the ConnectThread to retry establishing the connection
//                mConnectThread.start();
            } else {
                // Permission denied by user, display a message or handle the denial appropriately
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LoadDataFromExcelTask extends AsyncTask<Void, Void, Void> {
        private double volatileSolidResultValue;
        private double digesterVolume;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                readExcelFromStorage(BiogasProduction2forMix.this, "formula.xls");
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
            Intent intent = new Intent(BiogasProduction2forMix.this, BiogasProduction2forMix.class);
            startActivity(intent);
        }

        //A---READING DATA OPERATION
        private void readExcelFromStorage(Context context, String fileName) throws IOException {
            File file = new File(context.getExternalFilesDir(null), fileName);
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                Log.e("TAG", "Reading from Excel: " + file);

                // Create instance having reference to .xls file
                Workbook workbook = new HSSFWorkbook(fileInputStream);

                // Fetch sheet at position 1 from the workbook
                Sheet sheet = workbook.getSheetAt(1);

                boolean volatileSolidValueObtained = false; // Flag to track if volatile solid value has been obtained

                // Iterate through rows in reverse order
                for (int i = sheet.getLastRowNum()-1; i >= 0; i--) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        // Check if the cell in column 5 (index 4) is numeric
                        Cell cell = row.getCell(5);
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            // Assuming volatile solid result is in column 5 (0-indexed) of sheet index 1
                            volatileSolidResultValue = cell.getNumericCellValue();
                            volatileSolidValueObtained = true; // Set flag to true
                            break; // Exit the loop after finding the first numeric value in column 5
                        }
                    }
                }

                // Check if volatile solid value was obtained and set totalFeedstockValue from the second-to-last row
                if (volatileSolidValueObtained) {
                    int rowIndex = sheet.getLastRowNum() - 1;
                    if (rowIndex >= 0) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            Cell cell = row.getCell(4);
                            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                                totalFeedstockValue = cell.getNumericCellValue();
                            }
                        }
                    }
                }

                // Fetch digester volume from the second sheet (index 2)
                sheet = workbook.getSheetAt(2);
                for (Row row : sheet) {
                    if (row.getRowNum() > 0) { // Exclude header row
                        Cell cell = row.getCell(5);
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            digesterVolume = cell.getNumericCellValue();
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
            double totalFeedstockVolumeValueF = (totalFeedstockValue + waterResultValue) / 1000;
            totalFeedstockVolumeInput.setText(String.valueOf(totalFeedstockVolumeValueF));

            // Calculate retentionTimeResult
            double retentionTimeResultValue = digesterVolume / totalFeedstockVolumeValueF;
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValue));

            // Round off the calculated values to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            waterResultValue = Double.parseDouble(decimalFormat.format(waterResultValue));
            totalFeedstockVolumeValueF = Double.parseDouble(decimalFormat.format(totalFeedstockVolumeValueF));
            retentionTimeResultValue = Double.parseDouble(decimalFormat.format(retentionTimeResultValue));

            // Set the rounded values to respective TextViews
            waterResult.setText(String.valueOf(waterResultValue));
            totalFeedstockVolumeInput.setText(String.valueOf(totalFeedstockVolumeValueF));
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValue));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input values", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            // Handle parsing errors if necessary
        }
    }
    private void calculate(double totalFeedstockValue) { // Receive totalFeedstockValue as a parameter
        // Get input values
        String waterratioStr = waterratioInput.getText().toString().trim();
        String volatileSolidResultTotal = volatileSolidResult.getText().toString().trim();
        String digesterVolumeStr = digesterVolumeInput.getText().toString().trim();
        String yieldFactorStr = yieldInput.getText().toString().trim();
        String temperatureStr = temperatureInput.getText().toString().trim(); // Add temperature input
        String totalFeedstockVolumeValueF = totalFeedstockVolumeInput.getText().toString().trim();

        // Check if temperature input is empty
        if (temperatureStr.isEmpty()) {
            // Show an error message or handle empty temperature input
            Toast.makeText(this, "Please enter a temperature value", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with calculations
        }

        // Parse temperature value
        double temperature;
        try {
            temperature = Double.parseDouble(temperatureStr);
        } catch (NumberFormatException e) {
            // Handle parsing errors for temperature input
            Toast.makeText(this, "Invalid temperature value", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with calculations
        }

        // Check if other input values are empty
        if (waterratioStr.isEmpty() || volatileSolidResultTotal.isEmpty() ||
                digesterVolumeStr.isEmpty() || yieldFactorStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid input values", Toast.LENGTH_SHORT).show();

            return; // Do not proceed with calculations
        }

        try {
            double volatileSolidResultValue = Double.parseDouble(volatileSolidResultTotal);
            double digesterVolume = Double.parseDouble(digesterVolumeStr);
            double yieldFactor = Double.parseDouble(yieldFactorStr);
            double totalFeedstockVolumeValue = Double.parseDouble(totalFeedstockVolumeValueF);


            // Calculate retentionTimeResult
            double retentionTimeResultValueF = (digesterVolume / totalFeedstockVolumeValue);
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValueF));

            // Calculate initialConcentrationResult
            double initialConcentrationResultValueF = (volatileSolidResultValue / totalFeedstockVolumeValue);
            initialConcentrationResult.setText(String.valueOf(initialConcentrationResultValueF));

            // Calculate biogasResult
            double biogasResultValueF = (digesterVolume * initialConcentrationResultValueF * yieldFactor) / 1000;
            biogasResult.setText(String.valueOf(biogasResultValueF));

            // Round off the calculated values to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            initialConcentrationResultValueF = Double.parseDouble(decimalFormat.format(initialConcentrationResultValueF));
            biogasResultValueF = Double.parseDouble(decimalFormat.format(biogasResultValueF));
            retentionTimeResult.setText(String.valueOf(retentionTimeResultValueF));
            initialConcentrationResult.setText(String.valueOf(initialConcentrationResultValueF));
            biogasResult.setText(String.valueOf(biogasResultValueF));


        } catch (NumberFormatException e) {
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

}
