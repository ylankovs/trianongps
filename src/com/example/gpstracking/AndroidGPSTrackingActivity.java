package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AndroidGPSTrackingActivity extends Activity implements OnClickListener {

	// GPSTracker class
	GPSTracker gps;

	Button buttonStart;

	private String mess = "";
	private Timer myTimer;
	private double saveLong = 0;
	private double saveLat = 0;
	private static int TIMESTAMP_GPS = 5000;
	private static String filename = "gpscoords";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Toast.makeText(getApplicationContext(), "Created service", Toast.LENGTH_LONG).show();
		super.onCreate(icicle);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		//	    buttonStop = (Button) findViewById(R.id.buttonStop);

		buttonStart.setOnClickListener(this);
		//	    buttonStop.setOnClickListener(this);

	}


	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonStart:
			EditText text = (EditText)findViewById(R.id.userId);
			mess = text.getText().toString();
			if (mess != null || mess != "")
			{
				myTimer = new Timer();
				myTimer.schedule(new TimerTask() {			
					@Override
					public void run() {
						TimerMethod();
					}

				}, 0, TIMESTAMP_GPS);
				moveTaskToBack(true);
				break;
				//    case R.id.buttonStop:
				//      stopService(new Intent(this, MyService.class));
				//      break;
			}
		}
	}

	private void TimerMethod()
	{
		//This method is called directly by the timer
		//and runs in the same thread as the timer.

		//We call the method that will work with the UI
		//through the runOnUiThread method.
		this.runOnUiThread(Timer_Tick);
	}

	private double distance(double lat1, double lon1, double lat2, double lon2)
	{
		double R = 6371.0; // radius of the earth in km
		double dLat = (lat2 - lat1) * Math.PI / 180;
		double dLon = (lon2 - lon1) * Math.PI / 180;
		double a = 0.5 - Math.cos(dLat) / 2 + Math.cos(lat1 * Math.PI / 180) 
				* Math.cos(lat2 * Math.PI / 180) * (1 - Math.cos(dLon)) / 2; 

		return 1000 * R * 2 * Math.asin(Math.sqrt(a));
	}

	private double speed(double dist, int t)
	{
		return dist * 1000 / t;
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.    	       

			//Do something to the UI thread here
			gps = new GPSTracker(AndroidGPSTrackingActivity.this);

			Toast.makeText(getApplicationContext(), "Getting GPS location", Toast.LENGTH_LONG).show();

			// check if GPS enabled		
			if(gps.canGetLocation()){
				Toast.makeText(getApplicationContext(), "Got GPS location\n" + gps.getLatitude() + "\n" + gps.getLongitude(), Toast.LENGTH_LONG).show();
				double latitude = gps.getLatitude();
				double longitude = gps.getLongitude();
				double dist = distance(saveLat, saveLong, latitude, longitude);
				double sp = speed(dist, TIMESTAMP_GPS);

				Toast.makeText(getApplicationContext(), "Distance: " + dist + "\nSpeed: " + sp, Toast.LENGTH_LONG).show();
				if (dist >= 0)//0.0002)
				{
					// \n is for new line
					Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

					String params = "idAg=" + mess + "&Latt=" + latitude + "&Lngt=" + longitude;

					ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

					State mobile = connManager.getNetworkInfo(0).getState();
					State wifi = connManager.getNetworkInfo(1).getState();

					if (mobile == NetworkInfo.State.CONNECTED)
					{
						HttpURLConnection urlConnection = null;
						URL url = null;
						String err = null;
						try {
							url = new URL("http://91.217.202.15:8080/tracking/track.php?" + params);
							urlConnection = (HttpURLConnection) url.openConnection();
							InputStream in = new BufferedInputStream(urlConnection.getInputStream());
							//readStream(in);
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							err = e.getMessage();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							err = e.getMessage();
						}
						finally {
							urlConnection.disconnect();
						}

						if (err != null)
						{
							Toast.makeText(getApplicationContext(), "Error in the program." + err, Toast.LENGTH_LONG).show();
						}
					}
					else
					{
						Toast.makeText(getApplicationContext(), "Trying file writes", Toast.LENGTH_LONG).show();
						FileOutputStream outputStream;

						try {
							outputStream = openFileOutput(filename, Context.MODE_APPEND);
							outputStream.write(("^" + params).getBytes());
							outputStream.close();
							Toast.makeText(getApplicationContext(), "Wrote to file", Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), "Error in the program." + e.getMessage(), Toast.LENGTH_LONG).show();
						}
						
						StringBuilder sb = new StringBuilder();
						try{
							FileInputStream is = new FileInputStream(filename);
							BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
							String line = null;
							while ((line = reader.readLine()) != null) {
								sb.append(line);
							}
							is.close();
							Toast.makeText(getApplicationContext(), "Read from file: " + sb, Toast.LENGTH_LONG).show();
						} catch(OutOfMemoryError om){
							om.printStackTrace();
							Toast.makeText(getApplicationContext(), "Out of memory to read file", Toast.LENGTH_LONG).show();
						} catch(Exception ex){
							ex.printStackTrace();
							Toast.makeText(getApplicationContext(), "Error in the program." + ex.getMessage(), Toast.LENGTH_LONG).show();
						}
					}

					/*
					if (err != null)
						Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\n" + err, Toast.LENGTH_LONG).show();
					else
						Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
					 */

					saveLat = latitude;
					saveLong = longitude;
				}
			}else{
				// can't get location
				// GPS or Network is not enabled
				// Ask user to enable GPS/network in settings
				gps.showSettingsAlert();
			}
		}
	};

}