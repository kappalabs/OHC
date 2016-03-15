package com.kappa_labs.ohunter.client;

import android.app.Activity;
import android.content.Intent;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import layout.HuntActionFragment;
import layout.HuntOfferFragment;
import layout.HuntPlaceFragment;


public class HuntActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, HuntOfferFragment.OnFragmentInteractionListener, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "HuntActivity";
    public static final String LOCATION_KEY = "location_key";
    public static final String LAST_UPDATED_TIME_STRING_KEY = "last_updated_time_string_key";

    public static final String REJECT_BUTTON_VISIBLE_KEY = "REJECT_BUTTON_VISIBLE_KEY";
    public static final String ACCEPT_BUTTON_VISIBLE_KEY = "ACCEPT_BUTTON_VISIBLE_KEY";
    public static final String ROTATE_BUTTON_VISIBLE_KEY = "ROTATE_BUTTON_VISIBLE_KEY";
    public static final String ACTIVATE_BUTTON_VISIBLE_KEY = "ACTIVATE_BUTTON_VISIBLE_KEY";
    public static final String CAMERA_BUTTON_VISIBLE_KEY = "CAMERA_BUTTON_VISIBLE_KEY";

    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 8000;
    private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * Radius around the target when the camera can activate.
     */
    public static final int RADIUS = 150;
    private static final int MAKE_PHOTO_REQUEST = 0x01;
