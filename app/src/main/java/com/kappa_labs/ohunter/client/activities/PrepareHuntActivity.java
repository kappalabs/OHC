package com.kappa_labs.ohunter.client.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.utilities.MinMaxInputFilter;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.RadarSearchRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import layout.HuntOfferFragment;

public class PrepareHuntActivity extends AppCompatActivity implements Utils.OnResponseTaskCompleted, ConnectionCallbacks, OnConnectionFailedListener, TextWatcher, OnMapReadyCallback {

    private static final String TAG = "PrepareHunt";

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    /* Active zone of the activated target */
    private Circle mCircle;

    private static final String SAVED_LAST_LONGITUDE = "last_longitude";
    private static final String SAVED_LAST_LATITUDE = "last_latitude";
    private static final String SAVED_LAST_RADIUS = "last_radius";
    private static final String LATITUDE_TEXTVIEW_KEY = "latitude_textview_key";
    private static final String LONGITUDE_TEXTVIEW_KEY = "longitude_textview_key";
    private static final String RADIUS_TEXTVIEW_KEY = "radius_textview_key";
    private static final String DAYTIME_SPINNER_KEY = "daytime_spinner_key";

    private static final int RADAR_SEARCH_KEY = 4200;

    private static final double DEFAULT_LATITUDE = 50.0797689;
    private static final double DEFAULT_LONGITUDE = 14.4297133;
    private static final int DEFAULT_RADIUS = 10;
    private static final int RADIUS_MIN = 1;
    private static final int RADIUS_MAX = 50;

    /* Request code to use when launching the resolution activity */
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    /* Unique tag for the error dialog fragment */
    private static final String DIALOG_ERROR = "dialog_error";
    /* Bool to track whether the app is already resolving an error */
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private EditText mLongitudeEditText;
    private EditText mLatitudeEditText;
    private EditText mRadiusEditText;
    private Spinner mDaytimeSpinner;

