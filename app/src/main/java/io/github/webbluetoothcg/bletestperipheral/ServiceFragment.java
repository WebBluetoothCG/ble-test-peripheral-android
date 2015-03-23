package io.github.webbluetoothcg.bletestperipheral;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public abstract class ServiceFragment extends Fragment{
  public abstract BluetoothGattService getBluetoothGattService();
  public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value){
    throw new UnsupportedOperationException("Method writeCharacteristic not overriden");
  };
  /**
   * This interface must be implemented by activities that contain a ServiceFragment to allow an
   * interaction in the fragment to be communicated to the activity.
   */
  public interface ServiceFragmentDelegate {
    void sendNotificationToDevices(BluetoothGattCharacteristic characteristic);
  }
}
