package net.binzume.android.bletest;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter {

	private final LayoutInflater inflator;
	private final ArrayList<BLETagDevice> devices;

	public DeviceListAdapter(Context context, ArrayList<BLETagDevice> devices) {
		this.devices = devices;
		inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflator.inflate(R.layout.device_list_row, parent, false);
		}
		final BLETagDevice d = devices.get(position);
		((TextView) convertView.findViewById(R.id.NameText)).setText(d.addr);
		((TextView) convertView.findViewById(R.id.NameText)).setText( "ADDR:" + d.addr + " (" + d.name + ")");
		((TextView) convertView.findViewById(R.id.StatusText)).setText(d.isConnected() ? "Connected " + d.lastRssi : "Disconnected");
		((TextView) convertView.findViewById(R.id.StatusText)).setTextColor(d.isConnected() ? Color.GREEN : Color.RED);
		return convertView;
	}

	@Override
	public int getCount() {
		return devices.size();
	}

	@Override
	public BLETagDevice getItem(int position) {
		return devices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public BLETagDevice add(BLETagDevice d) {
		int p = devices.indexOf(d);
		if (p < 0) {
			devices.add(d);
		} else {
			devices.set(p, d);
		}
		notifyDataSetChanged();
		return d;
	}

}
