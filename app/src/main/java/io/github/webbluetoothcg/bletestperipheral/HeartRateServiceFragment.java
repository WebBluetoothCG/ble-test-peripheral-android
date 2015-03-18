package io.github.webbluetoothcg.bletestperipheral;


import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class HeartRateServiceFragment extends ServiceFragment {

  ///////////////////////
  ////// Constants //////
  ///////////////////////
  private static final UUID HEART_RATE_SERVICE_UUID = UUID
      .fromString("0000180D-0000-1000-8000-00805f9b34fb");

  private static final UUID BODY_SENSOR_LOCATION_UUID = UUID
      .fromString("00002A37-0000-1000-8000-00805f9b34fb");

  // TODO(g-ortuno): Implement Heart Rate Measurement and Heart Rate Control Point characteristics.
  private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
  private BluetoothGattService mHeartRateService;
  public HeartRateServiceFragment() {
    mBodySensorLocationCharacteristic =
        new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    mBodySensorLocationCharacteristic.setValue(/* Chest */ new byte[]{1});

    mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // TODO(g-ortuno): Implement front end.
    return inflater.inflate(R.layout.fragment_heart_rate, container, false);
  }

  @Override
  public BluetoothGattService getBluetoothGattService() {
    return mHeartRateService;
  }
}
