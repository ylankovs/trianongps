package com.example.gpstracking;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gpstracking.GPSTracker;

@SuppressLint("SimpleDateFormat") public class AndroidGPSTrackingActivity extends Activity implements OnClickListener {

	//The "x" and "y" position of the "Show Button" on screen.
	Point p;

	Button buttonLoad;
	Button buttonSend;
	Button buttonMinimize;

	TextView text;

	private static int a[] = {20, 59, 71, 59, 87, 223, 16, 117, 20, 181, 103, 23, 131, 188, 184,
		19, 230, 44, 250, 109, 134, 241, 34, 234, 165, 92, 125, 30, 212,
		186, 215, 153, 3, 177, 234, 6, 91, 124, 91};

	private Spinner spinner1, spinner2;
	private ArrayAdapter<String> spinnerAdapter1;
	private ArrayAdapter<String> spinnerAdapter2;

	private String mess = "";

	private static int NUM_ADDRS_PARAMS = 4;
	private static int NUM_MSG_PARAMS = 5;
	private static double CLIENT_RANGE_KMS = 0.036; // 2 km

	private static String filenameMSG = "messages";
	private static String filenameADRS = "addresses";

	private boolean clientWithinRange(double lattClnt, double latt, double lngtClnt, double lngt)
	{
		boolean ret = false;
		if ((lattClnt + CLIENT_RANGE_KMS >= latt && lattClnt - CLIENT_RANGE_KMS <= latt) && 
				(lngtClnt + CLIENT_RANGE_KMS >= lngt && lngtClnt - CLIENT_RANGE_KMS <= lngt))
		{
			ret = true;
		}
		return ret;
	}

	public boolean isNumeric(String s) {  
		return s.matches("[-+]?\\d*\\.?\\d+");  
	}

