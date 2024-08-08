package com.example.biogassimulation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BiogasMixedTotal extends AppCompatActivity {
    private static final String TAG = "BiogasMixedTotal";

    private BluetoothAdapter mBluetoothAdapter;
    private final ActivityResultLauncher<Intent> bluetoothEnableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Bluetooth enabled, proceed
                    startBluetoothActivity1();
                    startNextActivity();
                } else {
                    // Bluetooth not enabled
                    Snackbar.make(findViewById(android.R.id.content), "Bluetooth not enabled", Snackbar.LENGTH_SHORT).show();
                }
            });
    private EditText sumofBiogasINPUT;
    private Button calculateButton,simulateButton;
    private boolean shouldReadTemperature = true;
    private BluetoothSocket mSocket;

//    private BiogasProduction.ConnectedThread mConnectedThread;
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_biogas_mixed_total);

        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Snackbar.make(findViewById(android.R.id.content), "Device does not support Bluetooth", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Find EditText field and button by ID
        sumofBiogasINPUT = findViewById(R.id.mixedbiogasResultEdittext);
        calculateButton = findViewById(R.id.mixedbiogasCalculate);
        simulateButton = findViewById(R.id.mixedbiogasButton);

        // Set OnClickListener for the button
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Retrieve biogas production values from Excel
                    Double[] biogasValues = readLastTwoBiogasValuesFromExcel(BiogasMixedTotal.this, "formula.xls");

                    // Calculate the sum of biogasInputValue and biogasInputValue2
                    double sum = 0.0;
                    for (Double value : biogasValues) {
                        if (value != null) {
                            sum += value;
                        }
                    }

                    DecimalFormat decimalFormat = new DecimalFormat("#.####");
                    double sumresult = Double.parseDouble(decimalFormat.format(sum));

                    // Display the sum in the EditText field
                    sumofBiogasINPUT.setText(String.valueOf(sumresult));

                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle the exception (e.g., display an error message)
                }
            }
        });
        simulateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });}

    private Double[] readLastTwoBiogasValuesFromExcel(BiogasMixedTotal context, String fileName) throws IOException {
        File file = new File(context.getExternalFilesDir(null), fileName);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            // Create instance having reference to .xls file
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(3);

            // Initialize variables to store retrieved data
            List<Double> biogasValuesList = new ArrayList<>();

            // Start iterating from the last row and stop when we have found two non-null values
            int rowCount = sheet.getLastRowNum();
            int foundValues = 0;
            for (int i = rowCount; i >= 0 && foundValues < 2; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(8); // Assuming column 8 (0-indexed)
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        double value = cell.getNumericCellValue();
                        biogasValuesList.add(value);
                        foundValues++;
                    }
                }
            }

            // Convert list to array
            Double[] biogasValues = biogasValuesList.toArray(new Double[0]);

            // Reverse the array to maintain the order
            Collections.reverse(Arrays.asList(biogasValues));

            // Return the retrieved data
            return biogasValues;
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation");
        builder.setMessage("Do you want to proceed to another activity?");
        builder.setPositiveButton("COOK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Proceed to another activity
                writeDataToExcel("formula.xls");
                startBluetoothActivity1();
                checkBluetoothEnabled1();
            }
        });
        builder.setNegativeButton("ELECTRICAL OUTPUT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeDataToExcel("formula.xls");
                startNextActivity();

                checkBluetoothEnabled2();
            }
        });
        builder.show();
    }
    private void stopReadingTemperature() {
        shouldReadTemperature = false;
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

            double biogasT = Double.parseDouble(sumofBiogasINPUT.getText().toString());
            String sumTitle = "SUM:";

            newRow.createCell(7).setCellValue(sumTitle);
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
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
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

    private void startNextActivity() {
        // Start the next activity without disconnecting from Bluetooth
        Intent intent = new Intent(BiogasMixedTotal.this, Results.class);
        String deviceAddress = "98:DA:50:02:F1:99"; // Replace this with your actual Bluetooth device address
        intent.putExtra(Results.EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }


    // Method to start Bluetooth-related activity
    private void startBluetoothActivity1() {
        // Start the bttesttwo activity with the Bluetooth device address as an extra
        Intent intent = new Intent(BiogasMixedTotal.this, bttesttwo.class);
        String deviceAddress = "98:DA:50:02:F1:99"; // Replace this with your actual Bluetooth device address
        intent.putExtra(bttesttwo.EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }

    private void checkBluetoothEnabled1() {
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth not enabled, request to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableLauncher.launch(enableBtIntent);
        } else {
            // Bluetooth already enabled, proceed with Bluetooth operations
            startBluetoothActivity1();
        }
    }
    private void checkBluetoothEnabled2() {
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth not enabled, request to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableLauncher.launch(enableBtIntent);
        } else {
            // Bluetooth already enabled, proceed with Bluetooth operations
            startNextActivity();
        }
    }




}
