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

public class Peripheral extends Activity implements ServiceFragment.OnFragmentInteractionListener{

  private static final int REQUEST_ENABLE_BT = 1;
  private static final String TAG = Peripheral.class.getCanonicalName();
  private static final String CURRENT_FRAGMENT_TAG = "CURRENT_FRAGMENT";

  private TextView mAdvStatus;
  private TextView mConnectionStatus;
  private BluetoothGattService mBluetoothGattService;
  private BluetoothDevice mBluetoothDevice;
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
          mBluetoothDevice = device;
          String deviceName = mBluetoothDevice.getName();
          // Some devices don't return a name.
          if (deviceName == null) {
            deviceName = mBluetoothDevice.getAddress();
          }
          final String message = getString(R.string.status_connectedTo) + " " + deviceName;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mConnectionStatus.setText(message);
            }
          });
          Log.v(TAG, "Connected to device: " + mBluetoothDevice.getAddress());
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          mBluetoothDevice = null;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mConnectionStatus.setText(R.string.status_notConnected);
            }
          });
          Log.v(TAG, "Disconnected from device");
        }
      } else {
        mBluetoothDevice = null;
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

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);
      Log.v(TAG, "Notification sent. Status: " + status);
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

    // TODO(g-ortuno): This can be moved to Peripherals.
    ensureBleFeaturesAvailable();

    // If we are not being restored from a previous state then create and add the fragment.
    // Initialize it to null otherwise the compiler complains.
    ServiceFragment currentServiceFragment = null;
    if (savedInstanceState == null) {
      int peripheralIndex = getIntent().getIntExtra(Peripherals.EXTRA_PERIPHERAL_INDEX,
          /* default */ -1);
      if (peripheralIndex == 0) {
        currentServiceFragment = new BatteryServiceFragment();
      } else if (peripheralIndex == 1) {
        currentServiceFragment = new HeartRateServiceFragment();
      } else {
        Log.wtf(TAG, "Service doesn't exist");
      }
      getFragmentManager()
          .beginTransaction()
          .add(R.id.fragment_container, currentServiceFragment, CURRENT_FRAGMENT_TAG)
          .commit();
    } else {
      currentServiceFragment = (ServiceFragment) getFragmentManager()
          .findFragmentByTag(CURRENT_FRAGMENT_TAG);
    }
    mBluetoothGattService = currentServiceFragment.getBluetoothGattService();

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
  public void onNotifyButtonPressed(BluetoothGattCharacteristic characteristic) {
    if (mBluetoothDevice != null) {
      mGattServer.notifyCharacteristicChanged(mBluetoothDevice, characteristic,
          // true for indication (acknowledge) and false for notification (unacknowledge)
          // In this case there is not callback for us to receive the acknowledgement from
          // the client to we just set it to false.
          false);
    } else {
      Toast.makeText(this, R.string.bluetoothDeviceNotConnected, Toast.LENGTH_SHORT).show();
    }
  }

  private void resetStatusViews() {
    mAdvStatus.setText(R.string.status_notAdvertising);
    mConnectionStatus.setText(R.string.status_notConnected);
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
