package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import android.view.ViewGroup.LayoutParams;

@SuppressLint("SimpleDateFormat") public class AndroidGPSTrackingActivity extends Activity implements OnClickListener {

	//The "x" and "y" position of the "Show Button" on screen.
	Point p;

	Button buttonLoad;
	Button buttonSend;

	// GPSTracker class
	GPSTracker gps;

	private Spinner spinner1, spinner2;

	private String mess = "";
	private Timer myTimer;
	private double saveLong = 0;
	private double saveLat = 0;

	private static int TIMESTAMP_GPS = 30000;
	private static int NUM_COORD_PARAMS = 3;
	private static int NUM_ALLOWED_SETS_PER_TX = 65;

	private static String filenameGPS = "gpscoords";
	private static String filenameMSG = "messages";
	private static String filenameADRS = "addresses";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Toast.makeText(getApplicationContext(), "Created service", Toast.LENGTH_LONG).show();
		super.onCreate(icicle);
		setContentView(R.layout.gps);

		buttonLoad = (Button) findViewById(R.id.button1);
		buttonLoad.setOnClickListener(this);

		buttonSend = (Button) findViewById(R.id.button2);
		buttonSend.setOnClickListener(this);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		mess = extras.getString("emp");

		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {			
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, TIMESTAMP_GPS);

		moveTaskToBack(true);

		spinner1 = (Spinner)findViewById(R.id.spinner1);
		ArrayAdapter<String> spinnerAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
		spinnerAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(spinnerAdapter1);
		spinnerAdapter1.add("клиент");
		spinnerAdapter1.notifyDataSetChanged();
		
		spinner2 = (Spinner)findViewById(R.id.spinner2);
		ArrayAdapter<String> spinnerAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
		spinnerAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(spinnerAdapter2);
		spinnerAdapter2.add("платеж");
		spinnerAdapter2.add("заказ");
		spinnerAdapter2.add("инвентаризация");
		spinnerAdapter2.add("прочее");
		spinnerAdapter2.notifyDataSetChanged();
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