	private ArrayList<String> decodeString(String s)
	{
		// return: array of decoded strings, delimiter is 'g0'
		ArrayList<String> ret = new ArrayList<String>(); 
		String sym = null;
		String t = "";

		try {
			int kkm= a.length;
			int len= s.length();
			int i= 0;
			int iw= 0;
			while( i < (len-2) ) {
				sym = s.substring(i,i+1);
				if( sym == "g0" ) {
					ret.add("");
					iw= 0;
				}
				else if( isNumeric(sym) ) {
					ret.add(new Character((char)Integer.parseInt(
							Integer.toBinaryString(Integer.parseInt(sym) ^ a[iw++ % kkm]).replace(' ', '0'), 2)).toString());
					t += Integer.parseInt(Integer.toBinaryString(Integer.parseInt(sym) ^ a[iw++ % kkm]).replace(' ', '0'), 2) + "\n";
				}
				i+= 2;
			}
		}
		catch (Exception ex) {
			Toast.makeText(getApplicationContext(), "Error in decode sym=" + sym + " " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		Toast.makeText(getApplicationContext(), "Test " + t, Toast.LENGTH_LONG).show();
		Toast.makeText(getApplicationContext(), "Returning " + ret.toString(), Toast.LENGTH_LONG).show();
		return ret;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Toast.makeText(getApplicationContext(), "Opened activity", Toast.LENGTH_LONG).show();
		super.onCreate(icicle);
		setContentView(R.layout.gps);

		buttonLoad = (Button) findViewById(R.id.button1);
		buttonLoad.setOnClickListener(this);

		buttonSend = (Button) findViewById(R.id.button2);
		buttonSend.setOnClickListener(this);

		buttonMinimize = (Button) findViewById(R.id.button3);
		buttonMinimize.setOnClickListener(this);

		text = (TextView) findViewById(R.id.editText1);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		mess = extras.getString("emp");


		spinner1 = (Spinner)findViewById(R.id.spinner1);
		spinnerAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
		spinnerAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(spinnerAdapter1);
		spinnerAdapter1.add("клиент");
		spinnerAdapter1.notifyDataSetChanged();

		spinner2 = (Spinner)findViewById(R.id.spinner2);
		spinnerAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
		spinnerAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(spinnerAdapter2);
		spinnerAdapter2.add("платеж");
		spinnerAdapter2.add("заказ");
		spinnerAdapter2.add("инвентаризация");
		spinnerAdapter2.add("прочее");
		spinnerAdapter2.notifyDataSetChanged();
	}

	private String getClientId(String str)
	{
		String ret = "";

		return ret;
	}

	private String getTyp(String str)
	{
		String ret = "";

		return ret;
	}

	@Override
	public void onClick(View src) {
		String err = null;
		String sendingParams = null;
		StringBuilder sb = null;

		String time = new SimpleDateFormat("yyMMddkkmmss").format(new Date());

		switch (src.getId()) {
		case R.id.button3: // update spinner
			// load list of clients from file
			try{
				sb = new StringBuilder("");
				InputStream is = openFileInput(filenameADRS);
				if ( is != null ) {
					InputStreamReader inputStreamReader = new InputStreamReader(is);
					BufferedReader reader = new BufferedReader(inputStreamReader);
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
				}
				is.close();
				Toast.makeText(getApplicationContext(), "Read from file filenameADRS: " + sb, Toast.LENGTH_LONG).show();
			} catch(OutOfMemoryError om){
				om.printStackTrace();
				Toast.makeText(getApplicationContext(), "Out of memory to read file filenameADRS", Toast.LENGTH_LONG).show();
			} catch(Exception ex){
				ex.printStackTrace();
				Toast.makeText(getApplicationContext(), "Error in the program filenameADRS " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}

			if(sb != null && !sb.toString().equals(""))
			{
				String delims = "[\\^]";
				String[] split = (sb.toString()).split(delims);
				int nCnt = split.length/NUM_ADDRS_PARAMS;

				spinner1.setAdapter(spinnerAdapter1);

				int offsetAdr = 1;
				int offsetLatt = 2;
				int offsetLngt = 3;

				int cntUsed = 0;
				for (int i=0; i<(nCnt*NUM_ADDRS_PARAMS); i+=NUM_ADDRS_PARAMS)
				{
					if (clientWithinRange(
							Double.parseDouble(split[i + offsetLatt]), GPSTracker.getLatitude(),
							Double.parseDouble(split[i + offsetLngt]), GPSTracker.getLongitude()))
					{
						spinnerAdapter1.add(split[i + offsetAdr]);
						cntUsed ++;
					}
				}
				spinnerAdapter1.notifyDataSetChanged();

				Toast.makeText(getApplicationContext(), "Spinner updated " + cntUsed + "/" + nCnt, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.button2: // buttonLoad
			try {
				DefaultHttpClient httpclient = new DefaultHttpClient();

				HttpGet httppost = new HttpGet("http://91.217.202.15:8080/tracking/track_fetch_addrs.php?idAg=" + mess);
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

				/*
				byte[] encoded = Base64.encode(total.toString().getBytes("CP1252"), Base64.DEFAULT);
				String str = new String(encoded, "CP1252");
				 */

				/*
				ArrayList<String> t = decodeString(total.toString());

				StringBuffer result = new StringBuffer();
				for (int i = 0; i < t.size(); i++) {
					result.append( t.get(i) );
				}
				String mynewstring = result.toString();
				 */

				//				byte bytes[] = total.toString().getBytes("Cp1251"); 
				//				String mynewstring = new String(bytes, "UTF-8"); 

				String mynewstring = total.toString();

				Toast.makeText(getApplicationContext(), "Read from server:\n" + mynewstring, Toast.LENGTH_LONG).show();

				//				Toast.makeText(getApplicationContext(), "Trying file writes", Toast.LENGTH_LONG).show();
				FileOutputStream outputStream;

				try {
					outputStream = openFileOutput(filenameADRS, Context.MODE_PRIVATE);
					outputStream.write(mynewstring.getBytes());
					outputStream.close();
					//					Toast.makeText(getApplicationContext(), "Wrote to file", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Error in the program." + e.getMessage(), Toast.LENGTH_LONG).show();
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
			break;
		case R.id.button1: // buttonSave
			sendingParams = "";
			sb = new StringBuilder("");

			String textInput = text.getText().toString();
			String clientSelect = spinner1.getSelectedItem().toString();
			String typSelect = spinner2.getSelectedItem().toString();

			//			sData= 'dstamp^div^idOrg^typ^msg^
			String clntId = getClientId(clientSelect);
			String typ = getTyp(typSelect);

			sendingParams += time + "^" + "0" + "^" + clientSelect + "^" + typSelect + "^" + textInput + "^";

			try{
				InputStream is = openFileInput(filenameMSG);
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
				Toast.makeText(getApplicationContext(), "Out of memory to read file filenameMSG", Toast.LENGTH_LONG).show();
			} catch(Exception ex){
				ex.printStackTrace();
				Toast.makeText(getApplicationContext(), "Error in the program filenameMSG " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}

			if(sb != null && !sb.toString().equals(""))
			{
				sb.deleteCharAt(0);
				sb.append("^" + sendingParams);
			}
			else
				sb.append(sendingParams);

			//			Toast.makeText(getApplicationContext(), "Full list: " + sb, Toast.LENGTH_LONG).show();

			HttpURLConnection urlConnection = null;
			URL url = null;
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			State mobile = connManager.getNetworkInfo(0).getState();
			State wifi = connManager.getNetworkInfo(1).getState();

			if (mobile == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTED)
			{
				try {
					String delims = "[\\^]";
					String[] split = (sb.toString()).split(delims);
					int nTx = split.length / NUM_MSG_PARAMS;

					for(int j=0; j<nTx; j+=NUM_MSG_PARAMS)
					{
						String temp = "";
						temp += split[j] + "^" + split[j+1] + "^" + split[j+2] + "^" + split[j+3] + "^" + split[j+4] + "^";

						Toast.makeText(getApplicationContext(), "Sending request: http://91.217.202.15:8080/tracking/track_msg_sav.php?sData"
								+ temp, Toast.LENGTH_LONG).show();

						url = new URL("http://91.217.202.15:8080/tracking/track_msg_sav.php?sData=" + temp);
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
					//				Toast.makeText(getApplicationContext(), "Read from file filenameMSG: " + sb + " lines= " + numLines, Toast.LENGTH_LONG).show();

					File dir = getFilesDir();
					File file = new File(dir, filenameMSG);
					boolean deleted = file.delete();
					//				Toast.makeText(getApplicationContext(), "File delete filenameMSG: " + (deleted ? "yes" : "no"), Toast.LENGTH_LONG).show();
				}
			}
			else
			{
				//				Toast.makeText(getApplicationContext(), "Trying file writes filenameMSG", Toast.LENGTH_LONG).show();
				FileOutputStream outputStream;

				try {
					outputStream = openFileOutput(filenameMSG, Context.MODE_APPEND);
					outputStream.write(("^" + sendingParams).getBytes());
					outputStream.close();
					//					Toast.makeText(getApplicationContext(), "Wrote to file filenameMSG", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Error in the program filenameMSG." + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
			break;
		}
	}
}