package net.binzume.android.bletest;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ListActivity {
	private BroadcastReceiver receiver;
	private BLETagDevice currentDevice = null;

	private ArrayList<BLETagDevice> devices = new ArrayList<BLETagDevice>();

	private DeviceListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		adapter = new DeviceListAdapter(this, devices);
		setListAdapter(adapter);

		Intent intent1 = new Intent(getApplicationContext(), BlePolingService.class);
		intent1.setAction("start");
		startService(intent1);

		findViewById(R.id.AlertButton1).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (currentDevice == null) {
					return;
				}
				Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
				intent.setAction("alert");
				intent.putExtra("addr", currentDevice.addr);
				intent.putExtra("value", 1);
				startService(intent);
			}
		});

		findViewById(R.id.AlertButton2).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (currentDevice == null) {
					return;
				}
				Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
				intent.setAction("alert");
				intent.putExtra("addr", currentDevice.addr);
				intent.putExtra("value", 2);
				startService(intent);
			}
		});

		findViewById(R.id.AlertStopButton).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (currentDevice == null) {
					return;
				}
				Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
				intent.setAction("alert");
				intent.putExtra("addr", currentDevice.addr);
				intent.putExtra("value", 0);
				startService(intent);
			}
		});

		findViewById(R.id.DisconnectButton).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
				intent.setAction("close");
				if (currentDevice != null) {
					intent.putExtra("addr", currentDevice.addr);
				}
				startService(intent);
			}
		});

		findViewById(R.id.RefreshButton).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
				intent.setAction("tellStatus");
				if (currentDevice != null) {
					intent.putExtra("addr", currentDevice.addr);
				}
				startService(intent);
			}
		});

	}

	@Override
	protected void onListItemClick(ListView l, View v, int pos, long id) {
		currentDevice = adapter.getItem(pos);
		update();
	}

	private void update() {
		if (currentDevice != null) {
			BLETagDevice d = currentDevice;
			((TextView) findViewById(R.id.NameText)).setText(d.addr);
			((TextView) findViewById(R.id.NameText)).setText("ADDR:" + d.addr + " (" + d.name + ")");
			((TextView) findViewById(R.id.StatusText)).setText(d.isConnected() ? "Connected " + d.lastRssi : "Disconnected");
			((TextView) findViewById(R.id.StatusText)).setTextColor(d.isConnected() ? Color.GREEN : Color.RED);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if ("bletag_status".equals(intent.getAction())) {
					BLETagDevice d = (BLETagDevice) intent.getSerializableExtra("device");
					if (d.equals(currentDevice)) {
						currentDevice = d;
						update();
					}
					adapter.add(d);
				}
				if ("button_pressed".equals(intent.getAction())) {
					BLETagDevice d = (BLETagDevice) intent.getSerializableExtra("device");
					if (d.equals(currentDevice)) {
						currentDevice = d;
						update();
						if (intent.getIntExtra("st", 0) != 0) {
							((TextView) findViewById(R.id.StatusText)).setTextColor(Color.BLUE);
						}
					}
					adapter.add(d);
				}
				if ("devices".equals(intent.getAction())) {
					Object[] dd = (Object[]) intent.getSerializableExtra("devices");
					for (Object d : dd) {
						adapter.add((BLETagDevice) d);
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction("bletag_status");
		filter.addAction("button_pressed");
		filter.addAction("devices");
		registerReceiver(receiver, filter);

		Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
		intent.setAction("tellStatus");
		startService(intent);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
	}

}
