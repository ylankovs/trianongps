package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	Button buttonStart;
	private static String emp = "";
	private static String pass = "";
	
	private static boolean logged = false;

	private static String user = "";
	private static String password = "";

	private static String filenameConf = "config";

	private HttpURLConnection urlConnection = null;
	private URL url = null;

	public static String getEmpId()
	{
		return emp;
	}

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(this);

		if (performLoginConfig())
		{
			startService(new Intent(MainActivity.this,GPSTracker.class));	

			Intent act2 = new Intent(buttonStart.getContext(), AndroidGPSTrackingActivity.class);
			act2.putExtra("emp", emp);
			startActivity(act2);
		}
	}

	private boolean performLoginConfig()
	{
		// read config file, get user information
		boolean ret = false;

		try{
			InputStream is = openFileInput(filenameConf);
			if ( is != null ) {
				InputStreamReader inputStreamReader = new InputStreamReader(is);
				BufferedReader reader = new BufferedReader(inputStreamReader);
				String line = null;
				while ((line = reader.readLine()) != null) {
					String delims = "[\\=]";
					String[] split = line.split(delims);

					if (split[0].equals("user"))
						user = split[1];
					else if (split[0].equals("password"))
						password = split[1];
				}
				reader.close();
			}
			is.close();
		} catch(OutOfMemoryError om){
			om.printStackTrace();
			//Toast.makeText(getApplicationContext(), "Out of memory to read file", Toast.LENGTH_LONG).show();
		} catch(Exception ex){
			ex.printStackTrace();
			//Toast.makeText(getApplicationContext(), "Cannot read file " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}

		try {
			if (user != null && user != "" && password != null && password != "")
			{
				url = new URL("http://91.217.202.15:8080/tracking/db_check.php?user=" + user + "&password=" + password);
				urlConnection = (HttpURLConnection) url.openConnection();
				//Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				//readStream(in);
				urlConnection.disconnect();
				
				if (urlConnection.getResponseMessage().toString().equals("OK"))
					ret = true;
			}
		}
		catch (Exception ex) {
			Toast.makeText(getApplicationContext(), "Login exception: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return ret;
	}

	private boolean performLoginEmp(String e, String p)
	{
		// perform login from server and write to config on success
		boolean ret = false;

		try {
			if (e != null && e != "" && p != null && p != "")
			{
				url = new URL("http://91.217.202.15:8080/tracking/db_check.php?user=" + e + "&password=" + p);
				urlConnection = (HttpURLConnection) url.openConnection();
				//Toast.makeText(getApplicationContext(), "Server message: " + urlConnection.getResponseMessage(), Toast.LENGTH_LONG).show();
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				//readStream(in);
				urlConnection.disconnect();
				
				if (urlConnection.getResponseMessage().toString().equals("OK"))
				{
					ret = true;
					FileOutputStream outputStream;

					try {
						outputStream = openFileOutput(filenameConf, Context.MODE_PRIVATE);
						outputStream.write(("user=" + e).getBytes());
						outputStream.write(("\n").getBytes());
						outputStream.write(("password=" + p).getBytes());
						outputStream.close();
						//Toast.makeText(getApplicationContext(), "Wrote to file filenameConf", Toast.LENGTH_LONG).show();
					} catch (Exception ex) {
						Toast.makeText(getApplicationContext(), "Error to write to file " + ex.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			}
		}
		catch (Exception ex) {
			Toast.makeText(getApplicationContext(), "Login exception: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return ret;
	}

	@Override
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonStart:
			EditText user = (EditText)findViewById(R.id.userId);
			emp = user.getText().toString();
			EditText pwd = (EditText)findViewById(R.id.userPwd);
			pass = pwd.getText().toString();
			if (emp != null || emp != "")
			{
				try {
					logged = performLoginEmp(emp, pass);
				}
				catch(Exception ex) {
					ex.printStackTrace();
					Toast.makeText(getApplicationContext(), "Error, cannot login " + ex.getMessage(), Toast.LENGTH_LONG).show();
				}
				
//				Toast.makeText(getApplicationContext(), "Login status: " + logged, Toast.LENGTH_LONG).show();

				if (logged)
				{
					try{
						startService(new Intent(MainActivity.this,GPSTracker.class));	

						Intent act2 = new Intent(src.getContext(), AndroidGPSTrackingActivity.class);
						act2.putExtra("emp", emp);
						startActivity(act2);			
						break;
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
						Toast.makeText(getApplicationContext(), "Error in the program " + ex.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			}
		}
	}
}