//    private static final int PERMISSIONS_REQUEST_CHECK_SETTINGS = 0x01;
//    private static final int PERMISSIONS_REQUEST_LOCATION = 0x02;

    public static ArrayList<String> radarPlaceIDs = new ArrayList<>(0);
    public static Activity hunt;

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;

    private FloatingActionButton rejectFab, acceptFab, rotateFab, activateFab, cameraFab;
    private HuntOfferFragment mHuntOfferFragment;
    private HuntPlaceFragment mHuntPlaceFragment;
    private HuntActionFragment mHuntActionFragment;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hunt);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        hunt = this;

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
                        cameraFab.hide();
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
                        if (SharedDataManager.getActivatedPlaceID(HuntActivity.this) != null) {
                            cameraFab.show();
                        } else {
                            cameraFab.hide();
                        }
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
        rejectFab.setVisibility(View.GONE);
        rejectFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HuntOfferFragment.rejectSelectedTarget();
            }
        });

        /* Button for accept of a place */
        acceptFab = (FloatingActionButton) findViewById(R.id.fab_accept);
        acceptFab.setVisibility(View.GONE);
        acceptFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HuntOfferFragment.acceptSelectedTarget();
            }
        });

        /* Button to rotate the tile */
        rotateFab = (FloatingActionButton) findViewById(R.id.fab_rotate);
        rotateFab.setVisibility(View.GONE);
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
        activateFab.setVisibility(View.GONE);
        activateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null) {
                    if (!mHuntOfferFragment.activateSelectedTarget()) {
                        mHuntOfferFragment.deactivateSelectedTarget();
                    }
                }
            }
        });

        /* Button for starting the camera activity - taking similar photo */
        cameraFab = (FloatingActionButton) findViewById(R.id.fab_camera);
        cameraFab.setVisibility(View.GONE);
        cameraFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntPlaceFragment == null) {
                    Log.e(TAG, "Can't access the place fragment yet!");
                    return;
                }
                /* Change state of this target */
                HuntOfferFragment.photogenifySelectedTarget();
                /* Retrieve reference to selected bitmap */
                Bitmap selBitmap = mHuntPlaceFragment.getSelectedBitmap();
                if (selBitmap == null) {
                    Toast.makeText(HuntActivity.this, R.string.select_photo_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap templateBitmap;
                /* Change the orientation of the picture if necessary */
                if (selBitmap.getWidth() < selBitmap.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);

                    templateBitmap = Bitmap.createBitmap(selBitmap, 0, 0, selBitmap.getWidth(),
                            selBitmap.getHeight(), matrix, true);
                    /* We need to make a copy of the image before sending it to camera */
                    templateBitmap = Bitmap.createScaledBitmap(templateBitmap,
                            templateBitmap.getWidth(), templateBitmap.getHeight(), false);
                } else {
                    /* We need to make a copy of the image before sending it to camera */
                    templateBitmap = selBitmap.copy(selBitmap.getConfig(), true);
                }
                /* Start camera activity with the template bitmap on background */
                CameraActivity.setTemplateImage(templateBitmap);
                Intent intent = new Intent();
                intent.setClass(HuntActivity.this, CameraActivity.class);
                startActivityForResult(intent, MAKE_PHOTO_REQUEST);
            }
        });

        /* Update values using data stored in the Bundle */
        updateValuesFromBundle(savedInstanceState);

        /* Create an instance of GoogleAPIClient */
        buildGoogleApiClient();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        HuntOfferFragment.saveTargets(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION_KEY, mCurrentLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        /* Save buttons state */
        if (rejectFab != null) {
            outState.putBoolean(REJECT_BUTTON_VISIBLE_KEY, rejectFab.isShown());
        }
        if (acceptFab != null) {
            outState.putBoolean(ACCEPT_BUTTON_VISIBLE_KEY, acceptFab.isShown());
        }
        if (rotateFab != null) {
            outState.putBoolean(ROTATE_BUTTON_VISIBLE_KEY, rotateFab.isShown());
        }
        if (activateFab != null) {
            outState.putBoolean(ACTIVATE_BUTTON_VISIBLE_KEY, activateFab.isShown());
        }
        if (cameraFab != null) {
            outState.putBoolean(CAMERA_BUTTON_VISIBLE_KEY, cameraFab.isShown());
        }

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            if (mHuntActionFragment != null) {
                mHuntActionFragment.changeLocation(mCurrentLocation);
            }

            /* Load buttons state and show them if possible */
            if (savedInstanceState.getBoolean(REJECT_BUTTON_VISIBLE_KEY, false)) {
                rejectFab.show();
            }
            if (savedInstanceState.getBoolean(ACCEPT_BUTTON_VISIBLE_KEY, false)) {
                acceptFab.show();
            }
            if (savedInstanceState.getBoolean(ROTATE_BUTTON_VISIBLE_KEY, false)) {
                rotateFab.show();
            }
            if (savedInstanceState.getBoolean(ACTIVATE_BUTTON_VISIBLE_KEY, false)) {
                activateFab.show();
            }
            if (savedInstanceState.getBoolean(CAMERA_BUTTON_VISIBLE_KEY, false)) {
                cameraFab.show();
            }
        }

        /* Send the selected place to children pages */
        String selectedID = SharedDataManager.getSelectedPlaceID(this);
        Place selected = SharedDataManager.getPlace(this, selectedID);
        if (selected != null) {
            HuntPlaceFragment.changePlace(this, selected);
            HuntActionFragment.changePlace(selected);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAKE_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "camera result ok...");
                if (data != null) {
                    if (data.getBooleanExtra(CameraActivity.PHOTOS_TAKEN_KEY, false)) {
                        HuntOfferFragment.lockSelectedTarget();
                        Log.d(TAG, "camera result: bylo vyfoceno misto");
                    }
                    if (data.getBooleanExtra(CameraActivity.PHOTOS_EVALUATED_KEY, false)) {
                        HuntOfferFragment.completeSelectedTarget();
                        Log.d(TAG, "camera result: bylo vyfoceno a vyhodnoceno misto");
                    }
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
//                    Manifest.permission.ACCESS_FINE_LOCATION)) {
//                //TODO: vytvorit dialog s vysvetlenim duvodu pozadavku
//            } else {
//                ActivityCompat.requestPermissions(getActivity(),
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                        PERMISSIONS_REQUEST_LOCATION);
//            }
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mHuntActionFragment != null) {
            mHuntActionFragment.changeLocation(mLastLocation);
        }
        createLocationRequest();
        startLocationUpdates();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //TODO: behem komunikace se serverem prenastavit na mensi nebo vypnout pro zamezeni prehlceni
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
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
    public void onPlaceSelected(Place place) {
        String savedID = SharedDataManager.getSelectedPlaceID(this);
        if (!Objects.equals(place.getID(), savedID)) {
            SharedDataManager.saveSelectedPlaceID(this, place.getID());
            HuntPlaceFragment.changePlace(this, place);
            HuntActionFragment.changePlace(place);
        }
    }

    @Override
    public void onItemSelected(Target.TargetState state) {
        switch (state) {
            case ACCEPTED:
                acceptFab.hide();
                rejectFab.show();
                activateFab.show();
                break;
            case ACTIVATED:
                acceptFab.hide();
                rejectFab.hide();
                activateFab.show();
                break;
            case REJECTED:
                acceptFab.show();
                rejectFab.hide();
                activateFab.hide();
                break;
            default:
                acceptFab.setVisibility(View.GONE);
                rejectFab.setVisibility(View.GONE);
                activateFab.setVisibility(View.GONE);
                break;
        }
        rotateFab.show();
    }

    @Override
    public void onRequestNextPage() {
        acceptFab.setVisibility(View.GONE);
        rejectFab.setVisibility(View.GONE);
        activateFab.setVisibility(View.GONE);
        rotateFab.setVisibility(View.GONE);
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
