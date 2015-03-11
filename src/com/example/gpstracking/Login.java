
package com.example.gpstracking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class Login extends Activity implements OnClickListener {
  Button buttonStart, buttonStop;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    buttonStart = (Button) findViewById(R.id.buttonStart);
//    buttonStop = (Button) findViewById(R.id.buttonStop);

    buttonStart.setOnClickListener(this);
//    buttonStop.setOnClickListener(this);
  }

  public void onClick(View src) {
    switch (src.getId()) {
    case R.id.buttonStart:
      Toast.makeText(getApplicationContext(), "My Service starting", Toast.LENGTH_LONG).show();
      startService(new Intent(this, AndroidGPSTrackingActivity.class));
      break;
//    case R.id.buttonStop:
//      stopService(new Intent(this, MyService.class));
//      break;
    }
  }

}
