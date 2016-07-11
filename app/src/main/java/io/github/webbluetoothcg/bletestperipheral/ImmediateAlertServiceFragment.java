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
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;


public class ImmediateAlertServiceFragment extends ServiceFragment {

  private static final byte NO_ALERT = 0x00;
  private static final byte MILD_ALERT = 0x01;
  private static final byte HIGH_ALERT = 0x02;

  private static final String TAG = ImmediateAlertServiceFragment.class.getCanonicalName();

  private static final UUID IMMEDIATE_ALERT_SERVICE_UUID = UUID
      .fromString("00001802-0000-1000-8000-00805f9b34fb");

  private static final UUID ALERT_LEVEL_UUID = UUID
      .fromString("00002A06-0000-1000-8000-00805f9b34fb");

  private ServiceFragmentDelegate mDelegate;

  // GATT
  private BluetoothGattService mImmediateAlertService;
  private BluetoothGattCharacteristic mBatteryLevelCharacteristic;
  private TextView textView_receivedValue;

  public ImmediateAlertServiceFragment() {
    mBatteryLevelCharacteristic =
        new BluetoothGattCharacteristic(ALERT_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE);

    mBatteryLevelCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mImmediateAlertService = new BluetoothGattService(IMMEDIATE_ALERT_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mImmediateAlertService.addCharacteristic(mBatteryLevelCharacteristic);
  }

  // Lifecycle callbacks
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_immediate_alert, container, false);

    textView_receivedValue = (TextView) view.findViewById(R.id.textView_receivedValue);

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

  public BluetoothGattService getBluetoothGattService() {
    return mImmediateAlertService;
  }

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(IMMEDIATE_ALERT_SERVICE_UUID);
  }

  @Override
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
    //Log.v(TAG, "write Characteristic " + characteristic.getUuid() + " value " + value[0]);
    /**
     * https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.alert_level.xml
     *
     * The value of the characteristic shall be an unsigned 8 bit integer that has a fixed point
     * exponent of 0. The Alert Level characteristic defines the level of alert, and is one of the
     * following three values:
     *
     * Value 0, meaning “No Alert”
     * Value 1, meaning “Mild Alert”
     * Value 2, meaning “High Alert”
     *
     */
    final byte alertLevel = value[0];

    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        textView_receivedValue.setText(String.valueOf(alertLevel));
      }
    });

    switch (alertLevel){
      case NO_ALERT:
      case MILD_ALERT:
      case HIGH_ALERT:
        return BluetoothGatt.GATT_SUCCESS;
        //break;

      default:
        return BluetoothGatt.GATT_WRITE_NOT_PERMITTED;
        //break;
    }
  }
}
