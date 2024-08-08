package com.example.biogassimulation;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Results extends AppCompatActivity {
    // Bluetooth variables
    private OutputStream mOutputStream;

    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 1;

    private EditText energyMJInput;
    private CountDownTimer countDownTimer;
    private ImageView pulsingImage;

    private OutputStream outputStream;
    private boolean isTimerRunning = false;
    private EditText electricaloutputwithoutlossesInput;
    private EditText electricaloutputOnlyInput;
    private EditText loadWInput;
    private EditText loadkWInput;
    private EditText hoursRunningInput;
    private EditText powerOutputIn;
    private EditText biogasInput;
    private static final String TAG = "Results";
    private TextClock countdownTimer;

    private Animation pulseAnimation;
    private final static int MESSAGE_READ = 2;
    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_results);
        Log.d(TAG, "onCreate: Activity created successfully");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Handle devices without Bluetooth support
            // You can show a Toast or log a message here
            return;
        }
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        if (deviceAddress == null) {
            Toast.makeText(this, "Bluetooth device address is null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        connectToArduino(deviceAddress);

        // Initialize EditText fields
        biogasInput = findViewById(R.id.biogasINPUT);
        energyMJInput = findViewById(R.id.energyMJ);
        electricaloutputwithoutlossesInput = findViewById(R.id.electricalOWL);
        electricaloutputOnlyInput = findViewById(R.id.electricalOutput);
        loadWInput = findViewById(R.id.loadW);
        loadkWInput = findViewById(R.id.loadkW);
        hoursRunningInput = findViewById(R.id.hoursRunning);
        powerOutputIn = findViewById(R.id.powerOutField);
        // Initialize countdownTimer TextView
        countdownTimer = findViewById(R.id.countdownTimer);

        pulsingImage = findViewById(R.id.pulsingImageView);
// Find the "Stop" button and set its click listener
        findViewById(R.id.eSimulate).setOnClickListener(v -> {
            findViewById(R.id.eSimulate).setEnabled(false);
            connectToArduino(deviceAddress);

            // Delay between operations
            long delayBetweenOperations = 500; // 1 second delay (adjust as needed)

            // Connect to Arduino after a delay
            new Handler().postDelayed(() -> {
                connectToArduino(deviceAddress);
            }, delayBetweenOperations * 1);

            // Start biogas production simulation after a delay
            new Handler().postDelayed(() -> {
                sendDataToArduino("1",5000000);

            }, delayBetweenOperations * 3); // Double the delay for the second operation

            // Send data to Arduino after a delay
            new Handler().postDelayed(() -> {
                startBiogasProductionSimulation();
            }, delayBetweenOperations * 4); // Triple the delay for the third operation
        });


// Find the "Stop" button and set its click listener
        findViewById(R.id.stop).setOnClickListener(v -> {
            // Delay between operations
            long delayBetweenOperations = 500; // 1 second delay (adjust as needed)

            // Connect to Arduino after a delay
            new Handler().postDelayed(() -> {
                connectToArduino(deviceAddress);
                sendDataToArduino("0",5000000);
                    }, delayBetweenOperations*2);
            findViewById(R.id.eSimulate).setEnabled(true);

            stopCountdownTimer();

        });


        // Instantiate LoadDataFromExcelTask and execute it
        LoadDataFromExcelTask loadDataFromExcelTask = new LoadDataFromExcelTask(this, biogasInput);
        loadDataFromExcelTask.execute();

        // Load animation
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulsing_animation);

        //Exit
        findViewById(R.id.resultExit).setOnClickListener(v -> {
            // Navigate back to the main activity
            startActivity(new Intent(Results.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish(); // Finish the current activity
        });

        findViewById(R.id.resultNew).setOnClickListener(v -> {
            // Build an AlertDialog with three choices
            new AlertDialog.Builder(Results.this)
                    .setTitle("Select Waste Type")
                    .setItems(new String[]{"Livestock Waste", "Food Waste", "Mix"}, (dialog, which) -> {
                        // Handle the selection based on the selected item index
                        switch (which) {
                            case 0:
                                // Livestock Waste selected
                                startActivity(new Intent(Results.this, Livestock.class));
                                finish(); // Finish the current activity
                                break;
                            case 1:
                                // Food Waste selected
                                startActivity(new Intent(Results.this, Food.class));
                                finish(); // Finish the current activity
                                break;
                            case 2:
                                // Mix selected
                                startActivity(new Intent(Results.this, Mixed.class));
                                finish(); // Finish the current activity
                                break;
                        }
                        dialog.dismiss(); // Dismiss the dialog
                    })
                    .create()
                    .show(); // Show the AlertDialog
        });
        findViewById(R.id.resultSave).setOnClickListener(v -> {
            // Save data to Excel
            writeDataToExcel("formula.xls");

            // Show a notification that data is saved
            Toast.makeText(this, "Data saved successfully.", Toast.LENGTH_SHORT).show();

            // Disable the button after saving
            v.setEnabled(false);
        });

    }

    private void startBiogasProductionSimulation() {
        // Retrieve input values and perform calculations
        String biogasproduction = biogasInput.getText().toString().trim();
        String loadInputText = loadWInput.getText().toString().trim();
        double hoursRunning;
        try {
            // Parse input values to doubles
            double totalBiogasProduction = Double.parseDouble(biogasproduction);
            double loadInput = Double.parseDouble(loadInputText);

            // Calculate load in kW
            double loadkW = loadInput / 1000;

            // Calculate energy in MJ
            double energyMJ = totalBiogasProduction * 22;

            // Calculate electrical output without losses
            double electricaloutputwithoutlosses = energyMJ / 3.6;

            // Calculate electrical output only
            double electricaloutputOnly = electricaloutputwithoutlosses * 0.35;

            // Calculate hours running
            hoursRunning = electricaloutputOnly/loadkW;

            // Calculate power output
            double powerOutput = electricaloutputOnly/hoursRunning;

            // Format volumes to four decimal places
            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            energyMJ = Double.parseDouble(decimalFormat.format(energyMJ));
            electricaloutputwithoutlosses = Double.parseDouble(decimalFormat.format(electricaloutputwithoutlosses));
            electricaloutputOnly = Double.parseDouble(decimalFormat.format(electricaloutputOnly));
            powerOutput = Double.parseDouble(decimalFormat.format(powerOutput));


            // Fill out the respective EditText fields
            energyMJInput.setText(String.valueOf(energyMJ));
            electricaloutputwithoutlossesInput.setText(String.valueOf(electricaloutputwithoutlosses));
            electricaloutputOnlyInput.setText(String.valueOf(electricaloutputOnly));
            hoursRunningInput.setText(String.valueOf(hoursRunning));
            powerOutputIn.setText(String.valueOf(powerOutput));

            // Update the loadkWInput EditText with the calculated value
            loadkWInput.setText(String.valueOf(loadkW));

            // Start countdown timer with initial remaining biogas
            if (!(hoursRunning <= 0)) {
                long durationInMillis = (long) (hoursRunning * 3600 * 1000); // Convert hours to milliseconds
                startCountdownTimer(durationInMillis/60);//show the time minutes basis
            } else {
                // Stop the countdown timer if remaining biogas is zero or negative

                stopCountdownTimer();

            }

        } catch (NumberFormatException e) {
            // Handle the case where parsing fails
            Log.e(TAG, "Error parsing input values: " + e.getMessage());
            Toast.makeText(this, "Error: Unable to parse input values", Toast.LENGTH_SHORT).show();
            e.printStackTrace(); // Print the stack trace for debugging
        } catch (Exception e) {
            // Handle any other exceptions
            Log.e(TAG, "Error in startBiogasProductionSimulation: " + e.getMessage());
            e.printStackTrace(); // Print the stack trace for debugging

        }
    }
    private void startCountdownTimer(long remainingTimeInMillis) {
        // Assuming calculation was successful, disable the button
        findViewById(R.id.eSimulate).setEnabled(false);

        // Create a countdown timer to update the countdown timer text every second
        countDownTimer = new CountDownTimer(remainingTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {

                // Convert milliseconds to seconds
                int totalSeconds = (int) (millisUntilFinished / 1000);

                // Convert total seconds to minutes, remaining seconds, and remaining minutes
                int totalMinutes = totalSeconds / 60;
                int remainingSeconds = totalSeconds % 60;
                int remainingMinutes = totalMinutes % 60;

                // Convert remaining minutes to hours and remaining hours
                int totalHours = totalMinutes / 60;
                int remainingHours = totalHours % 60;

                // Format remaining time as HH:mm:ss
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", remainingHours, remainingMinutes, remainingSeconds);
                countdownTimer.setText(formattedTime);


                // Define the animation duration in milliseconds
                int animationDuration = 500; // Half a second

                // Set the animation duration
                pulseAnimation.setDuration(animationDuration);

                // Start animation only if the timer is running
                if (isTimerRunning) {
                    pulsingImage.startAnimation(pulseAnimation);
                }
            }
            public void onFinish() {
                stopCountdownTimer();

                // Stop the pulsing animation when the timer finishes
                pulsingImage.clearAnimation();

                // Enable the "Simulate" button when the timer finishes
                findViewById(R.id.eSimulate).setEnabled(true);

                // Reset the countdown timer display to zero
                countdownTimer.setText("00:00:00");

                isTimerRunning = false;

            }
        }.start();

        // Set the timer running flag to true
        isTimerRunning = true;
    }

    private void stopCountdownTimer() {
        sendDataToArduino("0",5000000);
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel the countdown timer if it's running
            countDownTimer = null; // Set the reference to null
            // Clear any animation or update UI accordingly
            pulsingImage.clearAnimation();
//            pulsingImage.setVisibility(View.INVISIBLE);

            isTimerRunning = false;
            countdownTimer.setText("00:00:00"); // Reset the countdown timer display to zero

        }
        sendDataToArduino("0",5000000);
    }

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
            Sheet sheet = workbook.getSheetAt(4); // Assuming sheet 5 is used for Electrical Load data

            // Find the last row with non-empty data in columns
            int lastRowNum = getLastDataRowIndex(sheet);
            // Get the current date and time
            String dateTime = getCurrentDateTime();
            // Create a new row after the last row with data
            Row newRow = sheet.createRow(lastRowNum + 1);

            // Retrieve values from EditText fields
            double energyMJ = Double.parseDouble(energyMJInput.getText().toString());
            double electricaloutputwithoutlosses = Double.parseDouble(electricaloutputwithoutlossesInput.getText().toString());
            double electricaloutputOnly = Double.parseDouble(electricaloutputOnlyInput.getText().toString());
            double loadkWValue = Double.parseDouble(loadkWInput.getText().toString());
            double hoursRunning = Double.parseDouble((hoursRunningInput.getText().toString()));
            double powerOutput = Double.parseDouble((powerOutputIn.getText().toString()));
            double loadWValue = Double.parseDouble((loadWInput.getText().toString()));


            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(energyMJ);
            newRow.createCell(2).setCellValue(electricaloutputwithoutlosses);
            newRow.createCell(3).setCellValue(electricaloutputOnly);
            newRow.createCell(4).setCellValue(loadWValue);
            newRow.createCell(5).setCellValue(loadkWValue);
            newRow.createCell(6).setCellValue(hoursRunning);
            newRow.createCell(7).setCellValue(powerOutput);

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


    /* ============================ Thread to Create Bluetooth Connection =================================== */
    private void connectToArduino(String deviceAddress) {
        if (mBluetoothAdapter == null) {
            // Handle the case where BluetoothAdapter is null
            Log.e(TAG, "BluetoothAdapter is null");
            return;
        }

        // Get the BluetoothDevice object corresponding to the given address
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            // Handle the case where the device is null
            Log.e(TAG, "Bluetooth device is null");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }

        try {
            mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            mSocket.connect();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            // Handle connection errors
            e.printStackTrace();
        }
    }

//    private void sendDataToArduino(String data) {
//        try {
//            if ((mOutputStream != null) || !mBluetoothAdapter.isEnabled()){
//                mOutputStream.write(data.getBytes());
//                Log.d(TAG, "Data sent to Arduino: " + data); // Add this log statement
//                // You can also display a toast message to indicate that data is sent
////                Toast.makeText(this, "Data sent to Arduino: " + data, Toast.LENGTH_SHORT).show();
//                return;
//            } else {
//                Log.e(TAG, "Bluetooth socket is not connected");
//                // Attempt to reconnect or notify the user
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to send data to Arduino", e);
//            // Handle reconnection logic or notify the user
//        }
//    }
private void sendDataToArduino(String command, int duration) {
    String data = "<" + command + "," + duration + ">\n"; // Ensure proper termination
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                if (mOutputStream != null && mBluetoothAdapter.isEnabled()) {
                    mOutputStream.write(data.getBytes());
                    Log.d(TAG, "Data sent to Arduino: " + data);
                } else {
                    Log.e(TAG, "Bluetooth socket is not connected");
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to send data to Arduino", e);
            }
        }
    }).start();
}
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendDataToArduino("0",5000000);
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopCountdownTimer();

        finish();
    }

}
