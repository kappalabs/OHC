package com.kappa_labs.ohunter.client;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import layout.HuntActionFragment;
import layout.HuntOfferFragment;
import layout.HuntPlaceFragment;


public class HuntActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, HuntOfferFragment.OnFragmentInteractionListener, HuntPlaceFragment.OnFragmentInteractionListener, HuntActionFragment.OnFragmentInteractionListener, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "HuntActivity";
    public static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates_key";
    public static final String LOCATION_KEY = "location_key";
    public static final String LAST_UPDATED_TIME_STRING_KEY = "last_updated_time_string_key";

    public static final String REJECT_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";
    public static final String ACCEPT_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";
    public static final String ROTATE_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";
    public static final String ACTIVATE_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";
    public static final String CAMERA_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private boolean mRequestingLocationUpdates = false;

    FloatingActionButton rejectFab, acceptFab, rotateFab, activateFab, cameraFab;
//    private boolean item_selected = false;
//    public static ArrayList<Place> green_places, red_places;
    public static ArrayList<String> radarPlaceIDs;
    private HuntOfferFragment mHuntOfferFragment;
    private HuntPlaceFragment mHuntPlaceFragment;
    private HuntActionFragment mHuntActionFragment;
    ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hunt);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        /* Create the adapter that will return a fragment for each of the three
           primary sections of the activity */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        /* Set up the ViewPager with the sections adapter */
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Fragment fragment = null;
                switch (position) {
                    case 0: // HuntOfferFragment
                        //TODO: stavy spis bude lepsi nekam ukladat
                        if (SharedDataManager.getSelectedPlaceID(HuntActivity.this) != null) {
                            activateFab.show();
                        } else {
                            activateFab.hide();
                        }
//                        if (item_selected) {
//                            activateFab.show();
//                        } else {
//                            activateFab.hide();
//                        }
                        cameraFab.hide();
                        rotateFab.hide();
                        fragment = mHuntOfferFragment;
                        break;
                    case 1: // HuntPlaceFragment
                        rejectFab.hide();
                        acceptFab.hide();
                        rotateFab.hide();
                        activateFab.hide();
                        cameraFab.hide();
                        fragment = mHuntPlaceFragment;
                        break;
                    case 2: // HuntActionFragment
                        rejectFab.hide();
                        acceptFab.hide();
                        rotateFab.hide();
                        activateFab.hide();
                        // TODO: dalsi logika zahrnujici vzdalenost od vybraneho cile
                        //TODO: ziskat data z manageru
                        if (SharedDataManager.getActivatedPlaceID(HuntActivity.this) != null) {
                            cameraFab.show();
                        } else {
                            cameraFab.hide();
                        }
//                        if (mHuntOfferFragment != null && mHuntOfferFragment.hasActivatedTarget()) {
//                            cameraFab.show();
//                        } else {
//                            cameraFab.hide();
//                        }
                        fragment = mHuntActionFragment;
                        break;
                }
                if (fragment != null) {
                    ((PageChangeAdapter) fragment).onPageSelected();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        /* Button to reject a place */
        rejectFab = (FloatingActionButton) findViewById(R.id.fab_reject);
        rejectFab.hide();
        rejectFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null && mHuntOfferFragment.rejectSelectedTarget()) {
                    rejectFab.hide();
                    acceptFab.show();
                    activateFab.hide();
                }
            }
        });

        /* Button for accept of a place */
        acceptFab = (FloatingActionButton) findViewById(R.id.fab_accept);
        acceptFab.hide();
        acceptFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null && mHuntOfferFragment.acceptSelectedTarget()) {
                    rejectFab.show();
                    acceptFab.hide();
                    activateFab.show();
                }
            }
        });

        /* Button to rotate the tile */
        rotateFab = (FloatingActionButton) findViewById(R.id.fab_rotate);
        rotateFab.hide();
        rotateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null) {
                    mHuntOfferFragment.rotateSelectedTile();
                }
            }
        });

        /* Button to activate a place for hunt */
        activateFab = (FloatingActionButton) findViewById(R.id.fab_activate);
        activateFab.hide();
        activateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null && mHuntOfferFragment.activateSelectedTarget()) {
                    rejectFab.hide();
                    acceptFab.hide();
                    activateFab.hide();
                }
            }
        });

        /* Button for starting the camera activity - taking similar photo */
        cameraFab = (FloatingActionButton) findViewById(R.id.fab_camera);
        cameraFab.hide();
        cameraFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntPlaceFragment == null) {
                    Log.e(TAG, "Can't access the place fragment yet!");
                    return;
                }
                Bitmap bitmap = mHuntPlaceFragment.getSelectedPicture();
                if (bitmap == null) {
                    Toast.makeText(HuntActivity.this, R.string.select_photo_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                /* Change the orientation of the picture if necessary */
                if (bitmap.getWidth() < bitmap.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);

                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    bitmap = rotatedBitmap;
                }
                CameraActivity.setTemplateImage(bitmap);
                Intent intent = new Intent();
                intent.setClass(HuntActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        /* Create an instance of GoogleAPIClient */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO: podle Google je to vhodne, zpusobuje vsak ojedinele problemy...
//        stopLocationUpdates();
    }

//    private void stopLocationUpdates() {
//        if (mGoogleApiClient.isConnected()){
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            mRequestingLocationUpdates = false;
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
//            startLocationUpdates();
//        }
//    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mCurrentLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        //TODO: stav tlacitka - viditelnost po rotaci!

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            if (mHuntActionFragment != null) {
                mHuntActionFragment.changeLocation(mCurrentLocation);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
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
        if (mHuntActionFragment != null) {
            mHuntActionFragment.changeLocation(mLastLocation);
        }
        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //TODO: behem komunikace se serverem prenastavit na mensi nebo vypnout pro zamezeni prehlceni
        mLocationRequest.setInterval(8000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
//                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        //...
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(HuntActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        //...
                        break;
                }
            }
        });
    }

    private void startLocationUpdates() {
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
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mRequestingLocationUpdates = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (mHuntActionFragment != null) {
            mHuntActionFragment.changeLocation(mCurrentLocation);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_hunt, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onItemSelected(Place place) {
//        item_selected = true;
        SharedDataManager.saveSelectedPlaceID(this, place.getID());
        HuntPlaceFragment.changePlace(place);
        HuntActionFragment.changePlace(place);

        /* Hide all buttons, offer will fire method to show the right ones.
         * NOTE: cannot use hide(), causes strange button behavior */
        acceptFab.setVisibility(View.INVISIBLE);
        rejectFab.setVisibility(View.INVISIBLE);
        activateFab.setVisibility(View.INVISIBLE);
        rotateFab.show();
    }

    @Override
    public void onGreenSelected() {
        acceptFab.hide();
        rejectFab.show();
        activateFab.show();
    }

    @Override
    public void onRedSelected() {
        acceptFab.show();
        rejectFab.hide();
        activateFab.hide();
    }

    @Override
    public void onRequestNextPage() {
        /* Go to the page with place information */
        mViewPager.setCurrentItem(1, true);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private String tabtitles[] = new String[] {
                getString(R.string.title_fragment_offer),
                getString(R.string.title_fragment_info),
                getString(R.string.title_fragment_hunt)};


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mHuntOfferFragment = (HuntOfferFragment) fragment;
                    break;
                case 1:
                    mHuntPlaceFragment = (HuntPlaceFragment) fragment;
                    break;
                default:
                    mHuntActionFragment = (HuntActionFragment) fragment;
                    break;
            }
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
//            Place demoPlace = green_places != null && green_places.size() > 0 ? green_places.get(0) : null;
            if (position == 0) {
//                HuntOfferFragment.mParamGreen = green_places;
//                HuntOfferFragment.mParamRed = red_places;
                return HuntOfferFragment.newInstance();
            } if (position == 1) {
                return HuntPlaceFragment.newInstance();
            } else {
                return HuntActionFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabtitles[position];
        }

    }

}
