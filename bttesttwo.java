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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class bttesttwo extends AppCompatActivity {

    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private OutputStream mOutputStream;


    private BluetoothSocket mSocket;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private TextView mTempInputTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Declare UI elements
    private CountDownTimer countDownTimer;

    private EditText biogasInput;
    private EditText remainingGasInput;
    private TextView countdownTimer;
    private ImageView pulsingImage;
    private Animation pulseAnimation;
    private static final String TAG = "bttesttwo";
    // Declare a boolean flag to track if the timer is running
    private boolean isTimerRunning = false;
//    private BluetoothConnectionIndicator bluetoothConnectionIndicator;
    // Define the Handler
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0:
                    // Handle incoming messages from the ConnectedThread
                    int numBytes = msg.arg1;
                    byte[] buffer = (byte[]) msg.obj;
                    String receivedMessage = new String(buffer, 0, numBytes);
                    Log.d("Received Data", "Received message: " + receivedMessage);
                    // Check if the received message contains temperature value
                    if (receivedMessage.startsWith("Temperature:")) {
                        // Extract the temperature value from the message
                        String temperatureString = receivedMessage.substring(receivedMessage.indexOf(":") + 1).trim(); // Extract after ":"
                        Log.d("Received Data", "Parsed temperature string: " + temperatureString);
                        try {
                            // Parse the temperature value as a double
                            double temperature = Double.parseDouble(temperatureString);
                            Log.d("Received Data", "Parsed temperature double: " + temperature);
                            // Update the UI with the temperature value
                            updateTemperatureUI(String.valueOf(temperature));
                        } catch (NumberFormatException e) {
                            Log.e("Fetch Temperature", "Error parsing temperature value: " + temperatureString);
                        }
                    }
                    break;
                case 1:
                    // Handle temperature data received from the ConnectedThread
                    int temperatureValue = msg.arg1;
                    // Convert the integer temperature value to double
                    double temperatureDouble = temperatureValue / 1000.0; // Assuming temperatureValue was scaled by 1000
                    // Update the UI with the temperature value
                    mTempInputTextView.setText(String.valueOf(temperatureDouble));
                    break;
                // Other cases if needed
            }
        }
    };

    private volatile boolean isConnectThreadRunning = true;
    private volatile boolean isConnectedThreadRunning = true;

    private double initialRemainingBiogas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_bttesttwo);

      // Retrieve the Bluetooth device address from the intent
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
        if (deviceAddress == null) {
            // Handle null address, show an error message or take appropriate action
            Toast.makeText(this, "Bluetooth device address is null", Toast.LENGTH_SHORT).show();
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

        // Start the connection process
        mConnectThread = new bttesttwo.ConnectThread();
        mConnectThread.start();

        biogasInput = findViewById(R.id.initialGasINPUT);
        remainingGasInput = findViewById(R.id.remainingGasINPUT);
        mTempInputTextView = findViewById(R.id.ConsumptionINPUT);
        countdownTimer = findViewById(R.id.countdownTimer);
        pulsingImage = findViewById(R.id.pulsingImageView);


        // Instantiate LoadDataFromExcelTask and execute it
        LoadDataFromExcelTask loadDataFromExcelTask = new LoadDataFromExcelTask(this, biogasInput);
        loadDataFromExcelTask.execute();

        // Load animation
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulsing_animation);

