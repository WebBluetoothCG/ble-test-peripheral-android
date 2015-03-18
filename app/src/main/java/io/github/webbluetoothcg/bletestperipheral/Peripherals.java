package io.github.webbluetoothcg.bletestperipheral;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class Peripherals extends ListActivity {

  // TODO(g-ortuno): Implement heart rate monitor peripheral
  private static final String[] PERIPHERALS_NAMES = new String[]{"Battery", "Heart Rate Monitor"};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_peripherals_list);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
        /* layout for the list item */ android.R.layout.simple_list_item_1,
        /* id of the TextView to use */ android.R.id.text1,
        /* values for the list */ PERIPHERALS_NAMES);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);

    Intent intent = new Intent(this, Peripheral.class);
    // TODO(g-ortuno): Pass the selected peripheral to the peripheral activity.
    startActivity(intent);
  }

}
