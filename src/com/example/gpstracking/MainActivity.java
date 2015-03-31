package com.example.gpstracking;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	Button buttonStart;
	private static String emp = "";
	private static boolean logged = false;

	private static String configFile = "config";

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

		return ret;
	}

	private boolean performLoginEmp(String e)
	{
		// perform login from server and write to config on success
		boolean ret = false;

		return ret;
	}

	@Override
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonStart:
			EditText text = (EditText)findViewById(R.id.userId);
			emp = text.getText().toString();
			if (emp != null || emp != "")
			{
				try {
					logged = performLoginEmp(emp);
				}
				catch(Exception ex) {
					ex.printStackTrace();
					Toast.makeText(getApplicationContext(), "Error, cannot login " + ex.getMessage(), Toast.LENGTH_LONG).show();
				}

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
