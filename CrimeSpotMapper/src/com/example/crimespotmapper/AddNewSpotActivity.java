package com.example.crimespotmapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.crimespotmapper.utils.ConstantHelper;
import com.example.crimespotmapper.utils.InternetConnectionCheck;
import com.example.crimespotmapper.utils.JSONParser;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class AddNewSpotActivity extends Activity implements OnClickListener {

	private Spinner crimeSpinner;
	private EditText timeEditText;
	private EditText dateEditText;
	private EditText namEditText;
	private EditText descEditText;

	private AsyncTask<String, String, String> asyncTask;

	private ProgressDialog pDialog;

	private JSONParser jsonParser = new JSONParser();

	private Button addButton;
	private String crimeType;
	private String crimeIcon;

	private String serverResponse = null;

	private DatePickerDialog datePickerDialog;
	private TimePickerDialog timePickerDialog;
	private SimpleDateFormat dateFormatter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_new_spot);

		namEditText = (EditText) findViewById(R.id.et_name);

		addButton = (Button) findViewById(R.id.add_button);

		descEditText = (EditText) findViewById(R.id.et_desc);

		timeEditText = (EditText) findViewById(R.id.et_time);
		timeEditText.setInputType(InputType.TYPE_NULL);

		dateEditText = (EditText) findViewById(R.id.et_date);
		dateEditText.setInputType(InputType.TYPE_NULL);

		crimeSpinner = (Spinner) findViewById(R.id.crime_type_spinner);

		dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

		timeEditText.setOnClickListener(this);
		dateEditText.setOnClickListener(this);
		addButton.setOnClickListener(this);

		List<Crime> crimes = new ArrayList<>();

		String[] crimeTypes = getResources().getStringArray(R.array.crime_type);
		String[] crimeDesc = getResources().getStringArray(R.array.crime_desc);
		final String[] crimeIcons = getResources().getStringArray(
				R.array.crime_ic);

		for (int i = 0; i < crimeDesc.length; i++) {
			Crime crime = new Crime();
			crime.setCrimeType(crimeTypes[i]);
			crime.setCrimeDesc(crimeDesc[i]);
			crime.setImage(crimeIcons[i]);
			crimes.add(crime);
		}

		final Adapter adapter = new Adapter(this, crimes);
		crimeSpinner.setAdapter(adapter);
		crimeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				crimeType = ((Crime) adapter.getItem(position)).getCrimeType();
				crimeIcon = crimeIcons[position];
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	@Override
	public void onClick(View v) {
		if (v == dateEditText) {
			dateEditText.requestFocus();
			Calendar newCalendar = Calendar.getInstance();
			datePickerDialog = new DatePickerDialog(this,
					new OnDateSetListener() {

						public void onDateSet(DatePicker view, int year,
								int monthOfYear, int dayOfMonth) {
							Calendar newDate = Calendar.getInstance();
							newDate.set(year, monthOfYear, dayOfMonth);
							dateEditText.setText(dateFormatter.format(newDate
									.getTime()));
						}

					}, newCalendar.get(Calendar.YEAR),
					newCalendar.get(Calendar.MONTH),
					newCalendar.get(Calendar.DAY_OF_MONTH));
			datePickerDialog.show();
		} else if (v == timeEditText) {
			timeEditText.requestFocus();
			Calendar newCalendar = Calendar.getInstance();
			timePickerDialog = new TimePickerDialog(this,
					new OnTimeSetListener() {

						@Override
						public void onTimeSet(TimePicker view, int hourOfDay,
								int minute) {
							timeEditText.setText(paddingString(hourOfDay) + ":"
									+ paddingString(minute));
						}
					}, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);
			timePickerDialog.show();
		} else if (v.getId() == R.id.add_button) {
			runAddNewCrimeSpot();
		}
	}
	
	private void runAddNewCrimeSpot() {
		if (InternetConnectionCheck.haveNetworkConnection(this)) {
			if (asyncTask == null) {
				// --- create a new task --
				asyncTask = new AddNewCrimeSpot();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.FINISHED) {
				asyncTask = new AddNewCrimeSpot();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
				asyncTask.cancel(false);
				asyncTask = new AddNewCrimeSpot();
				asyncTask.execute();
			}
		} else {
			Toast.makeText(this, "Internet connection not available",
					Toast.LENGTH_SHORT).show();
		}
	}

	private static String paddingString(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

	private class Adapter extends BaseAdapter {
		private Context context;
		private List<Crime> crimes;

		public Adapter(Context context, List<Crime> crimes) {
			this.context = context;
			this.crimes = crimes;
		}

		@Override
		public int getCount() {
			return crimes.size();
		}

		@Override
		public Object getItem(int position) {
			return crimes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView crimeType;
			TextView crimeDesc;
			ImageView crimeIcon;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.crime_type_item, null);

				crimeType = (TextView) convertView
						.findViewById(R.id.crime_type);
				crimeDesc = (TextView) convertView
						.findViewById(R.id.crime_desc);
				crimeIcon = (ImageView) convertView
						.findViewById(R.id.crime_icon);
			} else {
				crimeType = (TextView) convertView
						.findViewById(R.id.crime_type);
				crimeDesc = (TextView) convertView
						.findViewById(R.id.crime_desc);
				crimeIcon = (ImageView) convertView
						.findViewById(R.id.crime_icon);
			}
			crimeType.setText(crimes.get(position).getCrimeType());
			crimeDesc.setText(crimes.get(position).getCrimeDesc());

			int resourceID = context.getResources().getIdentifier(
					crimes.get(position).getImage(), "drawable",
					context.getPackageName());
			crimeIcon.setImageResource(resourceID);
			return convertView;
		}

	}

	private class Crime {
		private String crimeType;
		private String crimeDesc;
		private String image;

		public String getCrimeType() {
			return crimeType;
		}

		public void setCrimeType(String crimeType) {
			this.crimeType = crimeType;
		}

		public String getCrimeDesc() {
			return crimeDesc;
		}

		public void setCrimeDesc(String crimeDesc) {
			this.crimeDesc = crimeDesc;
		}

		public String getImage() {
			return image;
		}

		public void setImage(String image) {
			this.image = image;
		}
	}

	class AddNewCrimeSpot extends AsyncTask<String, String, String> {

		boolean flag = false;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(AddNewSpotActivity.this);
			pDialog.setMessage("Adding Information..");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		protected String doInBackground(String... args) {
			String name = namEditText.getText().toString();
			String description = descEditText.getText().toString();
			String time = timeEditText.getText().toString();
			String date = dateEditText.getText().toString();
			double latitude = getIntent().getDoubleExtra("lat", 0);
			double longitude = getIntent().getDoubleExtra("lng", 0);
			String locality = "";

			Geocoder gc = new Geocoder(AddNewSpotActivity.this);
			try {
				List<Address> addresses = gc.getFromLocation(latitude,
						longitude, 1);
				Address address = addresses.get(0);
				locality = address.getLocality();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (name.length() == 0 || description.length() == 0
					|| time.length() == 0 || date.length() == 0
					|| locality.length() == 0) {
				publishProgress();
				return null;
			}

			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("latitude", String
					.valueOf(latitude)));
			params.add(new BasicNameValuePair("longitude", String
					.valueOf(longitude)));
			params.add(new BasicNameValuePair("name", name));
			params.add(new BasicNameValuePair("type", crimeType));
			params.add(new BasicNameValuePair("description", description));
			params.add(new BasicNameValuePair("time", time));
			params.add(new BasicNameValuePair("date", date));
			params.add(new BasicNameValuePair("icon", crimeIcon));
			params.add(new BasicNameValuePair("locality", locality));

			// getting JSON Object
			// Note that create product url accepts POST method
			JSONObject json = jsonParser.makeHttpRequest(
					ConstantHelper.url_add_new_crime_spot, "POST", params);
			// check for success tag
			try {
				int success = json.getInt(ConstantHelper.TAG_SUCCESS);
				serverResponse = json.getString("message");
				if (success == 1) {
					flag = true;
				} else {
					
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			Toast.makeText(AddNewSpotActivity.this,
					"Required field(s) is missing", Toast.LENGTH_SHORT)
					.show();
		}
		
		protected void onPostExecute(String string) {
			pDialog.dismiss();
			if (flag) {
				Toast.makeText(AddNewSpotActivity.this, serverResponse,
						Toast.LENGTH_SHORT).show();
			}
		}

	}

}
