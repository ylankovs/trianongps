package com.example.gpstracking;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	Button buttonStart;
	private String emp = "";
	
	public void onCreate(Bundle icicle) {
		Toast.makeText(getApplicationContext(), "Created service", Toast.LENGTH_LONG).show();
		super.onCreate(icicle);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStart.setOnClickListener(this);
	}

	@Override
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonStart:
			EditText text = (EditText)findViewById(R.id.userId);
			emp = text.getText().toString();
			if (emp != null || emp != "")
			{
				Intent act2 = new Intent(src.getContext(), AndroidGPSTrackingActivity.class);
				act2.putExtra("emp", emp);
				startActivity(act2);
				
				break;
			}
		}
	}
}
