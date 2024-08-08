package com.example.biogassimulation;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothManager {
    public static final String EXTRA_DEVICE_ADDRESS = "98:DA:50:02:F1:99";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Activity mActivity;
    private TextView mConnectionStatusTextView;
    private TextView mTempInputTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    public BluetoothManager(Activity activity) {
        mActivity = activity;
    }

    public void initializeBluetooth() {
        mConnectionStatusTextView = mActivity.findViewById(R.id.TempInput);
        mTempInputTextView = mActivity.findViewById(R.id.TempInput);

        // Retrieve the Bluetooth device address from the intent
        String deviceAddress = mActivity.getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);

        if (deviceAddress == null) {
            // Device address not provided, finish the activity
            Toast.makeText(mActivity, "Device address not provided", Toast.LENGTH_SHORT).show();
            mActivity.finish();
            return;
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(mActivity, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            mActivity.finish(); // Finish the activity
            return;
        }

        // Get the Bluetooth device object from the address
        mDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        // Start the connection process
        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }

    // Other Bluetooth-related methods can be added here

    private class ConnectThread extends Thread {
        @Override
        public void run() {
            // Check for Bluetooth permissions
            if (ContextCompat.checkSelfPermission(mActivity,
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(mActivity,
                            Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

                // Request Bluetooth permissions
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN},
                        REQUEST_BLUETOOTH_PERMISSIONS);

                // No need to proceed further as permissions are not granted yet
                return;
            }

            // Permissions are granted, proceed with establishing the Bluetooth connection
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mSocket.connect();
                mConnectedThread = new ConnectedThread(mSocket);
                mConnectedThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // M
    }

    // E
}
//RD. End