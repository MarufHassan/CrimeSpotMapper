package com.example.crimespotmapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crimespotmapper.utils.ConstantHelper;
import com.example.crimespotmapper.utils.InternetConnectionCheck;
import com.example.crimespotmapper.utils.JSONParser;

public class CrimeSpotDetailsActivity extends Activity {

	public static final String TAG_SUCCESS = "success";

	private ProgressDialog pDialog;

	private JSONParser jsonParser;

	private AsyncTask<String, String, String> asyncTask;

	private TextView tvCrimeType;
	private TextView tvCrimeVictimName;
	private TextView tvCrimeDescription;
	private TextView tvCrimeDate;
	private TextView tvCrimeTime;
	private ImageView tvCrimeImage;
	private Button nearestPoliceContact;

	private double latitude;
	private double longitude;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crime_spot_details);

		tvCrimeType = (TextView) findViewById(R.id.crime_type);
		tvCrimeVictimName = (TextView) findViewById(R.id.victim_name);
		tvCrimeDescription = (TextView) findViewById(R.id.crime_desc);
		tvCrimeDate = (TextView) findViewById(R.id.tv_date);
		tvCrimeTime = (TextView) findViewById(R.id.tv_time);
		tvCrimeImage = (ImageView) findViewById(R.id.crime_icon);
		nearestPoliceContact = (Button) findViewById(R.id.nearestPoliceContact);
		nearestPoliceContact.setVisibility(View.INVISIBLE);

		latitude = getIntent().getDoubleExtra("lat", 0);
		longitude = getIntent().getDoubleExtra("lng", 0);
		
		nearestPoliceContact.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CrimeSpotDetailsActivity.this, PoliceContactActivity.class);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
				startActivity(intent);
			}
		});

		runLoadCrimeSpotDetails();

	}
	
	private void runLoadCrimeSpotDetails() {
		if (InternetConnectionCheck.haveNetworkConnection(this)) {
			if (asyncTask == null) {
				// --- create a new task --
				asyncTask = new LoadCrimeSpotDetails();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.FINISHED) {
				asyncTask = new LoadCrimeSpotDetails();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
				asyncTask.cancel(false);
				asyncTask = new LoadCrimeSpotDetails();
				asyncTask.execute();
			}
		} else {
			Toast.makeText(this, "Internet connection not available. Can't load..",
					Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	class LoadCrimeSpotDetails extends AsyncTask<String, String, String> {
		private JSONObject spot;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(CrimeSpotDetailsActivity.this);
			pDialog.setMessage("Loading Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					CrimeSpotDetailsActivity.this.finish();
					Toast.makeText(CrimeSpotDetailsActivity.this, "Cancelled",
							Toast.LENGTH_SHORT).show();
				}
			});
			pDialog.show();
		}

		protected String doInBackground(String... args) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("latitude", String
					.valueOf(latitude)));
			params.add(new BasicNameValuePair("longitude", String
					.valueOf(longitude)));
			// getting JSON string from URL
			jsonParser = new JSONParser();
			JSONObject json = jsonParser.makeHttpRequest(
					ConstantHelper.url_crime_spots_details, "GET", params);
			try {
				// Checking for SUCCESS TAG
				int success = json.getInt(TAG_SUCCESS);
				if (success == 1) {
					JSONArray crimeSpotObj = json.getJSONArray("spots"); // JSON
																			// Array

					spot = crimeSpotObj.getJSONObject(0);
				} else {

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		protected void onPostExecute(String file_url) {
			pDialog.dismiss();

			try {
				nearestPoliceContact.setVisibility(View.VISIBLE);
				tvCrimeType.setText(spot.getString("type"));
				tvCrimeVictimName.setText(spot.getString("name"));
				tvCrimeDate.setText(spot.getString("date"));
				tvCrimeTime.setText(spot.getString("time"));
				tvCrimeDescription.setText(spot.getString("description"));

				int iconId = getResources().getIdentifier(
						spot.getString("icon"), "drawable", getPackageName());
				tvCrimeImage.setImageResource(iconId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
			asyncTask.cancel(true);
		}
	}

}
