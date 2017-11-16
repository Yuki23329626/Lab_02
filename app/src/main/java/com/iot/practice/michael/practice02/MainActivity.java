package com.iot.practice.michael.practice02;

import java.util.ArrayList;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

  private final String TAG = "MainActivity";

  private Button btnDeviceSelect, btnGetData, btnDisconnect, btnLedOn, btnLedOff;
  private ListView listView;
  private ArrayList<String> arrayListDeviceName = new ArrayList<String>();
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

    btnDeviceSelect.setOnClickListener(OnClickBtnDeviceSelect);
    btnDisconnect.setOnClickListener(OnClickBtnDisconnect);

    listView = findViewById(R.id.listview);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    bluetoothAdapter.startDiscovery();

    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(broadcastReceiver, filter);

  }

  private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        arrayListDeviceName.add(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
        arrayListBluetoothDevice.add(bluetoothDevice);
        listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, arrayListDeviceName));
      }
    }
  };

  Button.OnClickListener OnClickBtnDeviceSelect = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      if (bluetoothAdapter == null){
        Log.d(TAG, "Device does not support Bluetooth");
        Toast.makeText(getApplicationContext(),
                "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
      }
      else {
        if (!bluetoothAdapter.isEnabled()) {
          Intent intentTurnOnBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(intentTurnOnBLE, 0);
          Toast.makeText(getApplicationContext(), "Bluetooth has turned on", Toast.LENGTH_LONG).show();

        } else {
          // Already on
        }
      }
    }
  };

  Button.OnClickListener OnClickBtnDisconnect = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      bluetoothAdapter.disable();
      Toast.makeText(getApplicationContext(),
              "Bluetooth has turned off", Toast.LENGTH_LONG).show();
    }
  };

}