package com.iot.practice.michael.practice02;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
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
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
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
        isConnected = false;
        isConnecting = false;
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
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
            isConnected = false;
            isConnecting = false;
            arrayListBluetoothDevice.clear();
            arrayListStrDevice.clear();
            listView.setAdapter(null);
            tvStatus.setText(R.string.disconnected);
            Toast.makeText(getApplicationContext(),"Bluetooth has turned off", Toast.LENGTH_LONG).show();
            bluetoothAdapter.disable();
        }
    };

    private BluetoothDevice getAvailableDevice() {
        String targetAddress = "2C:F7:F1";
        for (BluetoothDevice currentDevice : arrayListBluetoothDevice) {
            if (currentDevice.getAddress().startsWith(targetAddress) && currentDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                return currentDevice;
        }
        return null;
    }

    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;

    static class MHandler extends Handler {
        WeakReference<MainActivity> mainActivityWeakReference;

        MHandler(MainActivity mainActivity) {
            mainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(Message message) {
            MainActivity mainActivity = mainActivityWeakReference.get();
            switch (message.what) {
                case MESSAGE_READ:
                    String str = new String((byte[])message.obj, StandardCharsets.UTF_8);
                    try {
                        mainActivity.tvStatus.setText("Hi, " + str);
                    } catch (Exception e){
                        mainActivity.tvStatus.setText("Hi, " + e);
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_TOAST:
                    break;
                default:
                    break;
            } // end switch
        }
    }

    MHandler mHandler = new MHandler(this);

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("socket.getInputStream(): " + e);
                    }
                });
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("socket.getOutputStream(): " + e);
                    }
                });
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtain bytes to the UI activity
                    Message readMsg = mHandler.obtainMessage(
                            MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Disconnected");
                        }
                    });
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Could not close the client socket");
                    }
                });
            }
        }
    }

    private synchronized void connected(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        isConnected = true;
        isConnecting = false;
    }

    boolean isConnecting = false;
    boolean isConnected = false;
    private static ConnectedThread connectedThread = null;
    private static ConnectThread connectThread = null;
    private Button.OnClickListener onClickBtnGetData = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getAvailableDevice() == null) {
                tvStatus.setText("You don't have available paired device");
            } else {
                if (isConnecting) {
                    tvStatus.setText("Connecting");
                } else if (isConnected) {
                    tvStatus.setText("Get data");
                    // is connected
                } else {
                    tvStatus.setText("Start connecting");
                    connectThread = new ConnectThread(getAvailableDevice());
                    connectThread.start();
                }
            }
        }
    };

    final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
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
                        tvStatus.setText("Unable to connect to [" + mmDevice.getName() + "]:[" + mmDevice.getAddress() + "]: " + connectException.toString());
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
            synchronized (MainActivity.this) {
                connectThread = null;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText("Connected");
                }
            });
            connected(mmSocket, mmDevice);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Could not close the client socket");
                    }
                });
            }
        }
    }
}