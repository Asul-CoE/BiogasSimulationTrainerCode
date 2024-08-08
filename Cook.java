package com.example.biogassimulation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.UUID;

public class Cook extends AppCompatActivity {
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Declare UI elements
    private CountDownTimer countDownTimer;
    // Declare a variable to store the remaining time in milliseconds
    private long remainingTimeInMillis;
    private EditText biogasInput;
    private EditText remainingGasInput;
    private EditText mTempInputTextView;
    private TextView countdownTimer;
    private ImageView pulsingImage;
    private Animation pulseAnimation;
    private static final String TAG = "Cook";

    // Declare a boolean flag to track if the timer is running
    private boolean isTimerRunning = false;

    // Declare variables to hold biogas production values and countdown timer duration
    private double biogasProductionPerHour;
    private long durationInMillis;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook);

        // Retrieve Bluetooth device address from intent extras
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);

        // Initialize UI elements
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

        // Button click listener to trigger biogas production calculation
        findViewById(R.id.eSimulate2).setOnClickListener(v -> {
            if (!isTimerRunning) {
                // Disable the simulate button
                v.setEnabled(false);
                startBiogasProductionSimulation();
            }
        });

        //Exit
        findViewById(R.id.resultExit).setOnClickListener(v -> {
            // Navigate back to the main activity
            startActivity(new Intent(Cook.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish(); // Finish the current activity
        });

        findViewById(R.id.resultNew).setOnClickListener(v -> {
            // Build an AlertDialog with three choices
            new AlertDialog.Builder(Cook.this)
                    .setTitle("Select Waste Type")
                    .setItems(new String[]{"Livestock Waste", "Food Waste", "Mix"}, (dialog, which) -> {
                        // Handle the selection based on the selected item index
                        switch (which) {
                            case 0:
                                // Livestock Waste selected
                                // Handle accordingly
                                break;
                            case 1:
                                // Food Waste selected
                                // Handle accordingly
                                break;
                            case 2:
                                // Mix selected
                                // Handle accordingly
                                break;
                        }
                        dialog.dismiss(); // Dismiss the dialog
                    })
                    .create()
                    .show(); // Show the AlertDialog
        });

        // Button click listener to stop the countdown timer and animation
        findViewById(R.id.stopButton).setOnClickListener(v -> stopSimulation());

        // Add TextChangedListener to the consumption level EditText
        mTempInputTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Update remaining gas when consumption level changes
                updateRemainingGas();
            }
        });


    }

    // Method to stop the countdown timer and animation
    private void stopSimulation() {
        stopCountdownTimer();
        pulsingImage.clearAnimation();
        isTimerRunning = false;

        // Reset countdown timer to zero
        countdownTimer.setEnabled(false);

        // Enable the simulate button
        findViewById(R.id.eSimulate2).setEnabled(true);
    }

    // Method to start biogas production simulation
    private void startBiogasProductionSimulation() {
        try {
            // Get user input from EditText fields
            String biogasProductionStr = biogasInput.getText().toString();
            String gasThresholdStr = mTempInputTextView.getText().toString();

            // Convert daily biogas production to hourly production
            biogasProductionPerHour = Double.parseDouble(biogasProductionStr) / 24.0;

            // Calculate the consumption rate per hour based on user input
            double gasThresholdHour = Double.parseDouble(gasThresholdStr);

            // Convert consumption rate to cubic meter/minute
            double gasThresholdMinute = gasThresholdHour / 60.0;

            // Calculate initial remaining biogas
            double remainingBiogas = biogasProductionPerHour - gasThresholdMinute;

            // Ensure remaining gas is not negative
            remainingBiogas = Math.max(remainingBiogas, 0);

            // Update EditText field with calculated value
            remainingGasInput.setText(String.valueOf(remainingBiogas));

            // Start countdown timer with initial remaining biogas
            if (!(remainingBiogas <= 0)) {
                long durationInMillis = (long) (remainingBiogas / gasThresholdMinute * (1000.0 * 60.0));
                startCountdownTimer(durationInMillis);
            } else {
                // Stop the countdown timer if remaining biogas is zero or negative
                stopCountdownTimer();
            }
        } catch (NumberFormatException e) {
            // Handle invalid input
            e.printStackTrace();
            // Inform the user about the error
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to update remaining gas
    private void updateRemainingGas() {
        try {
            // Get the latest consumption level from the EditText field
            double gasThresholdHour = Double.parseDouble(mTempInputTextView.getText().toString());

            // Convert consumption rate to cubic meter/minute
            double gasThresholdMinute = gasThresholdHour / 60.0;

            // Calculate remaining biogas based on the latest consumption level
            double remainingBiogas = biogasProductionPerHour - gasThresholdMinute;

            // Ensure remaining gas is not negative
            remainingBiogas = Math.max(remainingBiogas, 0);

            // Update EditText field with calculated value
            remainingGasInput.setText(String.valueOf(remainingBiogas));

            if (remainingBiogas > 0) {
                // Calculate new duration for countdown timer based on remaining gas
                long newDurationInMillis = (long) (remainingBiogas / gasThresholdMinute * (1000.0 * 60.0));

                // Adjust countdown timer to the new duration
                adjustCountdownTimer(newDurationInMillis);
            } else {
                // Stop the countdown timer if remaining biogas is zero or negative
                stopCountdownTimer();
            }
        } catch (NumberFormatException e) {
            // Handle invalid input
            e.printStackTrace();
            // Inform the user about the error
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to adjust the countdown timer duration
    private void adjustCountdownTimer(long newDuration) {
        // Cancel the current countdown timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Start a new countdown timer with the adjusted duration
        startCountdownTimer(newDuration);
    }

    // Method to start the countdown timer
    private void startCountdownTimer(long duration) {
        // Store the initial remaining time
        remainingTimeInMillis = duration;

        // Create a new countdown timer if it's not running
        if (!isTimerRunning) {
            // Disable the simulate button
            findViewById(R.id.eSimulate2).setEnabled(false);

            // Create a new countdown timer
            countDownTimer = new CountDownTimer(remainingTimeInMillis, 1000) {
                public void onTick(long millisUntilFinished) {
                    // Update remaining time
                    remainingTimeInMillis = millisUntilFinished;

                    // Format remaining time as HH:mm:ss
                    int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                    int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
                    int seconds = (int) ((millisUntilFinished % (1000 * 60)) / 1000);
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                    countdownTimer.setText(formattedTime);

                    // Start animation only if the timer is running
                    if (isTimerRunning) {
                        pulsingImage.startAnimation(pulseAnimation);

                    }
                }

                public void onFinish() {
                    // Stop animation when timer finishes
                    pulsingImage.clearAnimation();

                    // Set timer running flag to false
                    isTimerRunning = false;
                }
            }.start();

            // Set the timer running flag to true
            isTimerRunning = true;
        }
    }

    // Method to stop the countdown timer
    private void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel the countdown timer if it's running
            countDownTimer = null; // Set the reference to null
        }
        isTimerRunning = false;
        countdownTimer.setText("00:00:00"); // Reset the countdown timer display to zero
    }
    /////BLUETOOTH SETTINGS///

}