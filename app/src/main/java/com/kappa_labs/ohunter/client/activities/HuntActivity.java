package com.kappa_labs.ohunter.client.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
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
import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.adapters.PageChangeAdapter;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.PhotosManager;
import com.kappa_labs.ohunter.client.utilities.PointsManager;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.TargetsManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.CompareRequest;
import com.kappa_labs.ohunter.lib.requests.CompleteTargetRequest;
import com.kappa_labs.ohunter.lib.requests.RejectPlaceRequest;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import layout.HuntActionFragment;
import layout.HuntOfferFragment;
import layout.HuntPlaceFragment;

/**
 * Activity holding the main game content consisting of three fragments.
 */
public class HuntActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, HuntOfferFragment.OnFragmentInteractionListener, HuntPlaceFragment.OnFragmentInteractionListener, GoogleApiClient.OnConnectionFailedListener, ResponseTask.OnResponseTaskCompleted {

    public static final String TAG = "HuntActivity";
    public static final String LOCATION_KEY = "location_key";
    public static final String LAST_UPDATED_TIME_STRING_KEY = "last_updated_time_string_key";

    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 8000;
    private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * Default radius of active zone around a target (in meters).
     */
    public static final int DEFAULT_RADIUS = 150;
    private static final int MAKE_PHOTO_REQUEST = 0x01;
    private static final int PERMISSIONS_LOCATION_ON_CONNECTED = 0x01;
    private static final int PERMISSIONS_LOCATION_START_UPDATES = 0x02;

    public static List<String> radarPlaceIDs = new ArrayList<>(0);
    public static Activity thisActivity;

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private PointsManager mPointsManager;