    public static Photo.DAYTIME preferredDaytime = Photo.DAYTIME.UNKNOWN;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepare_hunt);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDaytimeSpinner = (Spinner) findViewById(R.id.spinner_daytime);
        mDaytimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        /* Requests all the types */
                        preferredDaytime = Photo.DAYTIME.UNKNOWN;
                        break;
                    case 1:
                        /* Requests only non-dark photos */
                        preferredDaytime = Photo.DAYTIME.DAY;
                        break;
                    case 2:
                        /* Requests only dark photos */
                        preferredDaytime = Photo.DAYTIME.NIGHT;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* EMPTY */ }
        });

        Button mStartHuntButton = (Button) findViewById(R.id.button_start_new_hunt);
        mStartHuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                /* Get the Player object, if does not exist, show login screen */
                Player player = SharedDataManager.getPlayer(PrepareHuntActivity.this);
                if (player == null) {
                    /* Show login prompt message */
                    Toast.makeText(PrepareHuntActivity.this, getString(R.string.login_prompt),
                            Toast.LENGTH_LONG).show();

                    /* Start the login screen activity */
                    Intent i = new Intent();
                    i.setClass(PrepareHuntActivity.this, LoginActivity.class);
                    startActivity(i);

                    return;
                }

                /* Store the last used position */
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(SAVED_LAST_LATITUDE, Double.doubleToRawLongBits(getLatitude()));
                editor.putLong(SAVED_LAST_LONGITUDE, Double.doubleToRawLongBits(getLongitude()));
                editor.putInt(SAVED_LAST_RADIUS, getRadius());
                editor.apply();

                /* Reset the states for new hunt */
                SharedDataManager.initNewHunt(PrepareHuntActivity.this, false, System.currentTimeMillis());
                SharedDataManager.removeTargets(PrepareHuntActivity.this);
                HuntOfferFragment.clearTargets();

                /* Start radar search to receive list of available places */
                Request request = new RadarSearchRequest(player, getLatitude(), getLongitude(), getRadius() * 1000);

                Utils.RetrieveResponseTask responseTask =
                        Utils.getInstance().new RetrieveResponseTask(PrepareHuntActivity.this,
                                Utils.getServerCommunicationDialog(PrepareHuntActivity.this),
                                RADAR_SEARCH_KEY);
                responseTask.execute(request);
            }
        });

        mLongitudeEditText = (EditText) findViewById(R.id.editText_east);
        mLongitudeEditText.addTextChangedListener(this);
        mLatitudeEditText = (EditText) findViewById(R.id.editText_north);
        mLatitudeEditText.addTextChangedListener(this);
        mRadiusEditText = (EditText) findViewById(R.id.editText_radius);
        mRadiusEditText.setFilters(new MinMaxInputFilter[]{new MinMaxInputFilter(RADIUS_MIN, RADIUS_MAX)});
        mRadiusEditText.addTextChangedListener(this);

        /* Create an instance of GoogleAPIClient */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        /* Register callback on the map fragment */
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment)).getMapAsync(this);

        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putString(LATITUDE_TEXTVIEW_KEY, mLatitudeEditText.getText().toString());
        outState.putString(LONGITUDE_TEXTVIEW_KEY, mLongitudeEditText.getText().toString());
        outState.putString(RADIUS_TEXTVIEW_KEY, mRadiusEditText.getText().toString());
        outState.putInt(DAYTIME_SPINNER_KEY, mDaytimeSpinner.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            if (savedInstanceState.keySet().contains(LATITUDE_TEXTVIEW_KEY)) {
                mLatitudeEditText.setText(savedInstanceState.getString(LATITUDE_TEXTVIEW_KEY));
            }
            if (savedInstanceState.keySet().contains(LONGITUDE_TEXTVIEW_KEY)) {
                mLongitudeEditText.setText(savedInstanceState.getString(LONGITUDE_TEXTVIEW_KEY));
            }
            if (savedInstanceState.keySet().contains(RADIUS_TEXTVIEW_KEY)) {
                mRadiusEditText.setText(savedInstanceState.getString(RADIUS_TEXTVIEW_KEY));
            }
            if (savedInstanceState.keySet().contains(DAYTIME_SPINNER_KEY)) {
                int pos = savedInstanceState.getInt(DAYTIME_SPINNER_KEY);
                if (pos >= 0 && pos < mDaytimeSpinner.getAdapter().getCount()) {
                    mDaytimeSpinner.setSelection(pos);
                }
            }
        } else {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            double latitude = Double.longBitsToDouble(
                    sharedPref.getLong(SAVED_LAST_LATITUDE, Double.doubleToLongBits(DEFAULT_LATITUDE)));
            mLatitudeEditText.setText(String.valueOf(latitude));
            double longitude = Double.longBitsToDouble(
                    sharedPref.getLong(SAVED_LAST_LONGITUDE, Double.doubleToLongBits(DEFAULT_LONGITUDE)));
            mLongitudeEditText.setText(String.valueOf(longitude));
            int radius = sharedPref.getInt(SAVED_LAST_RADIUS, DEFAULT_RADIUS);
            mRadiusEditText.setText(String.valueOf(radius));
        }
    }

    @Override
    public void onResponseTaskCompleted(Request _request, Response response, OHException ohex, Object data) {
        /* Problem on server side */
        if (ohex != null) {
            Toast.makeText(PrepareHuntActivity.this, getString(R.string.ohex_general) + " " + ohex,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.ohex_general) + ohex);
            return;
        }
        /* Problem on client side */
        if (response == null) {
            Log.e(TAG, "Problem on client side -> cannot start the o-hunt yet...");
            Toast.makeText(PrepareHuntActivity.this, getString(R.string.server_unreachable_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* Success */
        List<Place> places = new ArrayList<>();
        if (response.places != null) {
            Collections.addAll(places, response.places);
        }
        if (data instanceof Integer && (Integer) data == RADAR_SEARCH_KEY) {
            Log.d(TAG, "RadarSearch vratil " + places.size() + " mist");
            List<String> radarPlaceIDs = new ArrayList<>();
            for (Place place : places) {
                radarPlaceIDs.add(place.getID());
            }
            HuntActivity.radarPlaceIDs = radarPlaceIDs;

            /* Start the main game activity with these groups of places prepared */
            Intent i = new Intent();
            i.setClass(PrepareHuntActivity.this, HuntActivity.class);
            startActivity(i);
        }
    }

    private double getLongitude() {
        double lon;
        try {
            lon = Double.parseDouble(mLongitudeEditText.getText().toString());
        } catch (NumberFormatException nex) {
            Log.e(TAG, "Longitude edit text contains non-double value!");
            lon = 0;
            mLongitudeEditText.setText(String.valueOf(lon));
        }
        return lon;
    }

    private double getLatitude() {
        double lat;
        try {
            lat = Double.parseDouble(mLatitudeEditText.getText().toString());
        } catch (NumberFormatException nex) {
            Log.e(TAG, "Latitude edit text contains non-double value!");
            lat = 0;
            mLatitudeEditText.setText(String.valueOf(lat));
        }
        return lat;
    }

    private int getRadius() {
        int radius;
        try {
            radius = Integer.parseInt(mRadiusEditText.getText().toString());
        } catch (NumberFormatException nex) {
            Log.e(TAG, "Radius edit text contains non-integer value!");
            radius = DEFAULT_RADIUS;
            mRadiusEditText.setText(String.valueOf(DEFAULT_RADIUS));
        }
        return radius;
    }

    @Override
    protected void onStart() {
        if (!mResolvingError && mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (!mResolvingError && mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (getLatitude() != 0 && getLongitude() != 0) {
            mLastLocation = new Location("dummyprovider");
            mLastLocation.setLatitude(getLatitude());
            mLastLocation.setLongitude(getLongitude());
            //TODO ukladani pomoci LATITUDE_TEXTVIEW_KEY atd...
        } else if (mLastLocation != null) {
            /* Save the retrieved location */
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(SAVED_LAST_LATITUDE, Double.doubleToRawLongBits(mLastLocation.getLatitude()));
            editor.putLong(SAVED_LAST_LONGITUDE, Double.doubleToRawLongBits(mLastLocation.getLongitude()));
            editor.apply();
        } else {
            /* Load location from saved application information */
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            double latitude = Double.longBitsToDouble(
                    sharedPref.getLong(SAVED_LAST_LATITUDE, Double.doubleToLongBits(DEFAULT_LATITUDE)));
            double longitude = Double.longBitsToDouble(
                    sharedPref.getLong(SAVED_LAST_LONGITUDE, Double.doubleToLongBits(DEFAULT_LONGITUDE)));

            mLastLocation = new Location("dummyprovider");
            mLastLocation.setLatitude(latitude);
            mLastLocation.setLongitude(longitude);
        }
        mLatitudeEditText.setText(String.valueOf(mLastLocation.getLatitude()));
        mLongitudeEditText.setText(String.valueOf(mLastLocation.getLongitude()));
        if (mRadiusEditText.getText().toString().isEmpty()) {
            mRadiusEditText.setText(String.valueOf(DEFAULT_RADIUS));
        }

        setNewArea(mLastLocation.getLatitude(), mLastLocation.getLongitude(), getRadius());
    }

    private void setNewArea(double latitude, double longitude, double radius) {
        if (map != null) {
            /* Remove previous area from the map */
            if (mCircle != null) {
                mCircle.remove();
            }

            /* Add new area */
            LatLng ll = new LatLng(latitude, longitude);
            CircleOptions co = new CircleOptions()
                    .center(ll)
                    .radius(radius * 1000)
                    .strokeColor(Color.argb(230, 0, 0, 230))
                    .fillColor(Color.argb(80, 0, 255, 0));
            mCircle = map.addCircle(co);

            /* Move camera to this last known position */
            float zoom = (float) (10f - Math.log(radius / 10f) / Math.log(2f));
            zoom = Math.min(12, Math.max(zoom, 2));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .zoom(zoom)
                    .target(ll)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
        if (mResolvingError) {
            /* Already attempting to resolve an error */
            return;
        }
        if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Pote co se aplikace vrati z fragmentu zpet - problem s pozadovanou API funkci byl vyresen */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (map != null && !mLongitudeEditText.getText().toString().isEmpty()
                && !mLatitudeEditText.getText().toString().isEmpty()
                && !mRadiusEditText.getText().toString().isEmpty()) {
            try {
                double longitude = Double.parseDouble(mLongitudeEditText.getText().toString());
                double latitude = Double.parseDouble(mLatitudeEditText.getText().toString());
                double radius = Double.parseDouble(mRadiusEditText.getText().toString());

                setNewArea(latitude, longitude, radius);
            } catch (NumberFormatException ex) {
                /* Do nothing, wait for proper input */
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
//        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
//        map.setTrafficEnabled(true);
//        map.setIndoorEnabled(true);
//        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                setNewArea(latLng.latitude, latLng.longitude, getRadius());
                mLongitudeEditText.setText(String.format(Locale.ENGLISH, "%.7f", latLng.longitude));
                mLatitudeEditText.setText(String.format(Locale.ENGLISH, "%.7f", latLng.latitude));
            }
        });
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((PrepareHuntActivity) getActivity()).onDialogDismissed();
        }
    }

}