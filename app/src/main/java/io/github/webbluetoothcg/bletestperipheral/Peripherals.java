package io.github.webbluetoothcg.bletestperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class Peripherals extends Activity {

  private static final int REQUEST_ENABLE_BT = 0;
  private static final String TAG = Peripherals.class.getCanonicalName();
  private static BluetoothLeAdvertiser mAdvertiser;
  private static final AdvertiseCallback advCallback = new AdvertiseCallback() {
    //TODO(g-ortuno): Implement passing the result to the UI
    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      Log.d(TAG, "Not broadcasting");
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      Log.d(TAG, "Broadcasting");
    }
  };

  /////////////////////////////////
  ////// Lifecycle Callbacks //////
  /////////////////////////////////

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_peripherals);

    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    // Check if bluetooth is supported
    if (bluetoothAdapter == null) {
      //TODO(g-ortuno): Show message in the UI and close the app
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!bluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
      // Make sure device supports LE advertising
      //TODO(g-ortuno): Show message in the UI and close the app
      Log.e(TAG, "Advertising not supported");
      finish();
    } else {
      mAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAdvertiser != null) {
      startAdvertising();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mAdvertiser != null) {
      stopAdvertising();
    }
  }

  /////////////////////////
  ////// Advertising //////
  /////////////////////////

  private void startAdvertising() {
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mAdvertiser = bluetoothManager.getAdapter().getBluetoothLeAdvertiser();

    AdvertiseData advertisedData = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build();
    AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build();
    // This includes two services: Generic Attribute and Generic Access.
    mAdvertiser.startAdvertising(advertiseSettings,advertisedData, advCallback);
  }

  private void stopAdvertising() {
    mAdvertiser.stopAdvertising(advCallback);
  }
}
