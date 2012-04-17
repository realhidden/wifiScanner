package kibu.wifi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class KibuwifiActivity extends Activity {
	private static final long SCAN_DELAY = 2000;
	private static String saveDir = "kibu";
	private TextView txtAssoc;
	private TextView txtScan;
	private Button saveButton;
	private EditText textFilename;
	private IntentFilter i;
	private BroadcastReceiver receiver;
	private Timer timer;
	private String result = "";

	private List<ScanResult> list2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// wake lock
		/*
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		wl.acquire();
		*/

		txtAssoc = (TextView) findViewById(R.id.txtAssoc);
		txtScan = (TextView) findViewById(R.id.txtScan);
		textFilename = (EditText) findViewById(R.id.editText1);

		i = new IntentFilter();
		i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				WifiManager w = (WifiManager) c
						.getSystemService(Context.WIFI_SERVICE);

				list2 = w.getScanResults();
				StringBuilder sb = new StringBuilder("Scan Results:\n");
				sb.append("-----------------------\n");
				for (ScanResult r : list2) {
					sb.append(r.SSID + " " + r.BSSID + "" + r.level + " dBM\n");
				}
				txtScan.setText(sb.toString());
			}
		};

		saveButton = (Button) findViewById(R.id.button1);
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					List<ScanResult> scanResult = list2;
					// save to temp
					JSONArray jArray = new JSONArray();
					for (ScanResult r : list2) {
						JSONObject jObject = new JSONObject();

						jObject.put("SSID", r.SSID);
						jObject.put("BSSID", r.BSSID);
						jObject.put("level", r.level);
						jObject.put("frequency", r.frequency);
						jObject.put("capabilities", r.capabilities);

						jArray.put(jObject);
					}

					writeToFile(textFilename.getText().toString(),
							jArray.toString());

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					showAlert("JSON exception" + e.getMessage());
				}
			}
		});
	}

	public void writeToFile(String filename, String data) {
		String baseDir = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + File.separator + saveDir + File.separator;
		try {

			new File(baseDir).mkdirs();

			File f = new File(baseDir + filename + ".json");
			if (f.exists() == true) {
				throw new Exception("Choose different name!");
			}
			f.createNewFile();
			FileOutputStream stream = new FileOutputStream(f);
			stream.write(data.getBytes());
			stream.close();

			showAlert("File saved: " + filename);
		} catch (Exception e) {
			showAlert("Error file writing: " + baseDir + filename + ".json"
					+ "\n:" + e.getMessage());
		}
	}

	public void showAlert(String message) {
		new AlertDialog.Builder(this)
				.setMessage(message)
				.setTitle("Wifi")
				.setCancelable(true)
				.setNeutralButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						}).show();
	}

	@Override
	protected void onResume() {
		super.onResume();

		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

				if (wm.isWifiEnabled()) {
					WifiInfo info = wm.getConnectionInfo();
					if (info != null) {
						result = "Associated with " + info.getSSID() + "\nat "
								+ info.getLinkSpeed()
								+ WifiInfo.LINK_SPEED_UNITS + " ("
								+ info.getRssi() + " dBM)";
					} else {
						result = "Not currently associated.";
					} 
					wm.startScan();
				} else {
					result = "WIFI is disabled.";
				}

				// set text back to ui
				Runnable rr = new Runnable() {

					public void run() {
						TextView txtView = (TextView) findViewById(R.id.txtAssoc);
						txtView.setText("State: " + result);
					}
				};

				runOnUiThread(rr);

			}
		}, 0, SCAN_DELAY);
		registerReceiver(receiver, i);
	}

	@Override
	protected void onPause() {
		super.onPause();

		timer.cancel();
		unregisterReceiver(receiver);
	}
}