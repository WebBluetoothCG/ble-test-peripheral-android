package io.github.webbluetoothcg.bletestperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;

public class HeartRateServiceFragment extends ServiceFragment {
  private static final String TAG = HeartRateServiceFragment.class.getCanonicalName();
  /**
   * See <a href="https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.heart_rate.xml">
   * Heart Rate Service</a>
   */
  private static final UUID HEART_RATE_SERVICE_UUID = UUID
      .fromString("0000180D-0000-1000-8000-00805f9b34fb");
  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml">
   * Heart Rate Measurement</a>
   */
  private static final UUID HEART_RATE_MEASUREMENT_UUID = UUID
      .fromString("00002A37-0000-1000-8000-00805f9b34fb");

  private static final int INITIAL_HEART_RATE_MEASUREMENT_VALUE = 60;
  // Max value of uint8
  private static final int MAX_HEART_RATE_MEASUREMENT_VALUE = 255;
  private static final int INITIAL_EXPENDED_ENERGY = 0;
  // Max value of uint16
  private static final int MAX_ENERGY_EXPENDED = 65535;
  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.body_sensor_location.xml">
   * Body Sensor Location</a>
   */
  private static final UUID BODY_SENSOR_LOCATION_UUID = UUID
      .fromString("00002A38-0000-1000-8000-00805f9b34fb");
  private static final int LOCATION_OTHER = 0;

  private BluetoothGattService mHeartRateService;
  private BluetoothGattCharacteristic mHeartRateMeasurementCharacteristic;
  private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
  // TODO(g-ortuno): Implement Heart Rate Control Point characteristic.

  private ServiceFragmentDelegate mDelegate;

  private EditText mEditTextHeartRateMeasurement;
  private final OnEditorActionListener mOnEditorActionListenerHeartRateMeasurement = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newHeartRateMeasurementValueString = textView.getText().toString();
        // Need to check if the string is empty since isDigitsOnly returns
        // true for empty strings.
        if (!newHeartRateMeasurementValueString.isEmpty()
            && TextUtils.isDigitsOnly(newHeartRateMeasurementValueString)) {
          int newHeartRateMeasurementValue = Integer.parseInt(newHeartRateMeasurementValueString);
          if (newHeartRateMeasurementValue <= MAX_HEART_RATE_MEASUREMENT_VALUE) {
            mHeartRateMeasurementCharacteristic.setValue(newHeartRateMeasurementValue,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                /* offset */ 1);
          } else {
            Toast.makeText(getActivity(), R.string.heartRateMeasurementValueTooHigh,
                Toast.LENGTH_SHORT).show();
          }
        } else {
          Toast.makeText(getActivity(), R.string.heartRateMeasurementValueInvalid,
              Toast.LENGTH_SHORT).show();
        }
      }
      return false;
    }
  };
  private final OnEditorActionListener mOnEditorActionListenerEnergyExpended = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newEnergyExpendedString = textView.getText().toString();
        // Need to check if the string is empty since isDigitsOnly returns
        // true for empty strings.
        if (!newEnergyExpendedString.isEmpty()
            && TextUtils.isDigitsOnly(newEnergyExpendedString)) {
          int newEnergyExpended = Integer.parseInt(newEnergyExpendedString);
          if (newEnergyExpended <= MAX_ENERGY_EXPENDED) {
            mHeartRateMeasurementCharacteristic.setValue(newEnergyExpended,
                BluetoothGattCharacteristic.FORMAT_UINT16,
                /* offset */ 2);
          } else {
            Toast.makeText(getActivity(), R.string.energyExpendedTooHigh,
                Toast.LENGTH_SHORT).show();
          }
        } else {
          Toast.makeText(getActivity(), R.string.energyExpendedInvalid,
              Toast.LENGTH_SHORT).show();
        }
      }
      return false;
    }
  };
  private EditText mEditTextEnergyExpended;
  private Spinner mSpinnerBodySensorLocation;
  private final OnItemSelectedListener mLocationSpinnerOnItemSelectedListener =
      new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          setBodySensorLocationValue(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
      };

  private final OnClickListener mNotifyButtonListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      mDelegate.sendNotificationToDevices(mHeartRateMeasurementCharacteristic);
    }
  };

  public HeartRateServiceFragment() {
    mHeartRateMeasurementCharacteristic =
        new BluetoothGattCharacteristic(HEART_RATE_MEASUREMENT_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            /* No permissions */ 0);

    mBodySensorLocationCharacteristic =
        new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHeartRateService.addCharacteristic(mHeartRateMeasurementCharacteristic);
    mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);
    mSpinnerBodySensorLocation = (Spinner) view.findViewById(R.id.spinner_bodySensorLocation);
    mSpinnerBodySensorLocation.setOnItemSelectedListener(mLocationSpinnerOnItemSelectedListener);
    mEditTextHeartRateMeasurement = (EditText) view
        .findViewById(R.id.editText_heartRateMeasurementValue);
    mEditTextHeartRateMeasurement
        .setOnEditorActionListener(mOnEditorActionListenerHeartRateMeasurement);
    mEditTextEnergyExpended = (EditText) view
        .findViewById(R.id.editText_energyExpended);
    mEditTextEnergyExpended
        .setOnEditorActionListener(mOnEditorActionListenerEnergyExpended);
    Button notifyButton = (Button) view.findViewById(R.id.button_heartRateMeasurementNotify);
    notifyButton.setOnClickListener(mNotifyButtonListener);

    setHeartRateMeasurementValue(INITIAL_HEART_RATE_MEASUREMENT_VALUE,
        INITIAL_EXPENDED_ENERGY);
    setBodySensorLocationValue(LOCATION_OTHER);
    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mDelegate = (ServiceFragmentDelegate) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement ServiceFragmentDelegate");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mDelegate = null;
  }

  @Override
  public BluetoothGattService getBluetoothGattService() {
    return mHeartRateService;
  }

  private void setHeartRateMeasurementValue(int heartRateMeasurementValue, int expendedEnergy) {

    Log.d(TAG, Arrays.toString(mHeartRateMeasurementCharacteristic.getValue()));
    /* Set the characteristic to a byte array of size 4 so
     * we can use setValue(value, format, offset);
     * Flags (8bit) + Heart Rate Measurement Value (uint8) + Energy Expended (uint16) = 4 bytes
     *
     * Flags = 1 << 3:
     * Heart Rate Format (0) -> UINT8
     * Sensor Contact Status (00) -> Not Supported
     * Energy Expended (1) -> Field Present
     * RR-Interval (0) -> Field not pressent
     */
    mHeartRateMeasurementCharacteristic.setValue(new byte[]{1 << 3, 0, 0, 0});
    // Characteristic Value: [8, 0, 0, 0]
    mHeartRateMeasurementCharacteristic.setValue(heartRateMeasurementValue,
        BluetoothGattCharacteristic.FORMAT_UINT8,
        /* offset */ 1);
    // Characteristic Value: [8, 60, 0, 0]
    mEditTextHeartRateMeasurement.setText(Integer.toString(heartRateMeasurementValue));
    mHeartRateMeasurementCharacteristic.setValue(expendedEnergy,
        BluetoothGattCharacteristic.FORMAT_UINT16,
        /* offset */ 2);
    // Characteristic Value: [8, 60, 0, 0]
    mEditTextEnergyExpended.setText(Integer.toString(expendedEnergy));
  }
  private void setBodySensorLocationValue(int location) {
    mBodySensorLocationCharacteristic.setValue(new byte[]{(byte) location});
    mSpinnerBodySensorLocation.setSelection(location);
  }
}
