package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class BackgroundActivity extends Service {

	// GPSTracker class
	AndroidGPSTrackingActivity androidGPS;

	private Timer myTimer;
	private double saveLong = 0;
	private double saveLat = 0;
	
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/** Called when the activity is first created. */
	public void onCreate() {
		Toast.makeText(getApplicationContext(), "Created service", Toast.LENGTH_LONG).show();
	}

	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();

		androidGPS = new AndroidGPSTrackingActivity();
	}
}