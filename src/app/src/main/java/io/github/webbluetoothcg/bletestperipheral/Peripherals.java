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

  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = Peripherals.class.getCanonicalName();
  private static BluetoothLeAdvertiser mAdvertiser;
  private static BluetoothAdapter mBluetoothAdapter;
  private static final AdvertiseCallback advCallback = new AdvertiseCallback() {
    //TODO(g-ortuno): Implement passing the result to the UI
    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      Log.e(TAG, "Not broadcasting: " + errorCode);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      Log.v(TAG, "Broadcasting");
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
    mBluetoothAdapter = bluetoothManager.getAdapter();
    // Check if bluetooth is supported
    if (mBluetoothAdapter == null) {
      //TODO(g-ortuno): Show message in the UI and close the app
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
      // Make sure device supports LE advertising
      //TODO(g-ortuno): Show message in the UI and close the app
      Log.e(TAG, "Advertising not supported");
      finish();
    } else {
      mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "On result");
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_OK) {
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
          //TODO(g-ortuno): Show message in the UI
          Log.e(TAG, "Advertising not supported");
          finish();
        } else {
          mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        }
      } else {
        //TODO(g-ortuno): UX for asking the user to activate bt
        Log.e(TAG, "Bluetooth not enabled");
        finish();
      }
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
    AdvertiseData advertisedData = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(true)
        .build();
    AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build();
    // When a device in the central role connects to the app, the device will see two services:
    // Generic Attribute and Generic Access.
    mAdvertiser.startAdvertising(advertiseSettings, advertisedData, advCallback);
  }

  private void stopAdvertising() {
    mAdvertiser.stopAdvertising(advCallback);
  }
}
