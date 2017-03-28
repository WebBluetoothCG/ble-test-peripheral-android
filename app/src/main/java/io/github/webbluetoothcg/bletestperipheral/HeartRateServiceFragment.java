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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.ParcelUuid;
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
  private static final int MIN_UINT = 0;
  private static final int MAX_UINT8 = (int) Math.pow(2, 8) - 1;
  private static final int MAX_UINT16 = (int) Math.pow(2, 16) - 1;
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
  private static final int HEART_RATE_MEASUREMENT_VALUE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
  private static final int INITIAL_HEART_RATE_MEASUREMENT_VALUE = 60;
  private static final int EXPENDED_ENERGY_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT16;
  private static final int INITIAL_EXPENDED_ENERGY = 0;
  private static final String HEART_RATE_MEASUREMENT_DESCRIPTION = "Used to send a heart rate " +
      "measurement";

  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.body_sensor_location.xml">
   * Body Sensor Location</a>
   */
  private static final UUID BODY_SENSOR_LOCATION_UUID = UUID
      .fromString("00002A38-0000-1000-8000-00805f9b34fb");
  private static final int LOCATION_OTHER = 0;

  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_control_point.xml">
   * Heart Rate Control Point</a>
   */
  private static final UUID HEART_RATE_CONTROL_POINT_UUID = UUID
      .fromString("00002A39-0000-1000-8000-00805f9b34fb");

  private BluetoothGattService mHeartRateService;
  private BluetoothGattCharacteristic mHeartRateMeasurementCharacteristic;
  private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
  private BluetoothGattCharacteristic mHeartRateControlPoint;

  private ServiceFragmentDelegate mDelegate;

  private EditText mEditTextHeartRateMeasurement;
  private final OnEditorActionListener mOnEditorActionListenerHeartRateMeasurement = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newHeartRateMeasurementValueString = textView.getText().toString();
        if (isValidCharacteristicValue(newHeartRateMeasurementValueString,
            HEART_RATE_MEASUREMENT_VALUE_FORMAT)) {
          int newHeartRateMeasurementValue = Integer.parseInt(newHeartRateMeasurementValueString);
          mHeartRateMeasurementCharacteristic.setValue(newHeartRateMeasurementValue,
              HEART_RATE_MEASUREMENT_VALUE_FORMAT,
              /* offset */ 1);
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
        if (isValidCharacteristicValue(newEnergyExpendedString,
            EXPENDED_ENERGY_FORMAT)) {
          int newEnergyExpended = Integer.parseInt(newEnergyExpendedString);
          mHeartRateMeasurementCharacteristic.setValue(newEnergyExpended,
              EXPENDED_ENERGY_FORMAT,
              /* offset */ 2);
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

    mHeartRateMeasurementCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mHeartRateMeasurementCharacteristic.addDescriptor(
        Peripheral.getCharacteristicUserDescriptionDescriptor(HEART_RATE_MEASUREMENT_DESCRIPTION));

    mBodySensorLocationCharacteristic =
        new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mHeartRateControlPoint =
        new BluetoothGattCharacteristic(HEART_RATE_CONTROL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE);

    mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHeartRateService.addCharacteristic(mHeartRateMeasurementCharacteristic);
    mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
    mHeartRateService.addCharacteristic(mHeartRateControlPoint);
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

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(HEART_RATE_SERVICE_UUID);
  }

  private void setHeartRateMeasurementValue(int heartRateMeasurementValue, int expendedEnergy) {

    Log.d(TAG, Arrays.toString(mHeartRateMeasurementCharacteristic.getValue()));
    /* Set the org.bluetooth.characteristic.heart_rate_measurement
     * characteristic to a byte array of size 4 so
     * we can use setValue(value, format, offset);
     *
     * Flags (8bit) + Heart Rate Measurement Value (uint8) + Energy Expended (uint16) = 4 bytes
     *
     * Flags = 1 << 3:
     *   Heart Rate Format (0) -> UINT8
     *   Sensor Contact Status (00) -> Not Supported
     *   Energy Expended (1) -> Field Present
     *   RR-Interval (0) -> Field not pressent
     *   Unused (000)
     */
    mHeartRateMeasurementCharacteristic.setValue(new byte[]{0b00001000, 0, 0, 0});
    // Characteristic Value: [flags, 0, 0, 0]
    mHeartRateMeasurementCharacteristic.setValue(heartRateMeasurementValue,
        HEART_RATE_MEASUREMENT_VALUE_FORMAT,
        /* offset */ 1);
    // Characteristic Value: [flags, heart rate value, 0, 0]
    mEditTextHeartRateMeasurement.setText(Integer.toString(heartRateMeasurementValue));
    mHeartRateMeasurementCharacteristic.setValue(expendedEnergy,
        EXPENDED_ENERGY_FORMAT,
        /* offset */ 2);
    // Characteristic Value: [flags, heart rate value, energy expended (LSB), energy expended (MSB)]
    mEditTextEnergyExpended.setText(Integer.toString(expendedEnergy));
  }
  private void setBodySensorLocationValue(int location) {
    mBodySensorLocationCharacteristic.setValue(new byte[]{(byte) location});
    mSpinnerBodySensorLocation.setSelection(location);
  }

  private boolean isValidCharacteristicValue(String s, int format) {
    try {
      int value = Integer.parseInt(s);
      if (format == BluetoothGattCharacteristic.FORMAT_UINT8) {
        return (value >= MIN_UINT) && (value <= MAX_UINT8);
      } else if (format == BluetoothGattCharacteristic.FORMAT_UINT16) {
        return (value >= MIN_UINT) && (value <= MAX_UINT16);
      } else {
        throw new IllegalArgumentException(format + " is not a valid argument");
      }
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
    if (offset != 0) {
      return BluetoothGatt.GATT_INVALID_OFFSET;
    }
    // Heart Rate control point is a 8bit characteristic
    if (value.length != 1) {
      return BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
    }
    if ((value[0] & 1) == 1) {
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mHeartRateMeasurementCharacteristic.setValue(INITIAL_EXPENDED_ENERGY,
              EXPENDED_ENERGY_FORMAT, /* offset */ 2);
          mEditTextEnergyExpended.setText(Integer.toString(INITIAL_EXPENDED_ENERGY));
        }
      });
    }
    return BluetoothGatt.GATT_SUCCESS;
  }

  @Override
  public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
    if (characteristic.getUuid() != HEART_RATE_MEASUREMENT_UUID) {
      return;
    }
    if (indicate) {
      return;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getActivity(), R.string.notificationsEnabled, Toast.LENGTH_SHORT)
            .show();
      }
    });
  }

  @Override
  public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid() != HEART_RATE_MEASUREMENT_UUID) {
      return;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getActivity(), R.string.notificationsNotEnabled, Toast.LENGTH_SHORT)
            .show();
      }
    });
  }
}
