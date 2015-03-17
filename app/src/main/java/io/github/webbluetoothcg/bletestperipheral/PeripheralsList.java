package io.github.webbluetoothcg.bletestperipheral;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class PeripheralsList extends ListActivity {

  // TODO(g-ortuno): Add more services
  private static final String[] SERVICES_NAMES = new String[]{"Battery Service"};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_peripherals_list);
    PeripheralsListAdapter adapter = new PeripheralsListAdapter(this, SERVICES_NAMES);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);

    Intent intent = new Intent(this, Peripherals.class);
    // TODO(g-ortuno): Pass the selected service to the peripheral activity
    startActivity(intent);
  }

  private class PeripheralsListAdapter extends BaseAdapter{

    private LayoutInflater mLayoutInflater;
    private String[] mServicesNames;

    public PeripheralsListAdapter(Context context, String[] servicesNames) {
      mLayoutInflater = LayoutInflater.from(context);
      mServicesNames = servicesNames;
    }

    @Override
    public int getCount() {
      return mServicesNames.length;
    }

    @Override
    public String getItem(int position) {
      return mServicesNames[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view;
      // We create a view holder to save a reference to the views in the row. That way we don't
      // have to call findViewById() every time.
      ViewHolder holder;
      // If convertView is null then the row is being added for the first time and we
      // need to inflate its view and save references to its children views.
      if (convertView == null) {
        view = mLayoutInflater.inflate(R.layout.list_item_peripheral, parent,
         // Layout will be inflated but won't be attached to any other layout (so it won't be drawn,
         // receive touch events etc).
         /* attach to root */ false);
        holder = new ViewHolder();
        holder.textViewServiceName = (TextView) view.findViewById(R.id.textView_serviceName);
        // Set tag to store data within a view without resorting to another data structure.
        view.setTag(holder);
      } else {
        view = convertView;
        holder = (ViewHolder) view.getTag();
      }
      holder.textViewServiceName.setText(getItem(position));

      return view;
    }

    private class ViewHolder {
      public TextView textViewServiceName;
    }
  }
}