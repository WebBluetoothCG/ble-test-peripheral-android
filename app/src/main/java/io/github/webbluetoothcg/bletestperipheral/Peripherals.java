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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;


public class Peripherals extends Activity {

  //TODO(g-ortuno): We will probably need a class to create services more easily
  private static final UUID BATTERY_SERVICE_UUID = UUID
      .fromString("0000180F-0000-1000-8000-00805f9b34fb");

  private static final UUID BATTERY_LEVEL_UUID = UUID
      .fromString("00002A19-0000-1000-8000-00805f9b34fb");
  private static final int INITIAL_BATTERY_LEVEL = 50;
  private static final int BATTERY_LEVEL_MAX = 100;
  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = Peripherals.class.getCanonicalName();

  private TextView mAdvStatus;
  private TextView mConnectionStatus;
  private EditText mBatteryLevelEditText;
  private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newBatteryLevelString = textView.getText().toString();
        // Need to check if the string is empty since isDigitsOnly returns
        // true for empty strings.
        if (!newBatteryLevelString.isEmpty()
            && android.text.TextUtils.isDigitsOnly(newBatteryLevelString)) {
          int newBatteryLevel = Integer.parseInt(newBatteryLevelString);
          if (newBatteryLevel <= BATTERY_LEVEL_MAX) {
            setBatteryLevel(newBatteryLevel, textView);
          } else {
            Toast.makeText(Peripherals.this, R.string.batteryLevelTooHigh, Toast.LENGTH_SHORT)
                .show();
          }
        } else {
          Toast.makeText(Peripherals.this, R.string.batteryLevelIncorrect, Toast.LENGTH_SHORT)
              .show();
        }
      }
      return false;
    }
  };
  private SeekBar mBatteryLevelSeekBar;
  private final OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (fromUser) {
        setBatteryLevel(progress, seekBar);
      }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
  };
  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private AdvertiseData mAdvData;
  private AdvertiseSettings mAdvSettings;
  private BluetoothGattService mBatteryService;
  private BluetoothGattCharacteristic mBatteryLevelCharacteristic;
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
          String deviceName = device.getName();
          // Some devices don't return a name.
          if (deviceName == null) {
            deviceName = device.getAddress();
          }
          final String message = getString(R.string.status_connectedTo) + " " + deviceName;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mConnectionStatus.setText(message);
            }
          });
          Log.v(TAG, "Connected to device: " + device.getAddress());
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mConnectionStatus.setText(R.string.status_notConnected);
            }
          });
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        // There are too many gatt errors (some of them not even in the documentation) so we just
        // show the error to the user.
        final String errorMessage = getString(R.string.errorCode) + ": " + status;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mConnectionStatus.setText(errorMessage);
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

    mAdvStatus = (TextView) findViewById(R.id.textView_advertisingStatus);
    mConnectionStatus = (TextView) findViewById(R.id.textView_connectionStatus);
    mBatteryLevelEditText = (EditText) findViewById(R.id.textView_batteryLevel);
    mBatteryLevelEditText.setOnEditorActionListener(mOnEditorActionListener);
    mBatteryLevelSeekBar = (SeekBar) findViewById(R.id.seekBar_batteryLevel);
    mBatteryLevelSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

    setUpBluetooth();
    createAdvertisement();
    createBatteryService();
    setBatteryLevel(INITIAL_BATTERY_LEVEL, null);
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
    if (mAdvertiser != null) {
      resetStatusViews();
      // When a device in the central role connects to the app, the device will see two services:
      // Generic Attribute and Generic Access.
      mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
      mGattServer.addService(mBatteryService);
      mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvCallback);
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

  private void resetStatusViews() {
    mAdvStatus.setText(R.string.status_notAdvertising);
    mConnectionStatus.setText(R.string.status_notConnected);
  }

  private void setBatteryLevel(int newBatteryLevel, View source) {
    mBatteryLevelCharacteristic.setValue(newBatteryLevel,
        BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 0);
    if (source instanceof TextView) {
      mBatteryLevelSeekBar.setProgress(newBatteryLevel);
    } else if (source instanceof SeekBar) {
      mBatteryLevelEditText.setText(Integer.toString(newBatteryLevel));
    } else {
      mBatteryLevelSeekBar.setProgress(newBatteryLevel);
      mBatteryLevelEditText.setText(Integer.toString(newBatteryLevel));
    }
  }

  /////////////////////////
  ////// Advertising //////
  /////////////////////////

  private void createAdvertisement() {
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

  /////////////////////////////
  ////// Battery Service //////
  /////////////////////////////
  private void createBatteryService() {

    mBatteryLevelCharacteristic =
        new BluetoothGattCharacteristic(BATTERY_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mBatteryService = new BluetoothGattService(BATTERY_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mBatteryService.addCharacteristic(mBatteryLevelCharacteristic);
  }

  ///////////////////////
  ////// Bluetooth //////
  ///////////////////////
  private void setUpBluetooth() {
    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();

    if (mBluetoothAdapter == null) {
      Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Bluetooth not supported");
      finish();
    } else if (!mBluetoothAdapter.isEnabled()) {
      // Make sure bluetooth is enabled
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
      // Make sure device supports LE advertising
      Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Advertising not supported");
      finish();
    } else {
      mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }
  }
}
