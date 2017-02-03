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
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class HealthThermometerServiceFragment extends ServiceFragment {
  /**
   * See <a href="https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.health_thermometer.xml">
   * Health Thermometer Service</a>
   */
  private static final UUID HEALTH_THERMOMETER_SERVICE_UUID = UUID
      .fromString("00001809-0000-1000-8000-00805f9b34fb");

  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_measurement.xml">
   * Temperature Measurement</a>
   */
  private static final UUID TEMPERATURE_MEASUREMENT_UUID = UUID
      .fromString("00002A1C-0000-1000-8000-00805f9b34fb");
  private static final int TEMPERATURE_MEASUREMENT_VALUE_FORMAT = BluetoothGattCharacteristic.FORMAT_FLOAT;
  private static final float INITIAL_TEMPERATURE_MEASUREMENT_VALUE = 37.0f;
  private static final String TEMPERATURE_MEASUREMENT_DESCRIPTION = "Used to send a temperature " +
      "measurement";

  /**
   * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.measurement_interval.xml">
   * Measurement Interval</a>
   */
  private static final UUID MEASUREMENT_INTERVAL_UUID = UUID
      .fromString("00002A21-0000-1000-8000-00805f9b34fb");
  private static final int MEASUREMENT_INTERVAL_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT16;
  private static final int INITIAL_MEASUREMENT_INTERVAL = 1;
  private static final int MIN_MEASUREMENT_INTERVAL = 1;
  private static final int MAX_UINT16 = (int) Math.pow(2, 16) - 1;
  private static final String MEASUREMENT_INTERVAL_DESCRIPTION = "Defines the time between " +
      "measurements in seconds";

  private BluetoothGattService mHealthThermometerService;
  private BluetoothGattCharacteristic mTemperatureMeasurementCharacteristic;
  private BluetoothGattCharacteristic mMeasurementIntervalCharacteristic;

  private ServiceFragmentDelegate mDelegate;

  private CountDownTimer timer;

  private EditText mEditTextTemperatureMeasurement;
  private final OnEditorActionListener mOnEditorActionListenerTemperatureMeasurement = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newTemperatureMeasurementValueString = textView.getText().toString();
        if (isValidTemperatureMeasurementValue(newTemperatureMeasurementValueString)) {
          float newTemperatureMeasurementValue = Float.valueOf(newTemperatureMeasurementValueString);
          setTemperatureMeasurementValue(newTemperatureMeasurementValue);
        } else {
          Toast.makeText(getActivity(), R.string.temperatureMeasurementValueInvalid,
              Toast.LENGTH_SHORT).show();
        }
      }
      return false;
    }
  };

  private final OnEditorActionListener mOnEditorActionListenerMeasurementInterval = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newMeasurementIntervalString = textView.getText().toString();
        if (isValidMeasurementIntervalValue(newMeasurementIntervalString)) {
          int newMeasurementInterval = Integer.parseInt(newMeasurementIntervalString);
          mMeasurementIntervalCharacteristic.setValue(newMeasurementInterval,
              MEASUREMENT_INTERVAL_FORMAT,
              /* offset */ 0);
          resetTimer(newMeasurementInterval);
        } else {
          Toast.makeText(getActivity(), R.string.measurementIntervalInvalid,
              Toast.LENGTH_SHORT).show();
        }
      }
      return false;
    }
  };
  private EditText mEditTextMeasurementInterval;

  public HealthThermometerServiceFragment() {
    mTemperatureMeasurementCharacteristic =
        new BluetoothGattCharacteristic(TEMPERATURE_MEASUREMENT_UUID,
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            /* No permissions */ 0);

    mTemperatureMeasurementCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mTemperatureMeasurementCharacteristic.addDescriptor(
        Peripheral.getCharacteristicUserDescriptionDescriptor(TEMPERATURE_MEASUREMENT_DESCRIPTION));

    mMeasurementIntervalCharacteristic =
        new BluetoothGattCharacteristic(MEASUREMENT_INTERVAL_UUID,
            (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE),
            (BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE));

    mMeasurementIntervalCharacteristic.addDescriptor(
            Peripheral.getClientCharacteristicConfigurationDescriptor());

    mMeasurementIntervalCharacteristic.addDescriptor(
            Peripheral.getCharacteristicUserDescriptionDescriptor(MEASUREMENT_INTERVAL_DESCRIPTION));

    mHealthThermometerService = new BluetoothGattService(HEALTH_THERMOMETER_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHealthThermometerService.addCharacteristic(mTemperatureMeasurementCharacteristic);
    mHealthThermometerService.addCharacteristic(mMeasurementIntervalCharacteristic);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_health_thermometer, container, false);
    mEditTextTemperatureMeasurement = (EditText) view
        .findViewById(R.id.editText_temperatureMeasurementValue);
    mEditTextTemperatureMeasurement
        .setOnEditorActionListener(mOnEditorActionListenerTemperatureMeasurement);
    mEditTextMeasurementInterval = (EditText) view
        .findViewById(R.id.editText_measurementIntervalValue);
    mEditTextMeasurementInterval
        .setOnEditorActionListener(mOnEditorActionListenerMeasurementInterval);

    setDefaultMeasurementValues(INITIAL_TEMPERATURE_MEASUREMENT_VALUE,
        INITIAL_MEASUREMENT_INTERVAL);
    setTimer(INITIAL_MEASUREMENT_INTERVAL);

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
    timer.cancel();
    super.onDetach();
    mDelegate = null;
  }

  @Override
  public BluetoothGattService getBluetoothGattService() {
    return mHealthThermometerService;
  }

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(HEALTH_THERMOMETER_SERVICE_UUID);
  }

  private void setDefaultMeasurementValues(float temperatureMeasurementValue, int measurementIntervalValue) {

    mEditTextTemperatureMeasurement.setText(Float.toString(temperatureMeasurementValue));
    setTemperatureMeasurementValue(temperatureMeasurementValue);

    mMeasurementIntervalCharacteristic.setValue(measurementIntervalValue,
        MEASUREMENT_INTERVAL_FORMAT,
        /* offset */ 0);
    mEditTextMeasurementInterval.setText(Integer.toString(measurementIntervalValue));
  }

  private void setTemperatureMeasurementValue(float temperatureMeasurementValue) {

    /* Set the org.bluetooth.characteristic.temperature_measurement
     * characteristic to a byte array of size 5 so
     * we can use setValue(value, format, offset);
     *
     * Flags (8bit) + Temperature Measurement Value (float) = 5 bytes
     *
     * Flags:
     *   Temperature Units Flag (0) -> Celsius
     *   Time Stamp Flag (0) -> Time Stamp field not present
     *   Temperature Type Flag (0) -> Temperature Type field not present
     *   Unused (00000)
     */
    mTemperatureMeasurementCharacteristic.setValue(new byte[]{0b00000000, 0, 0, 0, 0});
    // Characteristic Value: [flags, 0, 0, 0, 0]
    int bits = Float.floatToIntBits(temperatureMeasurementValue);
    int sign = bits >>> 31;
    int exp = (bits >>> 23 & ((1 << 8) - 1)) - ((1 << 7) - 1);
    int mantissa = bits & ((1 << 23) - 1);
    mTemperatureMeasurementCharacteristic.setValue(mantissa, exp,
        TEMPERATURE_MEASUREMENT_VALUE_FORMAT,
        /* offset */ 1);
    // Characteristic Value: [flags, temperature measurement value]
  }

  private void setTimer(int measurementIntervalValue) {
    timer = new CountDownTimer(30000, measurementIntervalValue * 1000) {
      @Override
      public void onTick(long millisUntilFinished) {
        mDelegate.sendNotificationToDevices(mTemperatureMeasurementCharacteristic);
      }

      @Override
      public void onFinish() {
        timer.start();
      }
    }.start();
  }

  private void resetTimer(int measurementIntervalValue) {
    timer.cancel();
    setTimer(measurementIntervalValue);
  }

  private boolean isValidTemperatureMeasurementValue(String s) {
    try {
      float value = Float.valueOf(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidMeasurementIntervalValue(String s) {
    try {
      int value = Integer.parseInt(s);
      return (value >= MIN_MEASUREMENT_INTERVAL) && (value <= MAX_UINT16);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
    if (offset != 0) {
      return BluetoothGatt.GATT_INVALID_OFFSET;
    }
    // Measurement Interval is a 16bit characteristic
    if (value.length != 2) {
      return BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(value);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    final int newMeasurementIntervalValue = byteBuffer.getShort();
    if (!isValidMeasurementIntervalValue(Integer.toString(newMeasurementIntervalValue))) {
      return BluetoothGatt.GATT_FAILURE;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mMeasurementIntervalCharacteristic.setValue(newMeasurementIntervalValue,
            MEASUREMENT_INTERVAL_FORMAT,
            /* offset */ 0);
        resetTimer(newMeasurementIntervalValue);
      }
    });
    return BluetoothGatt.GATT_SUCCESS;
  }
}