// Inside your activity class
        findViewById(R.id.tryAgain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });




        findViewById(R.id.eSimulate2).setOnClickListener(v -> {
            connectToArduino(deviceAddress);

            sendDataToArduino("2",0);
            connectToArduino(deviceAddress);

            // Pause for 1 second before resuming potentiometer reading
            new Handler().postDelayed(() -> {

                // Check if the timer is not running
                if (!isTimerRunning) {
                    // Disable the simulate button
                    v.setEnabled(false);

                    // Start biogas production simulation
                    startBiogasProductionSimulation();

                    // Show a notification that data is saved
                    Toast.makeText(this, "Data saved.", Toast.LENGTH_SHORT).show();
                }
            }, 3000); // 1000 milliseconds = 1 second
        });


        //Exit
        findViewById(R.id.resultExit).setOnClickListener(v -> {
            // Navigate back to the main activity
            startActivity(new Intent(bttesttwo.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            stopCountdownTimer();
            stopSimulation();
            finish();
        });


        findViewById(R.id.resultNew).setOnClickListener(v -> {
            // Build an AlertDialog with three choices
            new AlertDialog.Builder(bttesttwo.this)
                    .setTitle("Select Waste Type")
                    .setItems(new String[]{"Livestock Waste", "Food Waste", "Mix"}, (dialog, which) -> {
                        // Handle the selection based on the selected item index
                        switch (which) {
                            case 0:
                                // Livestock Waste selected
                                startActivity(new Intent(bttesttwo.this, Livestock.class));
                                finish(); // Finish the current activity
                                // Handle accordingly
                                break;
                            case 1:
                                // Food Waste selected
                                startActivity(new Intent(bttesttwo.this, Food.class));
                                finish(); // Finish the current activity

                                break;
                            case 2:
                                // Mix selected
                                startActivity(new Intent(bttesttwo.this, Mixed.class));
                                finish(); // Finish the current activity
                                break;
                        }
                        dialog.dismiss(); // Dismiss the dialog
                    })
                    .create()
                    .show(); // Show the AlertDialog
        });


        findViewById(R.id.stopButton).setOnClickListener(v -> {

            // Start reading data from Arduino
            stopSimulation(); // Stop the simulation
            stopCountdownTimer(); // Stop the countdown timer
            //connectToArduino(deviceAddress); // Connect to Arduino after delay
        });


        // Button click listener to stop the countdown timer and animation
        findViewById(R.id.selectButton).setOnClickListener(v -> {
            long delayBetweenOperations = 100; // 0.5 second delay (adjust as needed)

            sendDataToArduino("3",500000000);//start reading after clicking Connect
            findViewById(R.id.eSimulate2).setEnabled(true);
            findViewById(R.id.stopButton).setEnabled(true);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectToArduino(deviceAddress);
                }
            }, delayBetweenOperations*2);
        });

        // Initialize and start the connectThread
        startConnectThread();

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
//                bluetoothConnectionIndicator.connect(EXTRA_DEVICE_ADDRESS); // Replace with the actual Bluetooth device address
            } else {
                // Permission denied by user, display a message or handle the denial appropriately
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Method to stop the countdown timer and animation
    private void stopSimulation() {
        stopCountdownTimer(); // Stop the countdown timer
        pulsingImage.clearAnimation(); // Stop animation
        isTimerRunning = false; // Set timer running flag to false

        // Reset countdown timer display to zero and disable it
        countdownTimer.setText("00:00:00");
        countdownTimer.setEnabled(false);

        // Enable the simulate button
        findViewById(R.id.eSimulate2).setEnabled(true);
    }

    private void startBiogasProductionSimulation() {
        try {
            String biogasProductionStr = biogasInput.getText().toString();
            String gasThresholdStr = mTempInputTextView.getText().toString();

            if (biogasProductionStr.isEmpty() || gasThresholdStr.isEmpty()) {
                Toast.makeText(this, "Please enter valid input values", Toast.LENGTH_SHORT).show();
                return;
            }

            double biogasProductionPerDay = Double.parseDouble(biogasProductionStr);
            double gasConsumptionRatePerHour = Double.parseDouble(gasThresholdStr)*60;

            if (gasConsumptionRatePerHour <= 0) {
                Toast.makeText(this, "Gas threshold must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            initialRemainingBiogas = biogasProductionPerDay;
            double durationInHours = initialRemainingBiogas / (gasConsumptionRatePerHour);
            long durationInMillis = (long) (durationInHours * 3600 * 1000);

            new Handler().postDelayed(() -> {
                sendDataToArduino("2", 0);

                new Handler().postDelayed(() -> startCountdownTimer(durationInMillis, gasConsumptionRatePerHour), 1000);
            }, 1000);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCountdownTimer(long durationInMillis, double gasConsumptionRatePerHour) {
        if (!isTimerRunning) {
            findViewById(R.id.eSimulate2).setEnabled(false);
            findViewById(R.id.stopButton).setEnabled(true);

            final boolean[] isFirstTick = {true};
            double gasConsumptionRatePerSecond = gasConsumptionRatePerHour / 3600.0;

            countDownTimer = new CountDownTimer(durationInMillis, 1000) {
                public void onTick(long millisUntilFinished) {
                    initialRemainingBiogas -= gasConsumptionRatePerSecond;
                    initialRemainingBiogas = Math.max(initialRemainingBiogas, 0);

                    remainingGasInput.setText(String.format(Locale.getDefault(), "%.4f", initialRemainingBiogas));

                    int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                    int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
                    int seconds = (int) ((millisUntilFinished % (1000 * 60)) / 1000);
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

                    countdownTimer.setText(formattedTime);

                    if (isTimerRunning) {
                        pulsingImage.startAnimation(pulseAnimation);
                    }

                    if (isFirstTick[0]) {
                        writeDataToExcel("formula.xls");
                        isFirstTick[0] = false;
                    }

                    sendDataToArduino("2", 0);
                }

                public void onFinish() {
                    pulsingImage.clearAnimation();
                    isTimerRunning = false;
                    countdownTimer.setText("00:00:00");
                    findViewById(R.id.eSimulate2).setEnabled(true);
                    findViewById(R.id.stopButton).setEnabled(false);
                    sendDataToArduino("4", 0);
                }
            }.start();

            isTimerRunning = true;
        }
    }



    // Method to stop the countdown timer
    private void stopCountdownTimer() {
        long delayBetweenOperations = 500; // 0.5 second delay (adjust as needed)
        sendDataToArduino("3",0);

        // Send data to Arduino to start reading potentiometer
        sendDataToArduino("4",0);

        // Connect to Arduino after a delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stop the countdown timer
                if (countDownTimer != null) {
                    countDownTimer.cancel(); // Cancel the countdown timer if it's running
                    countDownTimer = null; // Set the reference to null
                }
                isTimerRunning = false;
                countdownTimer.setText("00:00:00"); // Reset the countdown timer display to zero

                // Enable and disable buttons
                findViewById(R.id.eSimulate2).setEnabled(true);
                findViewById(R.id.stopButton).setEnabled(false);
            }
        }, delayBetweenOperations * 2); // Adjust delay as needed
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the Bluetooth socket when the activity is destroyed
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        bluetoothConnectionIndicator.disconnect();
        stopCountdownTimer();
        stopSimulation();
        finish();
    }

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
            sendInitializationCommands();
        } catch (IOException e) {
            // Handle connection errors
            e.printStackTrace();
        }

    }
    private void sendInitializationCommands() {
        String[] initCommands = {
                "<START>",
                "<CONNECTION ESTABLISHED>",
                "<END>"
        };
        for (String command : initCommands) {
            try {
                mOutputStream.write(command.getBytes());
                mOutputStream.flush();
                Log.d(TAG, "Sent: " + command);
                // Sleep for a short while to ensure commands are processed sequentially
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error sending initialization command", e);
            }
        }
    }

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



    private class ConnectThread extends Thread {
        @Override
        public void run() {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
            }

            if (ContextCompat.checkSelfPermission(bttesttwo.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(bttesttwo.this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(bttesttwo.this,
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }

            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mSocket.connect();
                startConnectedThread(mSocket);
            } catch (IOException e) {
                e.printStackTrace();
                if (isConnectThreadRunning) {
                    run();
                }
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BlockingQueue<byte[]> writeQueue = new LinkedBlockingQueue<>();
        private boolean sendPotentiometerData = true;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input/output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void write(byte[] bytes) {
            try {
                writeQueue.put(bytes);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error occurred when putting data in writeQueue", e);
            }
        }

        public void setSendPotentiometerData(boolean send) {
            sendPotentiometerData = send;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnectedThreadRunning) {
                try {
                    if (mmInStream.available() > 0) {
                        bytes = mmInStream.read(buffer);
                        String receivedMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received message: " + receivedMessage);

                        if (receivedMessage.startsWith("Temperature:")) {
                            String temperatureString = receivedMessage.substring(receivedMessage.indexOf(":") + 1).trim();
                            try {
                                int temperature = Integer.parseInt(temperatureString);
                                int translatedTemperature;
                                if (temperature >= 0 && temperature <= 25) {
                                    translatedTemperature = 226;
                                } else if (temperature >= 26 && temperature <= 31) {
                                    translatedTemperature = 280;
                                } else if (temperature >= 32 && temperature <= 37) {
                                    translatedTemperature = 420;
                                } else {
                                    Log.e(TAG, "Temperature out of range");
                                    translatedTemperature = -1;
                                }

                                mHandler.obtainMessage(1, translatedTemperature, -1).sendToTarget();
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing temperature value: " + temperatureString);
                            }
                        }
                    }

                    byte[] dataToWrite = writeQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (dataToWrite != null) {
                        mmOutStream.write(dataToWrite);
                        mmOutStream.flush();
                        Log.d(TAG, "Data sent to Arduino: " + new String(dataToWrite));
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Error occurred in run method", e);
                    close();
                    break;
                }
            }
        }

        public void close() {
            try {
                if (mmInStream != null) mmInStream.close();
                if (mmOutStream != null) mmOutStream.close();
                if (mmSocket != null) mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing connection", e);
            }
        }
    }

    private void updateTemperatureUI(String temperatureString) {
        try {
            // Update the TempInput EditText with the temperature value
            mTempInputTextView.post(() -> mTempInputTextView.setText(temperatureString));
        } catch (NumberFormatException e) {
            Log.e("Fetch Temperature", "Error parsing temperature value: " + temperatureString);
        }
    }

    //----------------------Spreadsheets Functions and Methods--------------------//
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
    //----------writing--------------//

    // Method to write data to Excel version 3.5
    private void writeDataToExcel(String fileName) {
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);

            // Assuming sheet 5 is used for Cooking Load data
            Sheet sheet = workbook.getSheetAt(5);

            // Find the last row with non-empty data in columns
            int lastRowNum = getLastDataRowIndex(sheet);

            // Get the current date and time
            String dateTime = getCurrentDateTime();

            // Create a new row after the last row with data
            Row newRow = sheet.createRow(lastRowNum + 1);

            // Retrieve values from EditText fields
            String remainingGasStr = remainingGasInput.getText().toString();
            String thresholdStr = mTempInputTextView.getText().toString();
            String initialGasStr = biogasInput.getText().toString();

            // Check if any of the EditText fields are empty
            if (remainingGasStr.isEmpty() || thresholdStr.isEmpty() || initialGasStr.isEmpty()) {
                Toast.makeText(this, "Please fill all the input fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double remainingGas = Double.parseDouble(remainingGasStr);
            double threshold = Double.parseDouble(thresholdStr);
            double initialGas = Double.parseDouble(initialGasStr);

            // Log the values to ensure they are correct
            Log.d(TAG, "DateTime: " + dateTime);
            Log.d(TAG, "Initial Gas: " + initialGas);
            Log.d(TAG, "Threshold: " + threshold);
            Log.d(TAG, "Remaining Gas: " + remainingGas);

            // Update the cell values with the new inputs
            newRow.createCell(0).setCellValue(dateTime); // Column A
            newRow.createCell(1).setCellValue(initialGas); // Column B
            newRow.createCell(2).setCellValue(threshold); // Column C
            newRow.createCell(3).setCellValue(remainingGas); // Column D

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

        } catch (IOException | NumberFormatException e) {
            Log.e(TAG, "Error writing to file: ", e);
            Toast.makeText(this, "Error writing to file", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to get current date and time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void startConnectThread() {
        if (mConnectThread == null) {
            mConnectThread = new ConnectThread();
            mConnectThread.start();
        }
    }

    private void startConnectedThread(BluetoothSocket socket) {
        if (mConnectedThread == null) {
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        isConnectThreadRunning = false;
        isConnectedThreadRunning = false;
        try {
            if (mConnectThread != null) {
                mConnectThread.join();
                mConnectThread = null;
            }
            if (mConnectedThread != null) {
                mConnectedThread.close();
                mConnectedThread.join();
                mConnectedThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isConnectThreadRunning = true;
        isConnectedThreadRunning = true;
        startConnectThread();
    }


}
