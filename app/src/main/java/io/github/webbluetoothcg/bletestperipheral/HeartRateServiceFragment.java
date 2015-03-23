package io.github.webbluetoothcg.bletestperipheral;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import java.util.UUID;

public class HeartRateServiceFragment extends ServiceFragment {

  /**
   * See <a href="https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.heart_rate.xml">
   * Heart Rate Service</a>
   */
  private static final UUID HEART_RATE_SERVICE_UUID = UUID
      .fromString("0000180D-0000-1000-8000-00805f9b34fb");

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
  private static final UUID HEART_RATE_CONTORL_POINT_UUID = UUID
      .fromString("00002A39-0000-1000-8000-00805f9b34fb");

  // TODO(g-ortuno): Implement Heart Rate Measurement and Heart Rate Control Point characteristics.
  private BluetoothGattCharacteristic mHeartRateControlPoint;
  private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
  private BluetoothGattService mHeartRateService;
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
  public HeartRateServiceFragment() {
    mBodySensorLocationCharacteristic =
        new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    setBodySensorLocationValue(LOCATION_OTHER);

    mHeartRateControlPoint =
        new BluetoothGattCharacteristic(HEART_RATE_CONTORL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE);

    mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
    mHeartRateService.addCharacteristic(mHeartRateControlPoint);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);
    Spinner spinner = (Spinner) view.findViewById(R.id.spinner_bodySensorLocation);
    spinner.setOnItemSelectedListener(mLocationSpinnerOnItemSelectedListener);

    return view;
  }

  @Override
  public BluetoothGattService getBluetoothGattService() {
    return mHeartRateService;
  }

  private void setBodySensorLocationValue(int location) {
    mBodySensorLocationCharacteristic.setValue(new byte[]{(byte) location});
  }

  @Override
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
    // Heart Rate control point is a 8bit characteristic
    if (value.length == 1) {
      if ((value[0] & 1) == 1) {
        // TODO(g-ortuno): Reset UI
        // TODO(g-ortuno): Reset Value in Heart Rate Measurement
      }
    }
  }
}
