package com.example.biogassimulation;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private final ActivityResultLauncher<Intent> bluetoothEnableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Bluetooth enabled, proceed
                    startBluetoothActivity();
                } else {
                    // Bluetooth not enabled
                    Snackbar.make(findViewById(android.R.id.content), "Bluetooth not enabled", Snackbar.LENGTH_SHORT).show();
                }
            });
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Snackbar.make(findViewById(android.R.id.content), "Device does not support Bluetooth", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Find buttons by their IDs
        ImageButton btnLivestock = findViewById(R.id.btn_livestock);
        ImageButton btnFoodWaste = findViewById(R.id.btn_foodwaste);
        ImageButton btnMix = findViewById(R.id.btn_mix);

        Button aboutButton = findViewById(R.id.About);
        Button btnData = findViewById(R.id.data);

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });


// Set click listener for the livestock button
        btnLivestock.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Livestock.class));
            // Start the Bluetooth-related activity
            startBluetoothActivity();
        });

        btnFoodWaste.setOnClickListener(v -> {


            startActivity(new Intent(MainActivity.this, Food.class));
        });

        btnMix.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Mixed.class)));

        checkBluetoothEnabled();
        startBluetoothActivity();

        btnData.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ViewData.class)));
        // Initialize MediaPlayer and set the looping background music
        mediaPlayer = MediaPlayer.create(this, R.raw.intromusic); // name of your MP3 file
        mediaPlayer.setLooping(true); // Set looping
        mediaPlayer.setVolume(0.05f, 0.05f); // Set minimal volume (left and right channels)

    }

    private void checkBluetoothEnabled() {
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth not enabled, request to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableLauncher.launch(enableBtIntent);
        } else {
            // Bluetooth already enabled, proceed with Bluetooth operations
            startBluetoothActivity();
        }
    }

    private void startBluetoothActivity() {
        // Start the appropriate Bluetooth-related activity
        String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
        UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    }
    private void startBluetoothActivitybt() {
        // Start the bttesttwo activity with the Bluetooth device address as an extra
        Intent intent = new Intent(MainActivity.this, bttesttwo.class);
        String deviceAddress = "98:DA:50:02:F1:99"; // Replace this with your actual Bluetooth device address
        intent.putExtra(bttesttwo.EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }

    private void showAboutDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_about, null);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("About")
                .setPositiveButton("Close", null); // Null listener, just to dismiss the dialog when clicked

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Start playing the music when the activity is visible
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the music when the activity is not visible
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the MediaPlayer resources when the activity is destroyed
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
