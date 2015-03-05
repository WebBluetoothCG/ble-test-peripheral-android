package io.github.webbluetoothcg.bletestperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;


public class Peripherals extends Activity {

  //TODO(g-ortuno): We will probably need a class to create services more easily
  private static final UUID BATTERY_SERVICE_UUID = UUID
      .fromString("0000180F-0000-1000-8000-00805f9b34fb");

  private static final UUID BATTERY_LEVEL_UUID = UUID
      .fromString("00002A19-0000-1000-8000-00805f9b34fb");

  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = Peripherals.class.getCanonicalName();
  private static BluetoothManager mBluetoothManager;
  private static BluetoothAdapter mBluetoothAdapter;
  private static BluetoothLeAdvertiser mAdvertiser;
  private static final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
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

  private static BluetoothGattServer mGattServer;
  private static final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
          //TODO(g-ortuno): Pass info to UI
          Log.v(TAG, "Connected to device: " + device.getAddress());
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          //TODO(g-ortuno): Pass info to UI
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        //TODO(g-ortuno): Pass info to UI
        Log.e(TAG, "Error when connecting: " + status);
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
      if (offset == 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
            offset, characteristic.getValue());
      } else {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            null);
      }
    }
  };

  /////////////////////////////////
  ////// Lifecycle Callbacks //////
  /////////////////////////////////

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_peripherals);

    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();
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
      startGattServer();
      startAdvertising();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mAdvertiser != null) {
      stopAdvertising();
      stopGattServer();
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
    mAdvertiser.startAdvertising(advertiseSettings, advertisedData, mAdvCallback);
  }

  private void stopAdvertising() {
    mAdvertiser.stopAdvertising(mAdvCallback);
  }

  /////////////////////////
  ////// Gatt Server //////
  /////////////////////////

  private void startGattServer() {
    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
    BluetoothGattService batteryService = buildBatteryService();
    mGattServer.addService(batteryService);
  }
  private void stopGattServer() {
    // Ideally we would like to close the server but that introduces a race condition where
    // the server is closed before onConnectionStateChange is called.
    mGattServer.clearServices();
  }

  /////////////////////////////
  ////// Battery Service //////
  /////////////////////////////
  public BluetoothGattService buildBatteryService() {
    int properties = BluetoothGattCharacteristic.PROPERTY_READ
        | BluetoothGattCharacteristic.PROPERTY_NOTIFY;
    int permissions = BluetoothGattCharacteristic.PERMISSION_READ;
    // Sample battery level of 50
    int sampleLevel = 50;

    BluetoothGattCharacteristic batteryCharacteristic =
        new BluetoothGattCharacteristic(BATTERY_LEVEL_UUID, properties,
            permissions);
    batteryCharacteristic.setValue(sampleLevel, BluetoothGattCharacteristic.FORMAT_UINT8, 0);

    BluetoothGattService batteryService = new BluetoothGattService(BATTERY_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    batteryService.addCharacteristic(batteryCharacteristic);

    return batteryService;
  }
}
