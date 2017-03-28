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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.UUID;


public class BatteryServiceFragment extends ServiceFragment {

  private static final UUID BATTERY_SERVICE_UUID = UUID
      .fromString("0000180F-0000-1000-8000-00805f9b34fb");

  private static final UUID BATTERY_LEVEL_UUID = UUID
      .fromString("00002A19-0000-1000-8000-00805f9b34fb");
  private static final int INITIAL_BATTERY_LEVEL = 50;
  private static final int BATTERY_LEVEL_MAX = 100;
  private static final String BATTERY_LEVEL_DESCRIPTION = "The current charge level of a " +
      "battery. 100% represents fully charged while 0% represents fully discharged.";

  private ServiceFragmentDelegate mDelegate;
  // UI
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
            Toast.makeText(getActivity(), R.string.batteryLevelTooHigh, Toast.LENGTH_SHORT)
                .show();
          }
        } else {
          Toast.makeText(getActivity(), R.string.batteryLevelIncorrect, Toast.LENGTH_SHORT)
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

  private final OnClickListener mNotifyButtonListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      mDelegate.sendNotificationToDevices(mBatteryLevelCharacteristic);
    }
  };

  // GATT
  private BluetoothGattService mBatteryService;
  private BluetoothGattCharacteristic mBatteryLevelCharacteristic;

  public BatteryServiceFragment() {
    mBatteryLevelCharacteristic =
        new BluetoothGattCharacteristic(BATTERY_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mBatteryLevelCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mBatteryLevelCharacteristic.addDescriptor(
        Peripheral.getCharacteristicUserDescriptionDescriptor(BATTERY_LEVEL_DESCRIPTION));

    mBatteryService = new BluetoothGattService(BATTERY_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mBatteryService.addCharacteristic(mBatteryLevelCharacteristic);
  }

  // Lifecycle callbacks
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_battery, container, false);

    mBatteryLevelEditText = (EditText) view.findViewById(R.id.textView_batteryLevel);
    mBatteryLevelEditText.setOnEditorActionListener(mOnEditorActionListener);
    mBatteryLevelSeekBar = (SeekBar) view.findViewById(R.id.seekBar_batteryLevel);
    mBatteryLevelSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    Button notifyButton = (Button) view.findViewById(R.id.button_batteryLevelNotify);
    notifyButton.setOnClickListener(mNotifyButtonListener);

    setBatteryLevel(INITIAL_BATTERY_LEVEL, null);
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
    return mBatteryService;
  }

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(BATTERY_SERVICE_UUID);
  }

  private void setBatteryLevel(int newBatteryLevel, View source) {
    mBatteryLevelCharacteristic.setValue(newBatteryLevel,
        BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 0);
    if (source != mBatteryLevelSeekBar) {
      mBatteryLevelSeekBar.setProgress(newBatteryLevel);
    }
    if (source != mBatteryLevelEditText) {
      mBatteryLevelEditText.setText(Integer.toString(newBatteryLevel));
    }
  }

  @Override
  public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
    if (characteristic.getUuid() != BATTERY_LEVEL_UUID) {
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
    if (characteristic.getUuid() != BATTERY_LEVEL_UUID) {
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
