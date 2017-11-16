package com.iot.practice.michael.practice02;

import java.util.ArrayList;
import java.util.Set;

import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

  private Button btnDeviceSelect, btnGetData, btnDisconnect, btnLedOn, btnLedOff;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    btnDeviceSelect = findViewById(R.id.btn_bluetooth_select);
    btnGetData = findViewById(R.id.btn_get_data);
    btnDisconnect = findViewById(R.id.btn_disconnect);
    btnLedOn = findViewById(R.id.btn_led_on);
    btnLedOff = findViewById(R.id.btn_led_off);

  }
}