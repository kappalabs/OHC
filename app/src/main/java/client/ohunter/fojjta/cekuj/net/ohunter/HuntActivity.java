package client.ohunter.fojjta.cekuj.net.ohunter;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;

import layout.HuntActionFragment;
import layout.HuntOfferFragment;
import layout.HuntPlaceFragment;


public class HuntActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, HuntOfferFragment.OnFragmentInteractionListener, HuntPlaceFragment.OnFragmentInteractionListener, HuntActionFragment.OnFragmentInteractionListener, GoogleApiClient.OnConnectionFailedListener {

//    public static final String GREEN_LIST_KEY = "green_list_key";
//    public static final String RED_LIST_KEY = "red_list_key";
    public static final String TAG = "HuntActivity";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    FloatingActionButton fab_info, fab_camera;
    private boolean item_selected = false;
    private Place place_selected;
    public static ArrayList<Place> green_places, red_places;
    private HuntOfferFragment mHuntOfferFragment;
    private HuntPlaceFragment mHuntPlaceFragment;
    private HuntActionFragment mHuntActionFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hunt);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
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
                        if (item_selected) {
                            fab_info.show();
                        }
                        fab_camera.hide();
                        fragment = mHuntOfferFragment;
                        break;
                    case 1: // HuntPlaceFragment
                        fab_info.hide();
                        fab_camera.hide();
                        fragment = mHuntPlaceFragment;
                        break;
                    case 2: // HuntActionFragment
                        fab_info.hide();
                        fab_camera.show();
                        fragment = mHuntActionFragment;
                        break;
                    default:
                        fab_info.hide();
                        fab_camera.hide();
                        break;
                }
                if (fragment != null) {
                    ((PageChangeAdapter)fragment).onPageSelected();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        fab_info = (FloatingActionButton) findViewById(R.id.fab_info);
        fab_info.setVisibility(View.GONE);
        fab_camera = (FloatingActionButton) findViewById(R.id.fab_camera);
        fab_camera.setVisibility(View.GONE);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
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

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mHuntActionFragment.changeLocation(mLastLocation);
        }
        createLocationRequest();
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(8000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
//        Log.d(TAG, result.await().getStatus().toString());
        //TODO
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_hunt, menu);
//        return true;
//    }

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
        item_selected = true;
        place_selected = place;
        HuntPlaceFragment.changePlace(place);
        HuntActionFragment.changePlace(place);
        fab_info.show();
    }

    @Override
    public void onItemUnselected() {
        item_selected = false;
        fab_info.hide();
    }

    @Override
    public void onGreenRejected(Place place) {

    }

    @Override
    public void onRedRejected(Place place) {

    }

    @Override
    public void onRedAccepted(Place place) {

    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {


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
            Place demoPlace = green_places != null && green_places.size() > 0 ? green_places.get(0) : null;
            if (position == 0) {
                return HuntOfferFragment.newInstance(green_places, red_places);
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

    }
}
