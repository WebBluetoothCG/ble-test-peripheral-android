/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;

import io.github.webbluetoothcg.bletestperipheral.ServiceFragment.ServiceFragmentDelegate;

public class Peripheral extends Activity implements ServiceFragmentDelegate {

  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = Peripheral.class.getCanonicalName();
  private static final String CURRENT_FRAGMENT_TAG = "CURRENT_FRAGMENT";

  private TextView mAdvStatus;
  private TextView mConnectionStatus;
  private ServiceFragment mCurrentServiceFragment;
  private BluetoothGattService mBluetoothGattService;
  private HashSet<BluetoothDevice> mBluetoothDevices;
  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private AdvertiseData mAdvData;
  private AdvertiseSettings mAdvSettings;
  private BluetoothLeAdvertiser mAdvertiser;
  private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      Log.e(TAG, "Not broadcasting: " + errorCode);
      int statusText;
      switch (errorCode) {
        case ADVERTISE_FAILED_ALREADY_STARTED:
          statusText = R.string.status_advertising;
          Log.w(TAG, "App was already advertising");
          break;
        case ADVERTISE_FAILED_DATA_TOO_LARGE:
          statusText = R.string.status_advDataTooLarge;
          break;
        case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
          statusText = R.string.status_advFeatureUnsupported;
          break;
        case ADVERTISE_FAILED_INTERNAL_ERROR:
          statusText = R.string.status_advInternalError;
          break;
        case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
          statusText = R.string.status_advTooManyAdvertisers;
          break;
        default:
          statusText = R.string.status_notAdvertising;
          Log.wtf(TAG, "Unhandled error: " + errorCode);
      }
      mAdvStatus.setText(statusText);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      Log.v(TAG, "Broadcasting");
      mAdvStatus.setText(R.string.status_advertising);
    }
  };

  private BluetoothGattServer mGattServer;
  private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
          mBluetoothDevices.add(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Connected to device: " + device.getAddress());
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          mBluetoothDevices.remove(device);
          updateConnectedDevicesStatus();
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        mBluetoothDevices.remove(device);
        updateConnectedDevicesStatus();
        // There are too many gatt errors (some of them not even in the documentation) so we just
        // show the error to the user.
        final String errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(Peripheral.this, errorMessage, Toast.LENGTH_LONG).show();
          }
        });
        Log.e(TAG, "Error when connecting: " + status);
      }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
      Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
      if (offset != 0) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
        return;
      }
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
          offset, characteristic.getValue());
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);
      Log.v(TAG, "Notification sent. Status: " + status);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
        BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
        int offset, byte[] value) {
      super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
          responseNeeded, offset, value);
      Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
      int status = mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value);
      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
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
    mAdvStatus = (TextView) findViewById(R.id.textView_advertisingStatus);
    mConnectionStatus = (TextView) findViewById(R.id.textView_connectionStatus);
    mBluetoothDevices = new HashSet<>();
    // TODO(g-ortuno): This can be moved to Peripherals.
    ensureBleFeaturesAvailable();

    // If we are not being restored from a previous state then create and add the fragment.
    if (savedInstanceState == null) {
      int peripheralIndex = getIntent().getIntExtra(Peripherals.EXTRA_PERIPHERAL_INDEX,
          /* default */ -1);
      if (peripheralIndex == 0) {
        mCurrentServiceFragment = new BatteryServiceFragment();
      } else if (peripheralIndex == 1) {
        mCurrentServiceFragment = new HeartRateServiceFragment();
      } else {
        Log.wtf(TAG, "Service doesn't exist");
      }
      getFragmentManager()
          .beginTransaction()
          .add(R.id.fragment_container, mCurrentServiceFragment, CURRENT_FRAGMENT_TAG)
          .commit();
    } else {
      mCurrentServiceFragment = (ServiceFragment) getFragmentManager()
          .findFragmentByTag(CURRENT_FRAGMENT_TAG);
    }
    mBluetoothGattService = mCurrentServiceFragment.getBluetoothGattService();

    mAdvSettings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build();
    mAdvData = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(true)
        .build();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_OK) {
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
          Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show();
          Log.e(TAG, "Advertising not supported");
          finish();
        } else {
          mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
          onStart();
        }
      } else {
        //TODO(g-ortuno): UX for asking the user to activate bt
        Toast.makeText(this, R.string.bluetoothNotEnabled, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Bluetooth not enabled");
        finish();
      }
    }
  }


  @Override
  protected void onStart() {
    super.onStart();
    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
    // If the user disabled Bluetooth when the app was in the background,
    // openGattServer() will return null.
    if (mAdvertiser != null && mGattServer != null) {
      resetStatusViews();
      // Add a service for a total of three services (Generic Attribute and Generic Access
      // are present by default).
      mGattServer.addService(mBluetoothGattService);
      mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvCallback);
    } else {
      ensureBleFeaturesAvailable();
    }
  }


  @Override
  protected void onStop() {
    super.onPause();
    if (mAdvertiser != null) {
      // If stopAdvertising() gets called before close() a null
      // pointer exception is raised.
      mGattServer.close();
      mAdvertiser.stopAdvertising(mAdvCallback);
      resetStatusViews();
    }
  }

  @Override
  public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
    if (mBluetoothDevices.isEmpty()) {
      Toast.makeText(this, R.string.bluetoothDeviceNotConnected, Toast.LENGTH_SHORT).show();
    } else {
      boolean indicate = (characteristic.getProperties()
          & BluetoothGattCharacteristic.PROPERTY_INDICATE)
          == BluetoothGattCharacteristic.PROPERTY_INDICATE;
      for (BluetoothDevice device : mBluetoothDevices) {
        // true for indication (acknowledge) and false for notification (unacknowledge).
        mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
      }
    }
  }

  private void resetStatusViews() {
    mAdvStatus.setText(R.string.status_notAdvertising);
    updateConnectedDevicesStatus();
  }

  private void updateConnectedDevicesStatus() {
    final String message = getString(R.string.status_devicesConnected) + " "
        + mBluetoothDevices.size();
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mConnectionStatus.setText(message);
      }
    });
  }

  ///////////////////////
  ////// Bluetooth //////
  ///////////////////////
  private void ensureBleFeaturesAvailable() {
    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();

    if (mBluetoothAdapter == null) {
      Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled.
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
      // Make sure device supports LE advertising.
      Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Advertising not supported");
      finish();
    } else {
      mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }
  }

}