    private FloatingActionButton acceptFab, openUpFab, rejectFab;
    private FloatingActionButton cameraFab, evaluateFab, rotateFab, sortFab;
    private HuntOfferFragment mHuntOfferFragment;
    private HuntPlaceFragment mHuntPlaceFragment;
    private HuntActionFragment mHuntActionFragment;
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hunt);

        /* Allows finalization of this activity from the main activity after end of hunt */
        thisActivity = this;

        /* Allow loading/saving targets photos from cache */
        PhotosManager.connect(this);

        /* Create the adapter that will return a fragment for each of the three
           primary sections of the activity */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        /* Create a manager to control the player's score */
        mPointsManager = MainActivity.getPointsManager();

        /* Set up the ViewPager with the sections adapter */
        mViewPager = (ViewPager) findViewById(R.id.container);
        assert mViewPager != null;
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = null;
                switch (position) {
                    case 0: // HuntOfferFragment
                        fragment = mHuntOfferFragment;
                        break;
                    case 1: // HuntPlaceFragment
                        acceptFab.hide();
                        openUpFab.hide();
                        rejectFab.hide();
                        cameraFab.hide();
                        evaluateFab.hide();
                        rotateFab.hide();
                        sortFab.hide();
                        fragment = mHuntPlaceFragment;
                        break;
                    case 2: // HuntActionFragment
                        // TODO: 13.4.16 pouze pro debug, toto tlacitko zde mozna ani nebude
                        Target selected = HuntOfferFragment.getSelectedTarget();
                        if (selected != null && selected.getState().canPhotogenify()) {
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
        });

        /* Button to reject a place */
        rejectFab = (FloatingActionButton) findViewById(R.id.fab_reject);
        assert rejectFab != null;
        rejectFab.setVisibility(View.GONE);
        rejectFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Target selected = HuntOfferFragment.getSelectedTarget();
                /* Reject only if player has enough points and target is selected */
                if (!mPointsManager.canReject() || selected == null) {
                    int points = mPointsManager.countMissingPoints(PointsManager.getRejectCost());
                    Wizard.missingPointsDialog(HuntActivity.this, points);
                    return;
                }
                /* Show reject confirmation dialog */
                Wizard.rejectQuestionDialog(HuntActivity.this, PointsManager.getRejectCost(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                /* Send information about the rejected target into the database on server */
                                String placeID = selected.getPlaceID();
                                DialogFragment dialog = Wizard.getServerCommunicationDialog(mHuntOfferFragment.getActivity());
                                ResponseTask task = new ResponseTask(dialog, HuntActivity.this);
                                task.execute(new RejectPlaceRequest(
                                        SharedDataManager.getPlayer(HuntActivity.this),
                                        placeID,
                                        PointsManager.getRejectCost()
                                ));
                            }
                        }
                );
            }
        });

        /* Button for accept of a place */
        acceptFab = (FloatingActionButton) findViewById(R.id.fab_accept);
        assert acceptFab != null;
        acceptFab.setVisibility(View.GONE);
        acceptFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SharedDataManager.getNumAcceptable(HuntActivity.this) > 0) {
                    /* Show accept confirmation dialog */
                    Wizard.acceptQuestionDialog(HuntActivity.this,
                            SharedDataManager.getNumAcceptable(HuntActivity.this),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    /* Set the state to accepted and decrease the number of acceptable targets */
                                    HuntOfferFragment.restateSelectedTarget(Target.TargetState.ACCEPTED);
                                    if (mHuntActionFragment != null) {
                                        mHuntActionFragment.updateTargetMarks();
                                    }
                                    SharedDataManager.setNumAcceptable(HuntActivity.this,
                                            SharedDataManager.getNumAcceptable(HuntActivity.this) - 1);
                                }
                            });
                } else {
                    Wizard.notEnoughAcceptableDialog(HuntActivity.this);
                }
            }
        });

        /* Button to rotate the tile */
        rotateFab = (FloatingActionButton) findViewById(R.id.fab_rotate);
        assert rotateFab != null;
        rotateFab.setVisibility(View.GONE);
        rotateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHuntOfferFragment != null) {
                    if (HuntOfferFragment.hasSelectedTarget()) {
                        mHuntOfferFragment.rotateSelectedTile();
                    } else {
                        mHuntOfferFragment.rotateAllTiles();
                    }
                }
            }
        });

        /* Button for starting the camera activity - taking similar photo */
        cameraFab = (FloatingActionButton) findViewById(R.id.fab_camera);
        assert cameraFab != null;
        cameraFab.setVisibility(View.GONE);
        cameraFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Target selected = HuntOfferFragment.getSelectedTarget();
                // TODO: 13.4.16 pouze pro debug, aby nebylo nutne k cili primo chodit
                if (selected != null && selected.getState() != Target.TargetState.PHOTOGENIC) {
                    mCurrentLocation = new Location("dummyprovider");
                    mCurrentLocation.setLatitude(selected.latitude);
                    mCurrentLocation.setLongitude(selected.longitude);
                    checkTargetDistance();
                    return;
                }
                /* Check if target is selected and its state is ready for camera */
                if (selected == null || selected.getState() != Target.TargetState.PHOTOGENIC) {
                    Log.e(TAG, "Camera button was not suppose to be available, selected target is null!");
                    return;
                }
                Photo photo = selected.getPhoto(selected.getPhotoIndex());
                if (photo == null) {
                    Toast.makeText(HuntActivity.this, R.string.select_photo_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                /* Start the camera activity */
                CameraActivity.initCamera(selected, photo.reference);
                Intent intent = new Intent();
                intent.setClass(HuntActivity.this, CameraActivity.class);
                startActivityForResult(intent, MAKE_PHOTO_REQUEST);
            }
        });

        /* Button to defer the target */
        openUpFab = (FloatingActionButton) findViewById(R.id.fab_open_up);
        assert openUpFab != null;
        openUpFab.setVisibility(View.GONE);
        openUpFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Defer only if player has enough points */
                if (!mPointsManager.canDefer()) {
                    int points = mPointsManager.countMissingPoints(PointsManager.getDeferCost());
                    Wizard.missingPointsDialog(HuntActivity.this, points);
                    return;
                }
                /* Show defer confirmation dialog */
                Wizard.deferQuestionDialog(HuntActivity.this, PointsManager.getDeferCost(),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* Change the state of the target */
                        if (!HuntOfferFragment.restateSelectedTarget(Target.TargetState.OPENED)) {
                            Log.e(TAG, "Cannot defer selected target. Incorrect state?");
                            return;
                        }
                        if (mHuntActionFragment != null) {
                            mHuntActionFragment.updateTargetMarks();
                        }
                        mPointsManager.removePoints(PointsManager.getDeferCost());

                        /* Send information about the deferred target into the database on server */
                        mPointsManager.updateInDatabase(HuntActivity.this, null);
                    }
                });
            }
        });

        /* Button to evaluate target photos */
        evaluateFab = (FloatingActionButton) findViewById(R.id.fab_evaluate);
        assert evaluateFab != null;
        evaluateFab.setVisibility(View.GONE);
        evaluateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Target selected = HuntOfferFragment.getSelectedTarget();
                if (selected != null) {
                    String placeID = selected.getPlaceID();
                    /* Send only selected target for evaluation */
                    Request request = SharedDataManager.getRequestForTarget(
                            HuntActivity.this, placeID);
                    if (request == null) {
                        Log.e(TAG, "Wrong state of target! This target should be evaluated or not locked.");
                        return;
                    }
                    /* Asynchronously execute and wait for callback when result ready */
                    DialogFragment dialog = Wizard.getServerCommunicationDialog(mHuntOfferFragment.getActivity());
                    ResponseTask task = new ResponseTask(dialog, placeID, HuntActivity.this);
                    task.execute(request);
                } else {
                    /* Send all pending compare requests for evaluation */
                    Set<String> placeIDs = SharedDataManager.getPendingRequestsIDs(HuntActivity.this);
                    if (!placeIDs.isEmpty()) {
                        for (String placeID : placeIDs) {
                            Request request = SharedDataManager.getRequestForTarget(HuntActivity.this, placeID);
                            if (request == null) {
                                Log.e(TAG, "Request for place " + placeID + " is not available, skipping...");
                                continue;
                            }
                            /* Asynchronously execute and wait for callback when result ready */
                            DialogFragment dialog = Wizard.getServerCommunicationDialog(mHuntOfferFragment.getActivity());
                            ResponseTask task = new ResponseTask(dialog, placeID, HuntActivity.this);
                            task.execute(request);
                        }
                    } else {
                        Toast.makeText(HuntActivity.this, getString(R.string.no_pending_evaluation),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        /* Button to sort the targets on offer page according to their states */
        sortFab = (FloatingActionButton) findViewById(R.id.fab_sort);
        assert sortFab != null;
        sortFab.setVisibility(View.GONE);
        sortFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HuntOfferFragment.sortTargets();
            }
        });

        /* Update values using data stored in the Bundle */
        updateValuesFromBundle(savedInstanceState);

        /* Create an instance of GoogleAPIClient */
        buildGoogleApiClient();
    }

    /**
     * Prepares the activity for a new game, must be called before starting this activity
     */
    public static void initNewHunt() {
        HuntOfferFragment.initNewHunt();
        HuntPlaceFragment.initNewHunt();
        HuntActionFragment.initNewHunt();
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
        PhotosManager.connect(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        HuntOfferFragment.cancelDownloadTasks();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        thisActivity = null;
        mGoogleApiClient.disconnect();
        /* Release reference to this context */
        PhotosManager.disconnect(this);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION_KEY, mCurrentLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);

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
        }

        /* Send the selected place to children pages */
        Target selectedTarget = HuntOfferFragment.getSelectedTarget();
        if (selectedTarget != null) {
            HuntPlaceFragment.changePlace(this, selectedTarget);
            HuntActionFragment.changeTarget(selectedTarget);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAKE_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "camera result ok...");
                if (data != null) {
                    if (data.getBooleanExtra(CameraActivity.PHOTOS_TAKEN_KEY, false)) {
                        Log.d(TAG, "camera result: bylo vyfoceno misto");
                        handlePhotoTaken();
                    }
                }
            }
        }
    }

    private void handlePhotoTaken() {
        HuntOfferFragment.restateSelectedTarget(Target.TargetState.LOCKED);
        if (mHuntActionFragment != null) {
            mHuntActionFragment.updateTargetMarks();
        }
        SharedDataManager.addNumAcceptable(this, TargetsManager.DEFAULT_INCREMENT_ACCEPTABLE);
        HuntOfferFragment.randomlyOpenTargets(TargetsManager.DEFAULT_NUM_OPENED);
        Target target = HuntOfferFragment.getSelectedTarget();
        HuntPlaceFragment.changePlace(this, target);
        HuntActionFragment.changeTarget(target);
        SharedDataManager.addTargetToHistory(this, target);
        Wizard.targetLockedDialog(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        /* Connected to Google Play services */
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Wizard.locationPermissionDialog(this);
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_LOCATION_ON_CONNECTED
                );
            }
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
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Wizard.locationPermissionDialog(this);
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_LOCATION_START_UPDATES
                );
            }
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION_ON_CONNECTED:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null);
                }
                break;
            case PERMISSIONS_LOCATION_START_UPDATES:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                }
                break;
        }
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
        checkTargetDistance();
    }

    /**
     * Checks the distance to every target which can be photogenified. Shows notification
     * if the active zone of at least one of them is visited and changes the state of these targets.
     */
    private void checkTargetDistance() {
        if (mCurrentLocation == null) {
            return;
        }
        float[] results = new float[1];
        boolean showNotification = false;
        for (Target target : HuntOfferFragment.getTargets()) {
            /* Check the distance to every target, which can be photogenified */
            if (target.getState().canPhotogenify()) {
                Location.distanceBetween(target.latitude, target.longitude,
                        mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), results);
                float distance = results[0];
                if (distance <= DEFAULT_RADIUS && HuntOfferFragment.restateTarget(target.getPlaceID(), Target.TargetState.PHOTOGENIC)) {
                    if (mHuntActionFragment != null) {
                        mHuntActionFragment.updateTargetMarks();
                    }
                    /* Set the discovery gain for this target */
                    target.setDiscoveryGain(mPointsManager.getTargetDiscoveryGain());
                    showNotification = true;
                }
            }
        }
        /* Show only one notification for all photogenified targets */
        if (showNotification) {
            Wizard.showPhotogenifiedNotification(this);
        }
    }

    @Override
    public void onTargetChanged(Target target) {
        HuntPlaceFragment.changePlace(this, target);
        HuntActionFragment.changeTarget(target);
    }

    @Override
    public void onTargetAdded() {
        if (mHuntActionFragment != null) {
            mHuntActionFragment.updateTargetMarks();
        }
    }

    private void resolveButtonStates(Target.TargetState state) {
        resolveButtonState(acceptFab, state.canAccept());
        resolveButtonState(openUpFab, state.canOpenUp());
        resolveButtonState(rejectFab, state.canReject());
        resolveButtonState(cameraFab, state.canLock());
        resolveButtonState(evaluateFab, state.canComplete());
    }

    private void resolveButtonState(FloatingActionButton fab, boolean show) {
        if (show) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    @Override
    public void onItemSelected(Target.TargetState state) {
        if (mViewPager.getCurrentItem() == 0) {
            resolveButtonStates(state);

            rotateFab.show();
            sortFab.hide();
        }
        if (mHuntActionFragment != null) {
            mHuntActionFragment.updateTargetMarks();
        }
    }

    @Override
    public void onRequestNextPage() {
        acceptFab.setVisibility(View.GONE);
        rejectFab.setVisibility(View.GONE);
        rotateFab.setVisibility(View.GONE);
        evaluateFab.setVisibility(View.GONE);
        sortFab.setVisibility(View.GONE);
        cameraFab.setVisibility(View.GONE);
        openUpFab.setVisibility(View.GONE);

        /* Go to the page with place information */
        mViewPager.setCurrentItem(1, true);
    }

    @Override
    public void onItemUnselected() {
        if (mViewPager.getCurrentItem() == 0) {
            acceptFab.setVisibility(View.GONE);
            openUpFab.setVisibility(View.GONE);
            rejectFab.setVisibility(View.GONE);
            cameraFab.setVisibility(View.GONE);

            evaluateFab.show();
            rotateFab.show();
            sortFab.show();
        }
    }

    @Override
    public void onSelectionChanged(int photoIndex) {
        HuntOfferFragment.changeSelectedTargetPhoto(photoIndex);
    }

    private void handleOHException(OHException exception) {
        if (exception.getExType() == OHException.EXType.SERIALIZATION_INCOMPATIBLE) {
            Toast.makeText(HuntActivity.this, getString(R.string.ohex_serialization),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(HuntActivity.this, getString(R.string.ohex_general) + " " + exception,
                    Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, getString(R.string.ohex_general) + exception);
    }

    private void handleNullResponse() {
        Log.e(TAG, "Problem on client side");
        Toast.makeText(HuntActivity.this, getString(R.string.server_unreachable_error),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
        /* Problem on the server side */
        if (ohex != null) {
            handleOHException(ohex);
            return;
        }
        /* Problem on the client side */
        if (response == null) {
            handleNullResponse();
            return;
        }
        /* Success */
        if (request instanceof RejectPlaceRequest) {
            String placeID = ((RejectPlaceRequest) request).getPlaceID();
            int cost = ((RejectPlaceRequest) request).getLoss();
            Target target = HuntOfferFragment.getTargetByID(placeID);
            if (target == null) {
                Log.e(TAG, "Rejected target with ID " + placeID + " could not be found in offer!");
                return;
            } else {
                /* Only rejected opened targets can open new ones */
                if (target.getState() == Target.TargetState.OPENED) {
                    HuntOfferFragment.randomlyOpenTarget();
                }
                target.setRejectLoss(cost);
                HuntOfferFragment.restateTarget(target, Target.TargetState.REJECTED);
                if (mHuntActionFragment != null) {
                    mHuntActionFragment.updateTargetMarks();
                }
            }
            SharedDataManager.setPlayer(HuntActivity.this, response.player);
            Log.d(TAG, "Rejected target was written to the database. Target ID: " + placeID);
        } else if (data instanceof String && request instanceof CompareRequest) {
            /* Request to evaluate similarity successfully finished */
            String placeID = (String) data;
            String photoReference = ((CompareRequest) request).getReferencePhoto().reference;

            Toast.makeText(HuntActivity.this, String.format(getString(R.string.similarity_is),
                    response.similarity * 100), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "response similarity: " + response.similarity);

            /* Retrieve the discovery gain and count the similarity gain */
            Target target = HuntOfferFragment.getTargetByID(placeID);
            if (target == null) {
                Log.e(TAG, "onResponseTaskCompleted(): target for placeID " + placeID + " is not available");
                return;
            }
            int discoveryGain = target.getDiscoveryGain();
            int similarityGain = mPointsManager.getTargetSimilarityGain(response.similarity);
            Log.d(TAG, "discoveryGain = " + discoveryGain + ", similarityGain = " + similarityGain);

            Request completeRequest = new CompleteTargetRequest(
                    SharedDataManager.getPlayer(this),
                    placeID,
                    photoReference,
                    discoveryGain,
                    similarityGain,
                    SharedDataManager.getHuntNumber(HuntActivity.this)
            );
            if (mHuntOfferFragment != null) {
                DialogFragment dialog = Wizard.getServerCommunicationDialog(mHuntOfferFragment.getActivity());
                ResponseTask task = new ResponseTask(dialog, placeID, HuntActivity.this);
                task.execute(completeRequest);
            }

            /* If the complete result fails, compare is not going to be done again */
            SharedDataManager.setRequestForTarget(this, completeRequest, placeID);

            /* Add target to the history of completed ones */
            target.setState(Target.TargetState.LOCKED);
            SharedDataManager.addTargetToHistory(this, target);
            SharedDataManager.addRequestToHistory(this, placeID, null);
        } else if (request instanceof CompleteTargetRequest) {
            /* Request to complete the evaluated target successfully finished (stored in database) */
            String placeID = ((CompleteTargetRequest) request).getPlaceID();
            int discoveryGain = ((CompleteTargetRequest) request).getDiscoveryGain();
            int similarityGain = ((CompleteTargetRequest) request).getSimilarityGain();
            Target target = HuntOfferFragment.getTargetByID(placeID);
            if (target != null) {
                target.setDiscoveryGain(discoveryGain);
                target.setSimilarityGain(similarityGain);
            }
            SharedDataManager.removeRequestForTarget(this, placeID);
            HuntOfferFragment.restateTarget(placeID, Target.TargetState.COMPLETED);
            if (mHuntActionFragment != null) {
                mHuntActionFragment.updateTargetMarks();
            }
            SharedDataManager.setPlayer(this, response.player);
            if (target != null) {
                target.setState(Target.TargetState.COMPLETED);
            }
            SharedDataManager.addTargetToHistory(this, target);
            SharedDataManager.addRequestToHistory(this, placeID, null);
            Wizard.targetCompletedDialog(this);
            Log.d(TAG, "Completion of target " + placeID + " was written to database.");
        }
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private String tabTitles[] = new String[] {
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
            if (position == 0) {
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
            return tabTitles[position];
        }

    }

}
