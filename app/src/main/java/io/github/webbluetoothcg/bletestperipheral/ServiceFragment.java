package io.github.webbluetoothcg.bletestperipheral;

import android.app.Fragment;
import android.bluetooth.BluetoothGattService;

public abstract class ServiceFragment extends Fragment{
  public abstract BluetoothGattService getBluetoothGattService();
}
