package com.iot.practice.michael.practice02;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private Button btnDeviceSelect, btnGetData, btnDisconnect, btnLedOn, btnLedOff;
    private TextView tvStatus;
    private ListView listView;
    private ArrayList<String> arrayListStrDevice = new ArrayList<String>();
    private ArrayList<BluetoothDevice> arrayListBluetoothDevice = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDeviceSelect = findViewById(R.id.btn_bluetooth_select);
        btnGetData = findViewById(R.id.btn_get_data);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnLedOn = findViewById(R.id.btn_led_on);
        btnLedOff = findViewById(R.id.btn_led_off);

        btnDeviceSelect.setOnClickListener(onClickBtnDeviceSelect);
        btnDisconnect.setOnClickListener(onClickBtnDisconnect);
        btnGetData.setOnClickListener(onClickBtnGetData);

        listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(OnClickListView);

        tvStatus = findViewById(R.id.tv_status);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.d(TAG, "Device does not support Bluetooth");
            Toast.makeText(getApplicationContext(),
                    "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
            this.finish();
            System.exit(0);
        } else {
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            registerReceiver(broadcastReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(broadcastReceiver, filter);
            filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(broadcastReceiver, filter);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                String deviceBondState = "";
                int iDeviceBondState = device.getBondState();
                switch (iDeviceBondState) {
                    case BluetoothDevice.BOND_NONE:
                        deviceBondState = "未配對";
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        deviceBondState = "已配對";
                }
                String strDevice = deviceName + "\n" + deviceHardwareAddress + "  " + deviceBondState;
                // Toast.makeText(getApplicationContext(), "BroadcastOnReceived: " + device.getName() + ": " + device.getAddress(), Toast.LENGTH_LONG).show();
                if (!arrayListBluetoothDevice.contains(device)) {
                    arrayListBluetoothDevice.add(device);
                    arrayListStrDevice.add(strDevice);
                }
                listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, arrayListStrDevice));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                tvStatus.setText(R.string.discovering);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                tvStatus.setText(R.string.finished);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress().toUpperCase(); // MAC address
                String deviceBondState = "";
                int iDeviceBondState = device.getBondState();
                switch (iDeviceBondState) {
                    case BluetoothDevice.BOND_NONE:
                        deviceBondState = "未配對";
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        deviceBondState = "已配對";
                }
                String strDevice = deviceName + "\n" + deviceHardwareAddress + "  " + deviceBondState;
                arrayListStrDevice.set(arrayListBluetoothDevice.indexOf(device), strDevice);
                listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, arrayListStrDevice));
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (bluetoothAdapter.isEnabled())
            bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    ListView.OnItemClickListener OnClickListView = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            BluetoothDevice deviceClicked = arrayListBluetoothDevice.get(i);
            String deviceName = deviceClicked.getName();
            String deviceHardwareAddress = deviceClicked.getAddress(); // MAC address
            String deviceBondState = "";
            if (deviceClicked.getBondState() == BluetoothDevice.BOND_NONE) {
                deviceBondState = "配對中...";
                String strDevice = deviceName + "\n" + deviceHardwareAddress + "  " + deviceBondState;
                arrayListStrDevice.set(i, strDevice);
                pairDevice(deviceClicked);
            } else if (deviceClicked.getBondState() == BluetoothDevice.BOND_BONDED) {
                deviceBondState = "解除配對中...";
                String strDevice = deviceName + "\n" + deviceHardwareAddress + "  " + deviceBondState;
                arrayListStrDevice.set(i, strDevice);
                unpairDevice(deviceClicked);
            }
            listView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayListStrDevice));
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Button.OnClickListener onClickBtnDeviceSelect = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intentTurnOnBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentTurnOnBLE, 0);
            } else if (bluetoothAdapter.isEnabled()) {
                // Already on
                Toast.makeText(getApplicationContext(), "Bluetooth has turned on", Toast.LENGTH_LONG).show();
                bluetoothAdapter.startDiscovery();
            }
        }
    };

    Button.OnClickListener onClickBtnDisconnect = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            bluetoothAdapter.disable();
            arrayListBluetoothDevice.clear();
            arrayListStrDevice.clear();
            listView.setAdapter(null);
            tvStatus.setText(R.string.disconnected);
            Toast.makeText(getApplicationContext(),
                    "Bluetooth has turned off", Toast.LENGTH_LONG).show();
        }
    };

    private BluetoothDevice getAvailableDevice() {
        String targetAddress = "2C:F7:F1";
        for(BluetoothDevice currentDevice : arrayListBluetoothDevice) {
            if (currentDevice.getAddress().startsWith(targetAddress) && currentDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                return currentDevice;
        }
        return null;
    }

    boolean isConnecting = false;
    private Button.OnClickListener onClickBtnGetData = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(getAvailableDevice() == null){
                tvStatus.setText("You don't have available paired device");
            } else {
                if (!isConnecting) {
                    tvStatus.setText("Getting data");
                    Thread threadConnect = new ConnectThread(getAvailableDevice());
                    threadConnect.start();
                }
            }
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Socket's create() method failed");
                    }
                });
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Connecting");
                    }
                });
                mmSocket.connect();
            } catch (final IOException connectException) {
                // Unable to connect; close the socket and return.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Unable to connect: " + connectException.toString());
                    }
                });
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Could not close the client socket");
                        }
                    });
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Toast.makeText(getApplicationContext(), "The connection attempt succeeded", Toast.LENGTH_LONG).show();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
                tvStatus.setText("Could not close the client socket");
            }
        }
    }
}