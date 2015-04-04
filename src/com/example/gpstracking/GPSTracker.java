package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.gpstracking.R;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.example.gpstracking.MainActivity;

@SuppressLint("SimpleDateFormat") public class GPSTracker extends Service {

	public static final String DATABASE_NAME = "GPSLOGGERDB";
	public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";
	public static final String TRIPS_TABLE_NAME = "TRIPS";

	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");

	private LocationManager lm;
	private LocationListener locationListener;

	private static long minTimeMillis = 20000;
	private static long minDistanceMeters = 30;
	private static float minAccuracyMeters = 50;

	private int lastStatus = 0;
	private static boolean showingDebugToast = true;

	private static final String tag = "GPSTracker";
	boolean gps_enabled = false;
	boolean network_enabled = false;

	private static double saveLong = 0;
	private static double saveLat = 0;

	private static int NUM_COORD_PARAMS = 3;
	private static int NUM_ALLOWED_SETS_PER_TX = 65;

	private static String filenameGPS = "gpscoords";

	private HttpURLConnection urlConnection = null;
	private URL url = null;
	private ConnectivityManager connManager = null;

	private State mobile = null;
	private State wifi = null;

	/** Called when the activity is first created. */
	private void startLoggerService() {
		// ---use the LocationManager class to obtain GPS locations---
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationListener = new MyLocationListener();

		gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		//Toast.makeText(getApplicationContext(), "Enabled gps:" + gps_enabled + " net:" + network_enabled, 
		//		Toast.LENGTH_LONG).show();

		if (gps_enabled)
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					minTimeMillis,
					minDistanceMeters,
					locationListener);
		if (network_enabled)
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					minTimeMillis,
					minDistanceMeters,
					locationListener);
	}

	private void shutdownLoggerService() {
		lm.removeUpdates(locationListener);
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

	public class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			String err = null;
			if (loc != null) {
				boolean pointIsRecorded = false;
				try {
					if (loc.hasAccuracy() && loc.getAccuracy() <= minAccuracyMeters) {
						pointIsRecorded = true;

						double dist = distance(saveLat, saveLong, loc.getLatitude(), loc.getLongitude());
						String time = new SimpleDateFormat("yyyyMMddkkmmss").format(new Date());

						connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

						mobile = connManager.getNetworkInfo(0).getState();
						wifi = connManager.getNetworkInfo(1).getState();

						//				Toast.makeText(getApplicationContext(), "Distance: " + dist + "\nSpeed: " + sp, Toast.LENGTH_LONG).show();
						String params = loc.getLatitude() + "^" + loc.getLongitude() + "^" + time;

						StringBuilder sb = null;
						if (mobile == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTED)
						{
							if (dist >= 0.0002)
							{
								sb = new StringBuilder("");
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
								} catch(OutOfMemoryError om){
									om.printStackTrace();
									Toast.makeText(getApplicationContext(), "Out of memory to read file", Toast.LENGTH_LONG).show();
								} catch(Exception ex){
									ex.printStackTrace();
									Toast.makeText(getApplicationContext(), "Cannot read file " + ex.getMessage(), Toast.LENGTH_LONG).show();
								}

								if(sb != null && !sb.toString().equals(""))
								{
									sb.deleteCharAt(0);
									sb.append("^" + params);
								}
								else
									sb.append(params);

								urlConnection = null;
								url = null;
								err = null;
								try {
									String delims = "[\\^]";
									String[] split = (sb.toString()).split(delims);
									int nSets = split.length / NUM_COORD_PARAMS; 
									int nTx = (int)Math.ceil((double)nSets / NUM_ALLOWED_SETS_PER_TX);

									int nCnt = 0;
									for (int i=0; i<nTx; i++)
									{
										String sendingParams = "idAg=" + MainActivity.getEmpId() + "&sData=";
										for(int j=0; j<((nSets - nCnt) > NUM_ALLOWED_SETS_PER_TX ? NUM_ALLOWED_SETS_PER_TX*NUM_COORD_PARAMS
												: (nSets - nCnt) * NUM_COORD_PARAMS); j+=NUM_COORD_PARAMS)
										{
											sendingParams += split[j] + "^" + split[j+1] + "^" + split[j+2] + "^";
											nCnt ++;
										}

										//Toast.makeText(getApplicationContext(), "Sending request: http://91.217.202.15:8080/tracking/track.php?"
										//		+ sendingParams, Toast.LENGTH_LONG).show();

										sendingParams.substring(0, (sendingParams.length() - 1)); // cut off the first ^ symbol
										url = new URL("http://91.217.202.15:8080/tracking/track.php?" + sendingParams);
										urlConnection = (HttpURLConnection) url.openConnection();
										//Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
										InputStream in = new BufferedInputStream(urlConnection.getInputStream());
										//readStream(in);
										urlConnection.disconnect();
									}
								} catch (MalformedURLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									err = e.getMessage();
									//							Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									err = e.getMessage();
									//							Toast.makeText(getApplicationContext(), "err: " + err, Toast.LENGTH_LONG).show();
								}
								finally {
									if (urlConnection != null)
										urlConnection.disconnect();
									
									File dir = getFilesDir();
									File file = new File(dir, filenameGPS);
									boolean deleted = file.delete();
									//							Toast.makeText(getApplicationContext(), "File delete: " + (deleted ? "yes" : "no"), Toast.LENGTH_LONG).show();
								}

								saveLat = loc.getLatitude();
								saveLong = loc.getLongitude();
							}
						}
						else
						{
							if (dist >= 0.0002)
							{
								FileOutputStream outputStream;

								try {
									outputStream = openFileOutput(filenameGPS, Context.MODE_APPEND);
									outputStream.write(("^" + params).getBytes());
									outputStream.close();
									//Toast.makeText(getApplicationContext(), "Wrote to file filenameGPS", Toast.LENGTH_LONG).show();
								} catch (Exception e) {
									e.printStackTrace();
									Toast.makeText(getApplicationContext(), "Error to write to file " + e.getMessage(), Toast.LENGTH_LONG).show();
								}

								saveLat = loc.getLatitude();
								saveLong = loc.getLongitude();
							}
						}
					}
				} catch (Exception e) {
					Log.e(tag, e.toString());
				}
				if (false) {
					if (showingDebugToast) Toast.makeText(
							getBaseContext(),
							"Location stored: \nLat: " + sevenSigDigits.format(loc.getLatitude())
							+ " \nLon: " + sevenSigDigits.format(loc.getLongitude())
							+ " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
							+ " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
							Toast.LENGTH_SHORT).show();
				} else if (false){
					if (showingDebugToast) Toast.makeText(
							getBaseContext(),
							"Location not accurate enough: \nLat: " + sevenSigDigits.format(loc.getLatitude())
							+ " \nLon: " + sevenSigDigits.format(loc.getLongitude())
							+ " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
							+ " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		public void onProviderDisabled(String provider) {
			if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderDisabled: " + provider,
					Toast.LENGTH_SHORT).show();

		}

		public void onProviderEnabled(String provider) {
			if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderEnabled: " + provider,
					Toast.LENGTH_SHORT).show();

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			String showStatus = null;
			if (status == LocationProvider.AVAILABLE)
				showStatus = "Available";
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				showStatus = "Temporarily Unavailable";
			if (status == LocationProvider.OUT_OF_SERVICE)
				showStatus = "Out of Service";
			if (status != lastStatus && showingDebugToast) {
				Toast.makeText(getBaseContext(),
						"new status: " + showStatus,
						Toast.LENGTH_SHORT).show();
			}
			lastStatus = status;
		}

	}

	// Below is the service framework methods

	private NotificationManager mNM;

	@Override
	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		startLoggerService();

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		shutdownLoggerService();

                // Cancel the persistent notification.
                mNM.cancel(R.string.local_service_started);

                // Tell the user we stopped.
                Toast.makeText(this, R.string.local_service_stopped,
                                                Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	@SuppressWarnings("deprecation")
	private void showNotification() {
		
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.tracking,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, GPSTracker.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_name),
				text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.local_service_started, notification);
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static void setMinTimeMillis(long _minTimeMillis) {
		minTimeMillis = _minTimeMillis;
	}

	public static long getMinTimeMillis() {
		return minTimeMillis;
	}

	public static void setMinDistanceMeters(long _minDistanceMeters) {
		minDistanceMeters = _minDistanceMeters;
	}

	public static long getMinDistanceMeters() {
		return minDistanceMeters;
	}

	public static float getMinAccuracyMeters() {
		return minAccuracyMeters;
	}

	public static void setMinAccuracyMeters(float minAccuracyMeters) {
		GPSTracker.minAccuracyMeters = minAccuracyMeters;
	}

	public static void setShowingDebugToast(boolean showingDebugToast) {
		GPSTracker.showingDebugToast = showingDebugToast;
	}

	public static boolean isShowingDebugToast() {
		return showingDebugToast;
	}
	
	public static double getLatitude() {
		return saveLat;
	}
	
	public static double getLongitude() {
		return saveLong;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GPSTracker getService() {
			return GPSTracker.this;
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onTaskRemoved(Intent rootIntent)
	{
		//Toast.makeText(getApplicationContext(), "Detected service kill, restarting service!", Toast.LENGTH_LONG).show();
		Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
	    restartServiceIntent.setPackage(getPackageName());

	    PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
	    AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
	    alarmService.set(
	            AlarmManager.ELAPSED_REALTIME,
	            SystemClock.elapsedRealtime() + 1000,
	            restartServicePendingIntent);
	    
	    //Toast.makeText(getApplicationContext(), "Set service restart!", Toast.LENGTH_LONG).show();

	    super.onTaskRemoved(rootIntent);
	}
}
