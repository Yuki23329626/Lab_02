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

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

  private final String TAG = "MainActivity";

  private TextView tvStatus;
  private TextView tvTemperatureValue;
  private TextView tvHumidityValue;
  private ListView listView;
  private ArrayList<String> arrayListStrDevice = new ArrayList<>();
  private ArrayList<BluetoothDevice> arrayListBluetoothDevice = new ArrayList<>();
  private BluetoothAdapter bluetoothAdapter;

  // Constrain for Handler
  public static final int MESSAGE_READ = 0;
  public static final int MESSAGE_WRITE = 1;
  public static final int MESSAGE_TOAST = 2;

  // States of bluetooth connection
  boolean isConnecting = false;
  boolean isConnected = false;

  // Thread deal with connection and input/output
  private static ConnectedThread connectedThread = null;
  private static ConnectThread connectThread = null;

  boolean hadSaidHi = false;
  int countErr = 0;

  // SerialPortServiceClass_UUID
  final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Handler of MainActivity
  MHandler mHandler = new MHandler(this);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Get view of activity_main.xml
    Button btnDeviceSelect = findViewById(R.id.btn_bluetooth_select);
    Button btnGetData = findViewById(R.id.btn_get_data);
    Button btnDisconnect = findViewById(R.id.btn_disconnect);
    Button btnLedOn = findViewById(R.id.btn_led_on);
    Button btnLedOff = findViewById(R.id.btn_led_off);
    tvTemperatureValue = findViewById(R.id.tv_temperature_value);
    tvHumidityValue = findViewById(R.id.tv_humidity_value);
    tvStatus = findViewById(R.id.tv_status);

    // Set listener on view objects
    btnDeviceSelect.setOnClickListener(onClickBtnDeviceSelect);
    btnDisconnect.setOnClickListener(onClickBtnDisconnect);
    btnGetData.setOnClickListener(onClickBtnGetData);
    btnLedOn.setOnClickListener(onClickBtnLedOn);
    btnLedOff.setOnClickListener(onClickBtnLedOff);

    // ListView of found bluetooth devices
    listView = findViewById(R.id.listOfDevices);
    listView.setOnItemClickListener(OnClickListView);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (bluetoothAdapter == null) {
      Log.d(TAG, "Device does not support Bluetooth");
      Toast.makeText(getApplicationContext(),
              "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
      this.finish();
      System.exit(0);
    } else {
      // Set IntentFilter
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
        listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, arrayListStrDevice));
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
        listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, arrayListStrDevice));
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
    hadSaidHi = false;
    isConnecting = false;
    tvTemperatureValue.setText("");
    tvHumidityValue.setText("");
    super.onDestroy();
  }

  // Click on list item to pair/unpair bluetooth device
  ListView.OnItemClickListener OnClickListView = new ListView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
      BluetoothDevice deviceClicked = arrayListBluetoothDevice.get(i);
      String deviceName = deviceClicked.getName();
      String deviceHardwareAddress = deviceClicked.getAddress(); // MAC address
      String deviceBondState;
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
      listView.setAdapter(new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayListStrDevice));
    }
  };

  // Pair to the selected bluetooth device
  private void pairDevice(BluetoothDevice device) {
    try {
      Method method = device.getClass().getMethod("createBond", (Class[]) null);
      method.invoke(device, (Object[]) null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Unpair the selected bluetooth device
  private void unpairDevice(BluetoothDevice device) {
    try {
      Method method = device.getClass().getMethod("removeBond", (Class[]) null);
      method.invoke(device, (Object[]) null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Request to enable bluetooth
  Button.OnClickListener onClickBtnDeviceSelect = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (!bluetoothAdapter.isEnabled()) {
        // Ask user to turn on bluetooth
        Intent intentTurnOnBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intentTurnOnBLE, 0);
      } else if (bluetoothAdapter.isEnabled()) {
        // Already on
        Toast.makeText(getApplicationContext(), "Bluetooth has turned on", Toast.LENGTH_SHORT).show();
        bluetoothAdapter.startDiscovery();
      }
    }
  };

  // Find available bluetooth device and create connection
  // Get sensor data from the device
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
          connectThread = new ConnectThread(getAvailableDevice()); // Create thread connect to device
          connectThread.start(); // Start the thread
        }
      }
    }
  };

  // Cancel the running threads, close the socket and disable bluetooth
  // Clear views and reset flags
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
      tvHumidityValue.setText("");
      tvTemperatureValue.setText("");
      isConnected = false;
      hadSaidHi = false;
      isConnecting = false;
      arrayListBluetoothDevice.clear();
      arrayListStrDevice.clear();
      listView.setAdapter(null);
      Toast.makeText(getApplicationContext(), "Bluetooth has turned off", Toast.LENGTH_SHORT).show();
      bluetoothAdapter.disable();
      tvStatus.setText("Disconnected");
    }
  };

  // Send command to paired device to turn on the LED
  Button.OnClickListener onClickBtnLedOn = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      BluetoothDevice availableDevice = getAvailableDevice();
      if (connectedThread == null) {
        if (availableDevice == null) {
          tvStatus.setText("You don't have available paired device");
        } else {
          if (isConnecting) {
            tvStatus.setText("Connecting");
          } else {
            tvStatus.setText("Start connecting");
            connectThread = new ConnectThread(availableDevice);
            connectThread.start();
          }
        }
      } else {
        connectedThread.write("{\"cmdLed\":1}".getBytes());
        tvStatus.setText("LED ON");
      }
    }
  };

  // Send command to paired device to turn off the LED
  Button.OnClickListener onClickBtnLedOff = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      BluetoothDevice availableDevice = getAvailableDevice();
      if (connectedThread == null) {
        if (availableDevice == null) {
          tvStatus.setText("You don't have available paired device");
        } else {
          if (isConnecting) {
            tvStatus.setText("Connecting");
          } else {
            tvStatus.setText("Start connecting");
            connectThread = new ConnectThread(availableDevice);
            connectThread.start();
          }
        }
      } else {
        connectedThread.write("{\"cmdLed\":0}".getBytes());
        tvStatus.setText("LED OFF");
      }
    }
  };

  // Get available paired bluetooth device
  // Return null if not found any available device
  private BluetoothDevice getAvailableDevice() {
    String targetAddress = "2C:F7:F1";
    for (BluetoothDevice currentDevice : arrayListBluetoothDevice) {
      if (currentDevice.getAddress().startsWith(targetAddress) && currentDevice.getBondState() == BluetoothDevice.BOND_BONDED)
        return currentDevice;
    }
    return null;
  }

  // My Handler
  // React if receive the following message
  static class MHandler extends Handler {
    WeakReference<MainActivity> mainActivityWeakReference;

    MHandler(MainActivity mainActivity) {
      mainActivityWeakReference = new WeakReference<>(mainActivity);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void handleMessage(Message message) {
      MainActivity mainActivity = mainActivityWeakReference.get();
      String strInput = "";
      switch (message.what) {
        case MESSAGE_READ: // If there have messages to read
          try {
            strInput = new String((byte[]) message.obj, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(strInput);
            if (!mainActivity.hadSaidHi) {
              mainActivity.tvStatus.setText("Hi, LinkIt One Lab02");
              mainActivity.hadSaidHi = true;
            }

            if (jsonObject.getInt("Temp") == -1) {
              mainActivity.tvTemperatureValue.setText("Not Ready");
            } else {
              mainActivity.tvTemperatureValue.setText(jsonObject.getString("Temp"));
            }

            if (jsonObject.getInt("Humid") == -1) {
              mainActivity.tvHumidityValue.setText("Not Ready");
            } else {
              mainActivity.tvHumidityValue.setText(jsonObject.getString("Humid"));
            }
          } catch (Exception e) {
            mainActivity.tvStatus.setText("JsonException: " + e);
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

  // Clean up other running threads
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


  // Connecting as a client
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

  // Managing a connection
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
          isConnecting = false;
          isConnected = false;
          hadSaidHi = false;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              tvStatus.setText("Disconnected");
              tvHumidityValue.setText("");
              tvTemperatureValue.setText("");
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

}