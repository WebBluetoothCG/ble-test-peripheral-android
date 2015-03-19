package io.github.webbluetoothcg.bletestperipheral;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public abstract class ServiceFragment extends Fragment{
  public abstract BluetoothGattService getBluetoothGattService();
  /**
   * This interface must be implemented by activities that contain a ServiceFragment to allow an
   * interaction in the fragment to be communicated to the activity.
   */
  public interface OnFragmentInteractionListener {
    void onNotifyButtonPressed(BluetoothGattCharacteristic characteristic);
  }
}
