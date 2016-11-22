package com.example.crimespotmapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crimespotmapper.utils.ConstantHelper;
import com.example.crimespotmapper.utils.InternetConnectionCheck;
import com.example.crimespotmapper.utils.JSONParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class CrimeSpotActivity extends FragmentActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, OnClickListener {

	private ProgressDialog pDialog;
	private JSONParser jsonParser = new JSONParser();
	private JSONArray crimeSpots;

	private AsyncTask<String, String, String> asyncTask;

	private double currentMarkerLat;
	private double currentMarkerLng;

	private static final int MAP_TYPE_NONE = 0;
	private static final int MAP_TYPE_NORMAL = 1;
	private static final int MAP_TYPE_SATELLITE = 2;
	private static final int MAP_TYPE_TERRAIN = 3;
	private static final int MAP_TYPE_HYBRID = 4;

	
	private String locality = "Chittagong";

	protected static final int SEARCH_IN_WEEK = 0;
	protected static final int SEARCH_IN_DAYS = 1;
	protected static final int SEARCH_IN_MONTHS = 2;
	protected static final int SEARCH_IN_MONTH = 5;
	protected static final int SEARCH_IN_YEAR = 3;

	private ArrayList<String> crimeMarkerNames;
	private ArrayList<Double> crimeLocLatitude;
	private ArrayList<Double> crimeLocLongitude;
	private ArrayList<String> crimeTypes;

	private GoogleMap mMap;
	private GoogleApiClient apiClient;

	private Button findButton;
	private Button myLocationButton;
	private Button refreshButton;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (servicesOK()) {
			setContentView(R.layout.activity_crime_spot);

			findButton = (Button) findViewById(R.id.findButton);
			myLocationButton = (Button) findViewById(R.id.myLocationButton);
			refreshButton = (Button) findViewById(R.id.refreshButton);

			findButton.setOnClickListener(this);
			myLocationButton.setOnClickListener(this);
			refreshButton.setOnClickListener(this);
			

			if (initMap()) {
				LatLng ll = new LatLng(ConstantHelper.CHITTAGONG_LAT, ConstantHelper.CHITTAGONG_LNG);
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, ConstantHelper.DEFAULTZOOM);
				mMap.moveCamera(update);
				apiClient = new GoogleApiClient.Builder(this)
						.addApi(LocationServices.API)
						.addConnectionCallbacks(this)
						.addOnConnectionFailedListener(this).build();
				apiClient.connect();
				runLoadAllCrimeSpots();
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

				mMap.setInfoWindowAdapter(new InfoWindowAdapter() {

					@Override
					public View getInfoWindow(Marker marker) {
						
						if (marker.getPosition().latitude == currentMarkerLat && marker.getPosition().longitude == currentMarkerLng) {
							return null;
						}
						
						int latIndex = crimeLocLatitude.indexOf(marker
								.getPosition().latitude);

						View view = getLayoutInflater().inflate(
								R.layout.info_windows, null);

						ImageView crimeIcon = (ImageView) view
								.findViewById(R.id.crime_ic);
						TextView crimeType = (TextView) view
								.findViewById(R.id.crime_type);

						crimeType.setText(crimeTypes.get(latIndex));

						int resourceID = getResources().getIdentifier(
								crimeMarkerNames.get(latIndex), "drawable",
								getPackageName());
						crimeIcon.setImageResource(resourceID);

						return view;
					}

					@Override
					public View getInfoContents(Marker marker) {
						return null;
					}
				});

				mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

					@Override
					public void onInfoWindowClick(Marker marker) {
						Intent intent = new Intent(CrimeSpotActivity.this,
								CrimeSpotDetailsActivity.class);
						intent.putExtra("lat", marker.getPosition().latitude);
						intent.putExtra("lng", marker.getPosition().longitude);
						startActivity(intent);
					}
				});

				mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

					@Override
					public void onMapLongClick(LatLng ll) {
						Intent intent = new Intent(CrimeSpotActivity.this,
								AddNewSpotActivity.class);
						intent.putExtra("lat", ll.latitude);
						intent.putExtra("lng", ll.longitude);
						startActivity(intent);
					}
				});

				mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
					@Override
					public boolean onMarkerClick(Marker marker) {
						
						return false;
					}
				});
			}
		}
		return (mMap != null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.crime_spot, menu);
		return true;
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

	private void setMarkers(double lat, double lng, int markerIconRes) {
		MarkerOptions options = new MarkerOptions().position(
				new LatLng(lat, lng)).icon(
				BitmapDescriptorFactory.fromResource(markerIconRes));

		if (markerIconRes == 0) {
			options.icon(BitmapDescriptorFactory.defaultMarker());
		}
		mMap.addMarker(options);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.mapType: {
			final String[] mapTypes = { "None", "Normal", "Satellite",
					"Terrain", "Hybrid" };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Set Map Type");
			builder.setCancelable(true);
			builder.setItems(mapTypes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case MAP_TYPE_NONE:
						mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
						break;

					case MAP_TYPE_NORMAL:
						mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
						break;

					case MAP_TYPE_SATELLITE:
						mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
						break;

					case MAP_TYPE_TERRAIN:
						mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
						break;

					case MAP_TYPE_HYBRID:
						mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
						break;

					default:
						break;
					}
				}
			});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			AlertDialog mapTypeDialog = builder.create();
			mapTypeDialog.show();
		}
			break;

		case R.id.crimeDuration: {
			
			final String[] crimeDuration = { "Last week", "Last 15 days",
					"Last month", "Last 6 month", "Last year" };

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Crime Duration");
			builder.setCancelable(true);
			builder.setItems(crimeDuration,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (InternetConnectionCheck.haveNetworkConnection(CrimeSpotActivity.this)) {
								if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
									asyncTask.cancel(false);
								}
								switch (which) {
								case SEARCH_IN_WEEK:
									asyncTask = new LoadSpecificCrimeSpots("7", "DAY")
											.execute();
									break;

								case SEARCH_IN_DAYS:
									asyncTask = new LoadSpecificCrimeSpots("15", "DAY")
											.execute();
									break;

								case SEARCH_IN_MONTH:
									asyncTask = new LoadSpecificCrimeSpots("1", "MONTH")
											.execute();
									break;

								case SEARCH_IN_MONTHS:
									asyncTask = new LoadSpecificCrimeSpots("6", "MONTH")
											.execute();
									break;

								case SEARCH_IN_YEAR:
									asyncTask = new LoadSpecificCrimeSpots("1", "YEAR")
											.execute();
									break;

								default:
									break;
								}
							} else {
								Toast.makeText(CrimeSpotActivity.this, "Internet connection not available",
										Toast.LENGTH_SHORT).show();
							}
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();

		}
			break;
			
		case R.id.addNewSpot:
			Toast.makeText(this, "Long pressed on the map to add a new place..", Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (asyncTask != null) {
			if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
				asyncTask.cancel(true);				
			}
		}
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.findButton) {
			LayoutInflater inflater = LayoutInflater
					.from(CrimeSpotActivity.this);
			View view = inflater.inflate(R.layout.search_dialog, null);

			final EditText searchET = (EditText) view
					.findViewById(R.id.etSearch);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Find Location");
			builder.setView(view);

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							
							String location = searchET.getText().toString();

							Geocoder gc = new Geocoder(CrimeSpotActivity.this);
							List<Address> addresses = null;
							try {
								addresses = gc.getFromLocationName(location, 1);
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
							try {
								Address address = addresses.get(0);
								locality = address.getLocality();
								gotoLocation(address.getLatitude(),
										address.getLongitude(), ConstantHelper.DEFAULTZOOM);
								setMarkers(address.getLatitude(),
										address.getLongitude(),
										R.drawable.find_marker);
								currentMarkerLat = address.getLatitude();
								currentMarkerLng = address.getLongitude();
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(CrimeSpotActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
								return;
							}
							runLoadAllCrimeSpots();
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
			Location currentLocation = LocationServices.FusedLocationApi
					.getLastLocation(apiClient);
			if (currentLocation == null) {
				Toast.makeText(this, "Current location isn't available",
						Toast.LENGTH_SHORT).show();
			} else {
				
				locality = getLocality(currentLocation.getLatitude(), currentLocation.getLongitude());
				currentMarkerLat = currentLocation.getLatitude();
				currentMarkerLng = currentLocation.getLongitude();
				gotoLocation(currentLocation.getLatitude(),
						currentLocation.getLongitude(), 16);
				setMarkers(currentLocation.getLatitude(),
						currentLocation.getLongitude(), R.drawable.i_am_here);
				
				runLoadAllCrimeSpots();
			}
		} else if (v.getId() == R.id.refreshButton) {
			runLoadAllCrimeSpots();
		}
	}

	private String getLocality(double latitude, double longitude) {
		
		Geocoder gc = new Geocoder(this);
		List<Address> addresses = null;
		try {
			addresses = gc.getFromLocation(latitude, longitude, 1);
			locality = addresses.get(0).getLocality();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return locality;
	}

	private void runLoadAllCrimeSpots() {
		if (InternetConnectionCheck.haveNetworkConnection(this)) {
			if (asyncTask == null) {
				// --- create a new task --
				asyncTask = new LoadAllCrimeSpots();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.FINISHED) {
				asyncTask = new LoadAllCrimeSpots();
				asyncTask.execute();
			} else if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
				asyncTask.cancel(false);
				asyncTask = new LoadAllCrimeSpots();
				asyncTask.execute();
			}
		} else {
			Toast.makeText(this, "Internet connection not available",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	class LoadSpecificCrimeSpots extends AsyncTask<String, String, String> {
		String interval;
		String format;
		
		public LoadSpecificCrimeSpots(String interval, String format) {
			this.interval = interval;
			this.format = format;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(CrimeSpotActivity.this);
			pDialog.setMessage("Loading Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		protected String doInBackground(String... value) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("interval", interval));
			params.add(new BasicNameValuePair("format", format));
			// getting JSON string from URL
			jsonParser = new JSONParser();
			JSONObject json = jsonParser.makeHttpRequest(
					ConstantHelper.url_specific_crime_spots, "GET", params);
			try {
				// Checking for SUCCESS TAG
				int success = json.getInt(ConstantHelper.TAG_SUCCESS);
				if (success == 1) {
					crimeSpots = json.getJSONArray(ConstantHelper.TAG_CRIME_SPOTS);

					crimeMarkerNames = new ArrayList<>();
					crimeLocLatitude = new ArrayList<>();
					crimeLocLongitude = new ArrayList<>();
					crimeTypes = new ArrayList<>();

					for (int i = 0; i < crimeSpots.length(); i++) {
						JSONObject c = crimeSpots.getJSONObject(i);

						crimeLocLatitude.add(c.getDouble(ConstantHelper.TAG_LATITUDE));
						crimeLocLongitude.add(c.getDouble(ConstantHelper.TAG_LONGITUDE));
						crimeMarkerNames.add(c.getString(ConstantHelper.TAG_IMAGE));
						crimeTypes.add(c.getString(ConstantHelper.TAG_TYPES));
					}
				} else {

				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
			}

			return null;
		}

		protected void onPostExecute(String file_url) {
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {
					mMap.clear();
					if (crimeLocLatitude.size() > 0) {
						Toast.makeText(CrimeSpotActivity.this,
								crimeLocLatitude.size() + " crime spots found",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(CrimeSpotActivity.this,
								"No crime spots found", Toast.LENGTH_SHORT)
								.show();
					}
					for (int i = 0; i < crimeLocLatitude.size(); i++) {
						int iconId = getResources().getIdentifier(
								"m" + crimeMarkerNames.get(i), "drawable",
								getPackageName());

						CrimeSpotActivity.this.setMarkers(
								crimeLocLatitude.get(i),
								crimeLocLongitude.get(i), iconId);
					}
				}
			});

		}

	}

	class LoadAllCrimeSpots extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(CrimeSpotActivity.this);
			pDialog.setMessage("Loadings Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					
				}
			});
			pDialog.show();
		}

		protected String doInBackground(String... args) {
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("locality", locality));
			// getting JSON string from URL
			jsonParser = new JSONParser();
			JSONObject json = jsonParser.makeHttpRequest(ConstantHelper.url_all_crime_spots,
					"GET", params);
			try {
				// Checking for SUCCESS TAG
				crimeMarkerNames = new ArrayList<>();
				crimeLocLatitude = new ArrayList<>();
				crimeLocLongitude = new ArrayList<>();
				crimeTypes = new ArrayList<>();
				int success = json.getInt(ConstantHelper.TAG_SUCCESS);
				if (success == 1) {
					crimeSpots = json.getJSONArray(ConstantHelper.TAG_CRIME_SPOTS);

					for (int i = 0; i < crimeSpots.length(); i++) {
						JSONObject c = crimeSpots.getJSONObject(i);

						crimeLocLatitude.add(c.getDouble(ConstantHelper.TAG_LATITUDE));
						crimeLocLongitude.add(c.getDouble(ConstantHelper.TAG_LONGITUDE));
						crimeMarkerNames.add(c.getString(ConstantHelper.TAG_IMAGE));
						crimeTypes.add(c.getString(ConstantHelper.TAG_TYPES));
					}
				} else {

				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
			}

			return null;
		}

		protected void onPostExecute(String file_url) {
			pDialog.dismiss();
			Log.i("test", "AsynchTask Stopped");
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {
					if (crimeLocLatitude.size() > 0) {
						Toast.makeText(CrimeSpotActivity.this,
								crimeLocLatitude.size() + " crime spots found in " + locality + " Locality",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(CrimeSpotActivity.this,
								"No crime spots found in " + locality + " Locality", Toast.LENGTH_SHORT)
								.show();
					}
					for (int i = 0; i < crimeLocLatitude.size(); i++) {
						int iconId = getResources().getIdentifier(
								"m" + crimeMarkerNames.get(i), "drawable",
								getPackageName());

						CrimeSpotActivity.this.setMarkers(
								crimeLocLatitude.get(i),
								crimeLocLongitude.get(i), iconId);
					}
				}
			});

		}

	}
}
