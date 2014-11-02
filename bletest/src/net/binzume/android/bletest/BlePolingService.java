package net.binzume.android.bletest;

import java.util.HashMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class BlePolingService extends Service implements BLETagDevice.TagDeviceEventListener {

	private static final String TAG = BlePolingService.class.getName();

	private final Handler handler = new Handler();
	private final HashMap<String, BLETagDevice> devices = new HashMap<String, BLETagDevice>();

	public class BlePolingServiceBinder extends Binder {
		public BLETagDevice[] getDevices() {
			return devices.values().toArray(new BLETagDevice[0]);
		}
	}

	private int count = 0;

	@Override
	public void onCreate() {
		addDevice("01:DF:DC:42:00:1B");
		addDevice("00:14:DC:42:00:1B");
		addDevice("00:A8:DC:42:00:1B");
		addDevice("00:1B:DC:42:03:4E");
		setPollingInterval(300);

		/*
		Log.d("saifu", "startLeScan");
		final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
				Log.d(TAG, "Scan " + device.getName() + " addr:" + device.getAddress() + " rssi: " + rssi);
			}
		};

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		bluetoothAdapter.startLeScan(callback);

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "stopLeScan");
				bluetoothAdapter.stopLeScan(callback);
			}
		}, 5000);
		*/

		Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext());
		notificationBuilder.setTicker("BLEPolling");
		notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationBuilder.setContentTitle("BLEPollingService");
		notificationBuilder.setContentText("BLEPollingService");
		notificationBuilder.setAutoCancel(false);
		startForeground(1, notificationBuilder.build());
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		terminate();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return START_STICKY;
		if ("close".equals(intent.getAction())) {
			BLETagDevice d = findDevice(intent.getStringExtra("addr"));
			if (d != null) {
				d.disconnect();
			} else {
				terminate();
				stopSelf();
			}
			return START_NOT_STICKY;
		} else if ("tellStatus".equals(intent.getAction()) || "poll".equals(intent.getAction())) {
			BLETagDevice d = findDevice(intent.getStringExtra("addr"));
			if (d != null) {
				checkDevice(d);
			} else {
				Intent t = new Intent("devices");
				t.putExtra("devices", devices.values().toArray(new BLETagDevice[0]));
				sendBroadcast(t);
				checkDevices();
			}
			return START_STICKY;
		} else if ("alert".equals(intent.getAction())) {
			BLETagDevice d = findDevice(intent.getStringExtra("addr"));
			if (d != null) {
				d.setAlarm(intent.getIntExtra("value", 0));
			}
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		return new BlePolingServiceBinder();
	}

	private BLETagDevice addDevice(String addr) {
		BLETagDevice d = devices.get(addr);
		if (d == null) {
			d = new BLETagDevice(addr);
			d.setListener(this);
			devices.put(addr, d);
		}
		return d;
	}

	private BLETagDevice findDevice(String addr) {
		return devices.get(addr);
	}

	private void terminate() {
		Log.d(TAG, "terminate....");
		setPollingInterval(0);
		for (BLETagDevice d : devices.values()) {
			d.disconnect();
		}
		// stopForeground();
	}

	private void checkDevices() {
		Object[] dd = devices.values().toArray();

		if (dd.length > 0) {
			checkDevice((BLETagDevice) dd[count % dd.length]);
			count++;
		}
	}

	private void checkDevice(final BLETagDevice d) {
		if (d.getConnectState() == BLETagDevice.CONNECT_STATE_DISCONNECTED) {

			final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

			final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) { //  
				Log.e(TAG, "Available Bluetooth Adapter not found.");
				return;
			}

			BluetoothDevice dev = bluetoothAdapter.getRemoteDevice(d.addr);
			if (dev != null) {
				d.connect(dev, handler, this);
			} else {
				// bonded device
				for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
					Log.d(TAG, "Bonded " + device.getName());
					if (device.getAddress().equalsIgnoreCase(d.addr)) {
						d.connect(device, handler, this);
					}
				}
			}
		} else if (d.isConnected()) {
			d.readRemoteRssi();
			handler.postDelayed(new Runnable() {
				public void run() {
					d.checkBattery();
				}
			}, 1000);
		}
	}

	private void setPollingInterval(int interval) {
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent intent = new Intent(getApplicationContext(), BlePolingService.class);
		intent.setAction("poll");
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Log.d(TAG, "interval: " + interval);
		if (interval > 0) {
			long t = interval * 1000;
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + t, t, pendingIntent);
		} else {
			alarmManager.cancel(pendingIntent);
		}
	}

	@Override
	public void onStatusUpdated(final BLETagDevice d, int st) {
		sendStatus(d);

		if (st == 1) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					new GSSSensorApiClient().updateStatus(d.addr, "test", 0, d.lastRssi, d.battery);
					return null;
				}
			}.execute();
		}
	}

	private void sendStatus(BLETagDevice d) {
		Intent intent = new Intent("bletag_status");
		intent.putExtra("device", d);
		intent.putExtra("addr", d.addr);
		intent.putExtra("connected", d.isConnected());
		sendBroadcast(intent);
	}

	@Override
	public void onPressButton(final BLETagDevice d, int st) {
		Intent intent = new Intent("button_pressed");
		intent.putExtra("device", d);
		intent.putExtra("addr", d.addr);
		intent.putExtra("st", st);
		sendBroadcast(intent);

		if (st == 1) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					new GSSSensorApiClient().updateStatus(d.addr, "test", 1, d.lastRssi, d.battery);
					return null;
				}
			}.execute();
		}
	}

	@Override
	public void onLost(BLETagDevice d) {
		// TODO Auto-generated method stub

	}

}
