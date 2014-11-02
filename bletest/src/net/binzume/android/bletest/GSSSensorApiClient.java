package net.binzume.android.bletest;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class GSSSensorApiClient extends JsonApiClient {

	private static final String TAG = "GSSSensorApiClient";

	public GSSSensorApiClient() {
		super(Constants.API_URL);
	}
	
	public void updateStatus(String addr, String name, int st, int rssi, int battery) {
		Params params = new Params();
		params.put("key", Constants.API_KEY);
		params.put("name", name);
		params.put("status", String.valueOf(st));
		params.put("rssi", String.valueOf(rssi));
		params.put("battery", String.valueOf(battery));
		params.put("timestamp", String.valueOf(System.currentTimeMillis()));

		try {
			JSONObject json = post(addr, params);
			Log.d(TAG, "res:" + json.toString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		}

	}
}
