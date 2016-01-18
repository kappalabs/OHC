package client.ohunter.fojjta.cekuj.net.ohunter;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.RegisterRequest;
import com.kappa_labs.ohunter.lib.requests.Request;
import com.kappa_labs.ohunter.lib.requests.SearchRequest;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class PrepareHuntActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, TextWatcher {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    /* Zvyraznena oblast vyhledavani */
    private Circle mCircle;
    private static final String TAG = "PrepareHunt";

    private static final double DEFAULT_LATITUDE = 50.0797689;
    private static final double DEFAULT_LONGITUDE = 14.4297133;
    private static final double DEFAULT_RADIUS = 20;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private Button mStartHuntButton;
    private EditText mLongitudeEditText;
    private EditText mLatitudeEditText;
    private EditText mRadiusEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepare_hunt);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mStartHuntButton = (Button) findViewById(R.id.button_start_new_hunt);
        /* TODO: nechci focus na latitude! -> otevirani klavesnice */
        mStartHuntButton.setSelected(true);
        mStartHuntButton.requestFocus();
        mStartHuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                ArrayList<Place> places = new ArrayList<>();
                try {
//                    Socket server = new Socket("192.168.1.196", 4242);
//                    Socket server = new Socket("192.168.42.56", 4242);
                    Socket server = new Socket("192.168.43.144", 4242);
                    try {
                        Log.d(TAG, "Pred oos");
                        ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
                        Log.d(TAG, "Data na server");
//                        Request rr = new RegisterRequest("nickClient", "passwdClient");
                        Player p = new Player(1, "nick", 4242);
                        Request sr = new SearchRequest(p, 50.0647411, 14.4196972, 200, 1280, 720);
                        oos.writeObject(sr);
                        oos.flush();
                        Log.d(TAG, "Data OK odeslana");

                        Log.d(TAG, "Pred ois");
                        ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
                        Object obj = ois.readObject();
                        try {
                            Response resp = (Response) obj;
                            Log.d(TAG, "Mam response: null? "+((resp==null)?"ano":"ne"));
                            if (resp.places != null) {
                                places.addAll(resp.places);
                                Log.d(TAG, "mam " + resp.places.size() + " mist");
                                for (Place pl : resp.places) {
                                    Log.d(TAG, pl.toString());
                                }
                            }
                            if (resp.player != null) {
                                Log.d(TAG, "mam hrace: " + resp.player);
                            }
                            if (resp.similarity != Float.NaN) {
                                Log.d(TAG, "mam similarity: " + resp.similarity);
                            }
                        } catch (ClassCastException ex) {
                            if (obj instanceof OHException) {
                                Log.d(TAG, "Vypadla mi OHExceptiona: "+obj);
                            } else {
                                Log.d(TAG, "Server posila neznamy format tridy");
                            }
                        }
                        oos.close();
                        ois.close();
                        server.close();
                    } catch (IOException | ClassNotFoundException e) {
//                        Log.e(TAG, e.getLocalizedMessage());
//                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                } catch (IOException ex) {
//                    Log.e(TAG, ex.getLocalizedMessage());
//                    Log.e(TAG, ex.getMessage());
                    ex.printStackTrace();
                }

////                Bitmap b = places.get(0).photos.get(0).image;
////                DataInputStream data = new DataInputStream(in);
//                int w = 0;
//                int h = 0;
////                try {
////                    w = data.readInt();
////                    h = data.readInt();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//
//                w = places.get(0).photos.get(0).getWidth();
//                byte[] imgBytes = new byte[w * h * 4]; // 4 byte ABGR
////                try {
////                    data.readFully(imgBytes);
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//
//                // Convert 4 byte interleaved ABGR to int packed ARGB
//                int[] pixels = new int[w * h];
//                for (int i = 0; i < pixels.length; i++) {
//                    int byteIndex = i * 4;
//                    pixels[i] =
//                            ((imgBytes[byteIndex    ] & 0xFF) << 24)
//                                    | ((imgBytes[byteIndex + 3] & 0xFF) << 16)
//                                    | ((imgBytes[byteIndex + 2] & 0xFF) <<  8)
//                                    |  (imgBytes[byteIndex + 1] & 0xFF);
//                }
//
//                // Finally, create bitmap from packed int ARGB, using ARGB_8888
//                Bitmap bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);

                HuntActivity.green_places = places;
                HuntActivity.red_places = places;
                Intent i = new Intent();
                i.setClass(PrepareHuntActivity.this, HuntActivity.class);
//                i.putExtra(HuntActivity.GREEN_LIST_KEY, places);
//                i.putExtra(HuntActivity.RED_LIST_KEY, places);
                startActivity(i);
            }
        });

        mLongitudeEditText = (EditText) findViewById(R.id.editText_east);
        mLongitudeEditText.addTextChangedListener(this);
        mLatitudeEditText = (EditText) findViewById(R.id.editText_north);
        mLatitudeEditText.addTextChangedListener(this);
        mRadiusEditText = (EditText) findViewById(R.id.editText_radius);
        mRadiusEditText.addTextChangedListener(this);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            Log.d(TAG, "trying to connect to API");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment)).getMap();
        if (map == null) {
            return;
        }
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

    @Override
    protected void onStart() {
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
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
        if (mLastLocation != null) {
            /* Ziskanou pozici si ulozim */
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(getString(R.string.saved_last_longitude), Double.doubleToRawLongBits(mLastLocation.getLongitude()));
            editor.putLong(getString(R.string.saved_last_latitude), Double.doubleToRawLongBits(mLastLocation.getLatitude()));
            editor.apply();
        } else {
            /* Prectu si pozici z dat aplikace */
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            double latitude = Double.longBitsToDouble(sharedPref.getLong(getString(R.string.saved_last_latitude), Double.doubleToLongBits(DEFAULT_LATITUDE)));
            double longitude = Double.longBitsToDouble(sharedPref.getLong(getString(R.string.saved_last_longitude), Double.doubleToLongBits(DEFAULT_LONGITUDE)));

            mLastLocation = new Location("dummyprovider");
            mLastLocation.setLatitude(latitude);
            mLastLocation.setLongitude(longitude);

            if (latitude == DEFAULT_LATITUDE && longitude == DEFAULT_LONGITUDE) {
                Toast.makeText(this, getString(R.string.last_pos_not_known), Toast.LENGTH_SHORT).show();
            }
        }
        mLongitudeEditText.setText(String.valueOf(mLastLocation.getLongitude()));
        mLatitudeEditText.setText(String.valueOf(mLastLocation.getLatitude()));
        mRadiusEditText.setText(String.valueOf(DEFAULT_RADIUS));

        setNewArea(mLastLocation.getLatitude(), mLastLocation.getLongitude(), DEFAULT_RADIUS);
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
            float zoom = (float)(10 - Math.log(radius / 10) / Math.log(2));
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
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
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
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
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
            double longitude = Double.parseDouble(mLongitudeEditText.getText().toString());
            double latitude = Double.parseDouble(mLatitudeEditText.getText().toString());
            double radius = Double.parseDouble(mRadiusEditText.getText().toString());

            setNewArea(latitude, longitude, radius);
        }
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

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