			// check if GPS enabled		
			if(gps.canGetLocation()){
				String time = new SimpleDateFormat("yyyyMMddkkmmss").format(new Date());

				double latitude = gps.getLatitude();
				double longitude = gps.getLongitude();
				double dist = distance(saveLat, saveLong, latitude, longitude);
				double sp = speed(dist, TIMESTAMP_GPS);

				Toast.makeText(getApplicationContext(), "Getting GPS coordinates \nprev=" + saveLong + " " + saveLat + "\nnew=" + longitude + " " + latitude + " " + Math.abs((saveLong - longitude) + Math.abs(saveLat - latitude)), Toast.LENGTH_LONG).show();

				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

				State mobile = connManager.getNetworkInfo(0).getState();
				State wifi = connManager.getNetworkInfo(1).getState();

				Toast.makeText(getApplicationContext(), "Distance: " + dist + "\nSpeed: " + sp, Toast.LENGTH_LONG).show();
				String params = latitude + "^" + longitude + "^" + time;

				if (mobile == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTED)
				{
					if (dist >= 0.0002)
					{
						StringBuilder sb = new StringBuilder("");
						try{
							int numLines = 0;
							InputStream is = openFileInput(filenameGPS);
							if ( is != null ) {
								InputStreamReader inputStreamReader = new InputStreamReader(is);
								BufferedReader reader = new BufferedReader(inputStreamReader);
								String line = null;
								while ((line = reader.readLine()) != null) {
									sb.append(line);
									numLines ++;
								}
							}
							is.close();
							Toast.makeText(getApplicationContext(), "Read from file: " + sb + " lines= " + numLines, Toast.LENGTH_LONG).show();

							File dir = getFilesDir();
							File file = new File(dir, filenameGPS);
							boolean deleted = file.delete();
							Toast.makeText(getApplicationContext(), "File delete: " + (deleted ? "yes" : "no"), Toast.LENGTH_LONG).show();
						} catch(OutOfMemoryError om){
							om.printStackTrace();
							Toast.makeText(getApplicationContext(), "Out of memory to read file", Toast.LENGTH_LONG).show();
						} catch(Exception ex){
							ex.printStackTrace();
							Toast.makeText(getApplicationContext(), "Error in the program dist>0.0002 " + ex.getMessage(), Toast.LENGTH_LONG).show();
						}

						if(sb != null && !sb.toString().equals(""))
						{
							sb.deleteCharAt(0);
							sb.append("^" + params);
						}
						else
							sb.append(params);

						Toast.makeText(getApplicationContext(), "Full list: " + sb, Toast.LENGTH_LONG).show();

						// \n is for new line
						//Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

						HttpURLConnection urlConnection = null;
						URL url = null;
						String err = null;
						try {
							String delims = "[\\^]";
							String[] split = (sb.toString()).split(delims);
							int nSets = split.length / NUM_COORD_PARAMS; 
							int nTx = (int)Math.ceil((double)nSets / NUM_ALLOWED_SETS_PER_TX);
							
							Toast.makeText(getApplicationContext(), "split= " + split + " len= " + split.length + " nSets= " + nSets 
									+ " nTx= " + nTx, Toast.LENGTH_LONG).show();

							int nCnt = 0;
							for (int i=0; i<nTx; i++)
							{
								String sendingParams = "idAg=" + mess + "&sData=";
								for(int j=0; j<((nSets - nCnt) > NUM_ALLOWED_SETS_PER_TX ? NUM_ALLOWED_SETS_PER_TX*NUM_COORD_PARAMS
										: (nSets - nCnt) * NUM_COORD_PARAMS); j+=3)
								{
									sendingParams += split[j] + "^" + split[j+1] + "^" + split[j+2] + "^";
									nCnt ++;
								}
								
								Toast.makeText(getApplicationContext(), "Sending request: http://91.217.202.15:8080/tracking/track.php?"
										+ sendingParams, Toast.LENGTH_LONG).show();

								sendingParams.substring(0, (sendingParams.length() - 1)); // cut off the first ^ symbol
								url = new URL("http://91.217.202.15:8080/tracking/track.php?" + sendingParams);
								urlConnection = (HttpURLConnection) url.openConnection();
								Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
								InputStream in = new BufferedInputStream(urlConnection.getInputStream());
								//readStream(in);
								urlConnection.disconnect();
							}
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							err = e.getMessage();
							Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							err = e.getMessage();
							Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
						}
						finally {
							if (urlConnection != null)
								urlConnection.disconnect();
						}

						if (err != null)
							Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\n" + err, Toast.LENGTH_LONG).show();
						else
							Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

						saveLat = latitude;
						saveLong = longitude;
					}
					else
					{
						StringBuilder sb = new StringBuilder("");
						try{
							InputStream is = openFileInput(filenameGPS);
							if ( is != null ) {
								InputStreamReader inputStreamReader = new InputStreamReader(is);
								BufferedReader reader = new BufferedReader(inputStreamReader);
								String line = null;
								while ((line = reader.readLine()) != null) {
									sb.append(line);
								}
							}
							is.close();
							Toast.makeText(getApplicationContext(), "Read from file: " + sb, Toast.LENGTH_LONG).show();

							File dir = getFilesDir();
							File file = new File(dir, filenameGPS);
							boolean deleted = file.delete();
							Toast.makeText(getApplicationContext(), "File delete: " + (deleted ? "yes" : "no"), Toast.LENGTH_LONG).show();
						} catch(OutOfMemoryError om){
							om.printStackTrace();
							Toast.makeText(getApplicationContext(), "Out of memory to read file", Toast.LENGTH_LONG).show();
						} catch(Exception ex){
							ex.printStackTrace();
							Toast.makeText(getApplicationContext(), "Error in the program dist not checked " + ex.getMessage(), Toast.LENGTH_LONG).show();
						}

						if(sb != null && !sb.toString().equals(""))
						{
							sb.deleteCharAt(0);
							Toast.makeText(getApplicationContext(), "Full list: " + sb, Toast.LENGTH_LONG).show();

							// \n is for new line
							//Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

							HttpURLConnection urlConnection = null;
							URL url = null;
							String err = null;
							try {
								String delims = "[\\^]";
								String[] split = (sb.toString()).split(delims);
								int nSets = split.length / NUM_COORD_PARAMS; 
								int nTx = (int)Math.ceil(nSets / NUM_ALLOWED_SETS_PER_TX);

								int nCnt = 0;
								for (int i=0; i<nTx; i++)
								{
									String sendingParams = "idAg=" + mess + "&sData=";
									for(int j=0; j<((nSets - nCnt) > NUM_ALLOWED_SETS_PER_TX ? NUM_ALLOWED_SETS_PER_TX*NUM_COORD_PARAMS
											: (nSets - nCnt) * NUM_COORD_PARAMS); j+=3)
									{
										sendingParams += split[j] + "^" + split[j+1] + "^" + split[j+2] + "^";
										nCnt ++;
									}

									sendingParams.substring(0, (sendingParams.length() - 1)); // cut off the first ^ symbol
									url = new URL("http://91.217.202.15:8080/tracking/track.php?" + sendingParams);
									urlConnection = (HttpURLConnection) url.openConnection();
									Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
									InputStream in = new BufferedInputStream(urlConnection.getInputStream());
									urlConnection.disconnect();
									//readStream(in);
								}
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								err = e.getMessage();
								Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								err = e.getMessage();
								Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
							}
							finally {
								if (urlConnection != null)
									urlConnection.disconnect();
							}
						}
					}
				}
				else
				{
					if (dist >= 0.0002)
					{
						Toast.makeText(getApplicationContext(), "Trying file writes", Toast.LENGTH_LONG).show();
						FileOutputStream outputStream;

						try {
							outputStream = openFileOutput(filenameGPS, Context.MODE_APPEND);
							outputStream.write(("^" + params).getBytes());
							outputStream.close();
							Toast.makeText(getApplicationContext(), "Wrote to file", Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), "Error in the program." + e.getMessage(), Toast.LENGTH_LONG).show();
						}

						saveLat = latitude;
						saveLong = longitude;
					}
				}
			}
			else
			{
				// can't get location
				// GPS or Network is not enabled
				// Ask user to enable GPS/network in settings
				gps.showSettingsAlert();
			}
		}
	};

	@Override
	public void onClick(View src) {
		String err = null;

		switch (src.getId()) {
		case R.id.button1: // buttonLoad
			try {
				DefaultHttpClient httpclient = new DefaultHttpClient();

				HttpGet httppost = new HttpGet("http://www.urlOfThePageYouWantToRead.nl/text.txt");
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity ht = response.getEntity();

				BufferedHttpEntity buf = new BufferedHttpEntity(ht);

				InputStream is = buf.getContent();


				BufferedReader r = new BufferedReader(new InputStreamReader(is));

				StringBuilder total = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					total.append(line + "\n");
				}

				Toast.makeText(getApplicationContext(), "Read from server:\n" + total, Toast.LENGTH_LONG).show();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				err = e.getMessage();
				Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				err = e.getMessage();
				Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.button2: // buttonSave
			HttpURLConnection urlConnection = null;
			URL url = null;
			String sendingParams = null;
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			State mobile = connManager.getNetworkInfo(0).getState();
			State wifi = connManager.getNetworkInfo(1).getState();

			if (mobile == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTED)
			{
				try {
					url = new URL("http://91.217.202.15:8080/tracking/track.php?" + sendingParams);
					urlConnection = (HttpURLConnection) url.openConnection();
					Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					urlConnection.disconnect();
					//readStream(in);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					err = e.getMessage();
					Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					err = e.getMessage();
					Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
				}
				finally {
					if (urlConnection != null)
						urlConnection.disconnect();
				}
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Trying file writes", Toast.LENGTH_LONG).show();
				FileOutputStream outputStream;

				try {
					outputStream = openFileOutput(filenameMSG, Context.MODE_APPEND);
					outputStream.write(("^" + sendingParams).getBytes());
					outputStream.close();
					Toast.makeText(getApplicationContext(), "Wrote to file", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Error in the program." + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
			break;
		}
	}
}