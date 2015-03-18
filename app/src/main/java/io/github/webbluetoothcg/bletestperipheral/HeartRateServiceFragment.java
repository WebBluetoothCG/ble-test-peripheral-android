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
      .fromString("00002A37-0000-1000-8000-00805f9b34fb");
  private static final int LOCATION_OTHER = 0;

  // TODO(g-ortuno): Implement Heart Rate Measurement and Heart Rate Control Point characteristics.
  private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
  private BluetoothGattService mHeartRateService;

  private final OnItemSelectedListener mSpinnerOnItemSelectedListener = new OnItemSelectedListener() {
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

    mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);
    Spinner spinner = (Spinner) view.findViewById(R.id.spinner_bodySensorLocation);
    spinner.setOnItemSelectedListener(mSpinnerOnItemSelectedListener);

    return view;
  }

  @Override
  public BluetoothGattService getBluetoothGattService() {
    return mHeartRateService;
  }

  private void setBodySensorLocationValue(int location) {
    mBodySensorLocationCharacteristic.setValue(new byte[]{(byte) location});
  }
}
