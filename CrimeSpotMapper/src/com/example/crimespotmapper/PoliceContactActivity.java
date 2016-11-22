package com.example.crimespotmapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.crimespotmapper.utils.ConstantHelper;
import com.example.crimespotmapper.utils.InternetConnectionCheck;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class PoliceContactActivity extends FragmentActivity implements
		OnClickListener, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	private GoogleMap mMap;
	private Marker mMarker;

	private Location foundLocation;
	
	private double latitude = ConstantHelper.CHITTAGONG_LAT;
	private double longitude = ConstantHelper.CHITTAGONG_LNG;

	private GoogleApiClient apiClient;

	private Button findButton;
	private Button myLocationButton;
	private Button refreshButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!InternetConnectionCheck.haveNetworkConnection(this)) {
			Toast.makeText(this, "Internet connection not available",
					Toast.LENGTH_SHORT).show();
		}
		if (servicesOK()) {
			setContentView(R.layout.activity_police_contact);

			findButton = (Button) findViewById(R.id.findButton);
			myLocationButton = (Button) findViewById(R.id.myLocationButton);
			refreshButton = (Button) findViewById(R.id.refreshButton);

			findButton.setOnClickListener(this);
			myLocationButton.setOnClickListener(this);
			refreshButton.setOnClickListener(this);

			if (initMap()) {
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
						new LatLng(ConstantHelper.CHITTAGONG_LAT,
								ConstantHelper.CHITTAGONG_LNG), 12);
				mMap.moveCamera(update);
				apiClient = new GoogleApiClient.Builder(this)
						.addApi(LocationServices.API)
						.addConnectionCallbacks(this)
						.addOnConnectionFailedListener(this).build();
				apiClient.connect();

				setMarkers("Chittagong", "Bangladesh",
						ConstantHelper.CHITTAGONG_LAT,
						ConstantHelper.CHITTAGONG_LNG,
						R.drawable.find_marker);
				nearestPoliceStation(ConstantHelper.CHITTAGONG_LAT,
						ConstantHelper.CHITTAGONG_LNG);
				
				Intent intent = getIntent();
				if (intent != null) {
					double latitude = intent.getDoubleExtra("latitude", 0);
					double longitude = intent.getDoubleExtra("longitude", 0);
					if (latitude != 0 && longitude != 0) {
						setMarkers("", "", latitude, longitude,
								R.drawable.find_marker);
						nearestPoliceStation(latitude, longitude);
					}
				}

			} else {
				Toast.makeText(this, "Map not available!", Toast.LENGTH_SHORT)
						.show();
			}
		} else {
			Toast.makeText(this, "Google Play Services is not available",
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private boolean initMap() {
		if (mMap == null) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			mMap = mapFrag.getMap();

			if (mMap != null) {

				mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
					@Override
					public boolean onMarkerClick(Marker marker) {
						if (mMarker != null) {
							String str_origin = "origin="
									+ mMarker.getPosition().latitude + ","
									+ mMarker.getPosition().longitude;
							// Destination of route
							String str_dest = "destination="
									+ marker.getPosition().latitude + ","
									+ marker.getPosition().longitude;
							// Sensor enabled
							String sensor = "sensor=false";
							// Building the parameters to the web service
							String parameters = str_origin + "&" + str_dest
									+ "&" + sensor;
							// Output format
							String output = "json";
							// Building the url to the web service
							String url = "https://maps.googleapis.com/maps/api/directions/"
									+ output + "?" + parameters;
							DownloadTask downloadTask = new DownloadTask();
							// Start downloading json data from Google
							// Directions
							// API
							Object[] toPass = new Object[2];
							toPass[0] = mMap;
							toPass[1] = url;
							downloadTask.execute(toPass);
						}

						return false;
					}
				});

				mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

					@Override
					public void onMapLongClick(LatLng ll) {
						mMap.clear();
						setMarkers("", "", ll.latitude, ll.longitude,
								R.drawable.find_marker);
						nearestPoliceStation(ll.latitude, ll.longitude);
					}
				});
			}
		}
		return (mMap != null);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.findButton) {
			LayoutInflater inflater = LayoutInflater
					.from(PoliceContactActivity.this);
			View view = inflater.inflate(R.layout.search_dialog, null);

			final EditText searchET = (EditText) view
					.findViewById(R.id.etSearch);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Find Location");
			builder.setView(view);

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mMap.clear();
							if (!InternetConnectionCheck
									.haveNetworkConnection(PoliceContactActivity.this)) {
								Toast.makeText(PoliceContactActivity.this,
										"Internet connection not available",
										Toast.LENGTH_SHORT).show();
								return;
							}
							String location = searchET.getText().toString();

							Geocoder gc = new Geocoder(
									PoliceContactActivity.this);
							List<Address> addresses = null;
							try {
								addresses = gc.getFromLocationName(location, 1);
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
							Address address;
							try {
								address = addresses.get(0);
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(PoliceContactActivity.this,
										"Location not found",
										Toast.LENGTH_SHORT).show();
								return;
							}
							latitude = address.getLatitude();
							longitude = address.getLongitude();
							gotoLocation(address.getLatitude(),
									address.getLongitude(),
									ConstantHelper.DEFAULTZOOM);
							setMarkers(address.getLocality(),
									address.getCountryName(),
									address.getLatitude(),
									address.getLongitude(),
									R.drawable.find_marker);
							nearestPoliceStation(address.getLatitude(),
									address.getLongitude());
						}
					});
			builder.setNeutralButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			AlertDialog simpleDialog = builder.create();
			simpleDialog.show();
		} else if (v.getId() == R.id.myLocationButton) {
			foundLocation = LocationServices.FusedLocationApi
					.getLastLocation(apiClient);
			if (foundLocation == null) {
				Toast.makeText(this, "Current location isn't available",
						Toast.LENGTH_SHORT).show();
			} else {
				mMap.clear();
				latitude = foundLocation.getLatitude();
				longitude = foundLocation.getLongitude();
				gotoLocation(foundLocation.getLatitude(),
						foundLocation.getLongitude(), 16);
				setMarkers("Current Location", "", foundLocation.getLatitude(),
						foundLocation.getLongitude(), R.drawable.i_am_here);
				nearestPoliceStation(foundLocation.getLatitude(),
						foundLocation.getLongitude());
			}
		} else if (v.getId() == R.id.refreshButton) {
			gotoLocation(latitude, longitude, ConstantHelper.DEFAULTZOOM);
			setMarkers("", "", latitude, longitude, R.drawable.find_marker);
			nearestPoliceStation(latitude, longitude);
		}
	}

	public boolean servicesOK() {
		int isAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
					this, ConstantHelper.GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		} else {
			Toast.makeText(this, "Can't connect to Google Play services",
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	private void gotoLocation(double lat, double lng, float zoom) {
		LatLng ll = new LatLng(lat, lng);
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
		mMap.animateCamera(update);
	}

	public void geoLocate(View v) throws IOException {

	}

	private void setMarkers(String locality, String countryName, double lat,
			double lng, int markerIconRes) {

		if (mMarker != null) {
			mMarker.remove();
		}
		MarkerOptions options = new MarkerOptions().title(locality)
				.position(new LatLng(lat, lng))
				.icon(BitmapDescriptorFactory.fromResource(markerIconRes));

		if (markerIconRes == 0) {
			options.icon(BitmapDescriptorFactory.defaultMarker());
		}

		if (countryName.length() > 0) {
			options.snippet(countryName);
		}

		mMarker = mMap.addMarker(options);
	}

	private void nearestPoliceStation(double lat, double lng) {
		StringBuilder googlePlacesUrl = new StringBuilder(
				"https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
		googlePlacesUrl.append("location=" + lat + "," + lng);
		googlePlacesUrl.append("&radius=" + ConstantHelper.PROXIMITY_RADIUS);
		googlePlacesUrl.append("&types=" + "police");
		googlePlacesUrl.append("&sensor=true");
		googlePlacesUrl.append("&key=" + ConstantHelper.GOOGLE_API_KEY);

		GooglePlacesReadTask googlePlacesReadTask = new GooglePlacesReadTask();
		Object[] toPass = new Object[3];
		toPass[0] = mMap;
		toPass[1] = googlePlacesUrl.toString();
		toPass[2] = new LatLng(lat, lng);
		googlePlacesReadTask.execute(toPass);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onConnected(Bundle arg0) {
	}

	@Override
	public void onConnectionSuspended(int arg0) {
	}

	class DirectionsJSONParser {

		/**
		 * Receives a JSONObject and returns a list of lists containing latitude
		 * and longitude
		 */
		public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

			List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
			JSONArray jRoutes = null;
			JSONArray jLegs = null;
			JSONArray jSteps = null;

			try {

				jRoutes = jObject.getJSONArray("routes");

				/** Traversing all routes */
				for (int i = 0; i < jRoutes.length(); i++) {
					jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
					List path = new ArrayList<HashMap<String, String>>();

					/** Traversing all legs */
					for (int j = 0; j < jLegs.length(); j++) {
						jSteps = ((JSONObject) jLegs.get(j))
								.getJSONArray("steps");

						/** Traversing all steps */
						for (int k = 0; k < jSteps.length(); k++) {
							String polyline = "";
							polyline = (String) ((JSONObject) ((JSONObject) jSteps
									.get(k)).get("polyline")).get("points");
							List<LatLng> list = decodePoly(polyline);

							/** Traversing all points */
							for (int l = 0; l < list.size(); l++) {
								HashMap<String, String> hm = new HashMap<String, String>();
								hm.put("lat", Double.toString(((LatLng) list
										.get(l)).latitude));
								hm.put("lng", Double.toString(((LatLng) list
										.get(l)).longitude));
								path.add(hm);
							}
						}
						routes.add(path);
					}
				}

			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
			}

			return routes;
		}

		/**
		 * Method to decode polyline points Courtesy :
		 * http://jeffreysambells.com
		 * /2010/05/27/decoding-polylines-from-google-maps
		 * -direction-api-with-java
		 * */
		private List<LatLng> decodePoly(String encoded) {

			List<LatLng> poly = new ArrayList<LatLng>();
			int index = 0, len = encoded.length();
			int lat = 0, lng = 0;

			while (index < len) {
				int b, shift = 0, result = 0;
				do {
					b = encoded.charAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
				lat += dlat;

				shift = 0;
				result = 0;
				do {
					b = encoded.charAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
				lng += dlng;

				LatLng p = new LatLng((((double) lat / 1E5)),
						(((double) lng / 1E5)));
				poly.add(p);
			}

			return poly;
		}
	}

	class DownloadTask extends AsyncTask<Object, Void, String> {
		GoogleMap googleMap;

		// Downloading data in non-ui thread
		@Override
		protected String doInBackground(Object... objects) {

			// For storing data from web service
			String data = "";

			try {
				// Fetching the data from web service
				data = downloadUrl((String) objects[1]);
				googleMap = (GoogleMap) objects[0];
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		// Executes in UI thread, after the execution of
		// doInBackground()
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			ParserTask parserTask = new ParserTask();
			Object[] toPass = new Object[3];
			toPass[0] = googleMap;
			toPass[1] = result;
			// Invokes the thread for parsing the JSON data
			parserTask.execute(toPass);
		}

		private String downloadUrl(String strUrl) throws IOException {
			String data = "";
			InputStream iStream = null;
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(strUrl);
				// Creating an http connection to communicate with url
				urlConnection = (HttpURLConnection) url.openConnection();
				// Connecting to url
				urlConnection.connect();
				// Reading data from url
				iStream = urlConnection.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						iStream));
				StringBuffer sb = new StringBuffer();
				String line = "";
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				data = sb.toString();
				br.close();

			} catch (Exception e) {
				Log.d("Exception while downloading url", e.toString());
			} finally {
				iStream.close();
				urlConnection.disconnect();
			}
			return data;
		}
	}

	class GooglePlacesReadTask extends AsyncTask<Object, Integer, String> {
		String googlePlacesData = null;
		GoogleMap googleMap;
		LatLng latLng;

		@Override
		protected String doInBackground(Object... inputObj) {
			try {
				googleMap = (GoogleMap) inputObj[0];
				String googlePlacesUrl = (String) inputObj[1];
				latLng = (LatLng) inputObj[2];
				Http http = new Http();
				googlePlacesData = http.read(googlePlacesUrl);
			} catch (Exception e) {
				Log.d("Google Place Read Task", e.toString());
			}
			return googlePlacesData;
		}

		@Override
		protected void onPostExecute(String result) {
			PlacesDisplayTask placesDisplayTask = new PlacesDisplayTask();
			Object[] toPass = new Object[3];
			toPass[0] = googleMap;
			toPass[1] = result;
			toPass[2] = latLng;
			placesDisplayTask.execute(toPass);
		}
	}

	class Http {

		public String read(String httpUrl) throws IOException {
			String httpData = "";
			InputStream inputStream = null;
			HttpURLConnection httpURLConnection = null;
			try {
				URL url = new URL(httpUrl);
				httpURLConnection = (HttpURLConnection) url.openConnection();
				httpURLConnection.connect();
				inputStream = httpURLConnection.getInputStream();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(inputStream));
				StringBuffer stringBuffer = new StringBuffer();
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					stringBuffer.append(line);
				}
				httpData = stringBuffer.toString();
				bufferedReader.close();
			} catch (Exception e) {
				Log.d("Exception - reading Http url", e.toString());
			} finally {
				inputStream.close();
				httpURLConnection.disconnect();
			}
			return httpData;
		}

	}

	class ParserTask extends
			AsyncTask<Object, Integer, List<List<HashMap<String, String>>>> {
		GoogleMap googleMap;

		// Parsing the data in non-ui thread
		@Override
		protected List<List<HashMap<String, String>>> doInBackground(
				Object... objects) {

			JSONObject jObject;
			List<List<HashMap<String, String>>> routes = null;

			try {
				jObject = new JSONObject((String) objects[1]);
				googleMap = (GoogleMap) objects[0];
				DirectionsJSONParser parser = new DirectionsJSONParser();

				// Starts parsing data
				routes = parser.parse(jObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return routes;
		}

		// Executes in UI thread, after the parsing process
		@Override
		protected void onPostExecute(List<List<HashMap<String, String>>> result) {
			ArrayList<LatLng> points = null;
			PolylineOptions lineOptions = null;
			MarkerOptions markerOptions = new MarkerOptions();

			// Traversing through all the routes
			for (int i = 0; i < result.size(); i++) {
				points = new ArrayList<LatLng>();
				lineOptions = new PolylineOptions();

				// Fetching i-th route
				List<HashMap<String, String>> path = result.get(i);

				// Fetching all the points in i-th route
				for (int j = 0; j < path.size(); j++) {
					HashMap<String, String> point = path.get(j);

					double lat = Double.parseDouble(point.get("lat"));
					double lng = Double.parseDouble(point.get("lng"));
					LatLng position = new LatLng(lat, lng);

					points.add(position);
				}

				// Adding all the points in the route to LineOptions
				lineOptions.addAll(points);
				lineOptions.width(5);
				lineOptions.color(Color.rgb(0, 134, 65));
			}

			// Drawing polyline in the Google Map for the i-th route
			googleMap.addPolyline(lineOptions);
		}
	}

	class Places {

		public List<HashMap<String, String>> parse(JSONObject jsonObject) {
			JSONArray jsonArray = null;
			try {
				jsonArray = jsonObject.getJSONArray("results");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return getPlaces(jsonArray);
		}

		private List<HashMap<String, String>> getPlaces(JSONArray jsonArray) {
			int placesCount = jsonArray.length();
			List<HashMap<String, String>> placesList = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> placeMap = null;

			for (int i = 0; i < placesCount; i++) {
				try {
					placeMap = getPlace((JSONObject) jsonArray.get(i));
					placesList.add(placeMap);

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return placesList;
		}

		private HashMap<String, String> getPlace(JSONObject googlePlaceJson) {
			HashMap<String, String> googlePlaceMap = new HashMap<String, String>();
			String placeName = "-NA-";
			String vicinity = "-NA-";
			String latitude = "";
			String longitude = "";
			String reference = "";

			try {
				if (!googlePlaceJson.isNull("name")) {
					placeName = googlePlaceJson.getString("name");
				}
				if (!googlePlaceJson.isNull("vicinity")) {
					vicinity = googlePlaceJson.getString("vicinity");
				}
				latitude = googlePlaceJson.getJSONObject("geometry")
						.getJSONObject("location").getString("lat");
				longitude = googlePlaceJson.getJSONObject("geometry")
						.getJSONObject("location").getString("lng");
				reference = googlePlaceJson.getString("reference");
				googlePlaceMap.put("place_name", placeName);
				googlePlaceMap.put("vicinity", vicinity);
				googlePlaceMap.put("lat", latitude);
				googlePlaceMap.put("lng", longitude);
				googlePlaceMap.put("reference", reference);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return googlePlaceMap;
		}
	}

	class PlacesDisplayTask extends
			AsyncTask<Object, Integer, List<HashMap<String, String>>> {

		JSONObject googlePlacesJson;
		GoogleMap googleMap;
		LatLng latLng;

		@Override
		protected List<HashMap<String, String>> doInBackground(
				Object... inputObj) {

			List<HashMap<String, String>> googlePlacesList = null;
			Places placeJsonParser = new Places();

			try {
				googleMap = (GoogleMap) inputObj[0];
				googlePlacesJson = new JSONObject((String) inputObj[1]);
				latLng = (LatLng) inputObj[2];
				googlePlacesList = placeJsonParser.parse(googlePlacesJson);
			} catch (Exception e) {
				Log.d("Exception", e.toString());
			}
			return googlePlacesList;
		}

		@Override
		protected void onPostExecute(List<HashMap<String, String>> list) {
			for (int i = 0; i < list.size(); i++) {
				MarkerOptions markerOptions = new MarkerOptions();
				HashMap<String, String> googlePlace = list.get(i);
				double lat = Double.parseDouble(googlePlace.get("lat"));
				double lng = Double.parseDouble(googlePlace.get("lng"));

				String placeName = googlePlace.get("place_name");
				String vicinity = googlePlace.get("vicinity");

				markerOptions.position(new LatLng(lat, lng));
				markerOptions.title(placeName + " : " + vicinity);
				markerOptions.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.police_marker));
				googleMap.addMarker(markerOptions);
			}
		}
	}

}
