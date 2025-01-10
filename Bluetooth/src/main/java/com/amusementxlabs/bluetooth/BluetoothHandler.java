package com.amusementxlabs.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import java.util.Set;

public class BluetoothHandler {
    private static final String TAG = "BluetoothHandler";
    private static BluetoothHandler instance;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private Thread dataListenerThread;
    private boolean isListening = false;
    private String receivedData = "";

    private BluetoothHandler() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothHandler getInstance() {
        if (instance == null) {
            instance = new BluetoothHandler();
        }
        return instance;
    }

    public boolean connect(String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            // Start listening for data in a separate thread
            startListeningForData();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Connection failed", e);
            return false;
        }
    }

    public void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                outputStream.flush();
                Log.d(TAG, "Data sent: " + data);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to send data", e);
        }
    }

    public String getReceivedData() {
        String data = receivedData;
        receivedData = ""; // Clear after retrieving
        return data;
    }

    private void startListeningForData() {
        isListening = true;
        dataListenerThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytes;
                while (isListening) {
                    if (inputStream != null && (bytes = inputStream.read(buffer)) > 0) {
                        String newData = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received data: " + newData);
                        synchronized (this) {
                            receivedData += newData; // Append data to a buffer
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error while reading data", e);
            }
        });
        dataListenerThread.start();
    }

    public void disconnect() {
        try {
            isListening = false;
            if (dataListenerThread != null) {
                dataListenerThread.interrupt();
                dataListenerThread = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            Log.d(TAG, "Disconnected");
        } catch (IOException e) {
            Log.e(TAG, "Error while disconnecting", e);
        }
    }

    // Method to get paired devices
    public String getPairedDevices() {
        if (bluetoothAdapter == null) {
            return "Error: Bluetooth not supported on this device";
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            StringBuilder devicesList = new StringBuilder();
            for (BluetoothDevice device : pairedDevices) {
                devicesList.append(device.getName())
                        .append("::")
                        .append(device.getAddress())
                        .append(";");
            }
            return devicesList.toString(); // Return a semicolon-separated list of devices
        } else {
            return "No paired devices found";
        }
    }
}
