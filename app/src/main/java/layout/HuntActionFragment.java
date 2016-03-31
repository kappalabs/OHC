package layout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kappa_labs.ohunter.client.activities.HuntActivity;
import com.kappa_labs.ohunter.client.entities.Target;

import com.kappa_labs.ohunter.client.adapters.PageChangeAdapter;
import com.kappa_labs.ohunter.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Fragment} subclass to show map, information about targets and player position.
 * Camera activity is connected to this fragment, the photographic part of the game starts here.
 * Use the {@link HuntActionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntActionFragment extends Fragment implements OnMapReadyCallback, PageChangeAdapter {

    private static boolean targetReady;
    private static boolean zoomInvalidated = true;
    private static boolean infoInvalidated = true;

    private Location mLastLocation;
    private Circle mCircle;
    private SupportMapFragment fragment;
    private GoogleMap map;
    private static Marker playerMarker;
    private static Target target;
    private static List<Marker> targetMarks = new ArrayList<>();
    private static List<Target> targets = new ArrayList<>();

    private TextView targetLatitudeTextView;
    private TextView playerLatitudeTextView;
    private TextView targetLongitudeTextView;
    private TextView playerLongitudeTextView;
    private TextView distanceTextView;


    public HuntActionFragment() {
        /* Required empty public constructor */
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HuntActionFragment.
     */
    public static HuntActionFragment newInstance() {
        return new HuntActionFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hunt_action, container, false);

        targetLatitudeTextView = (TextView) view.findViewById(R.id.textView_target_latitude);
        targetLongitudeTextView = (TextView) view.findViewById(R.id.textView_target_longitude);
        playerLatitudeTextView = (TextView) view.findViewById(R.id.textView_player_latitude);
        playerLongitudeTextView = (TextView) view.findViewById(R.id.textView_player_longitude);
        distanceTextView = (TextView) view.findViewById(R.id.textView_distance);

        infoInvalidated = true;
        update();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        fragment = (SupportMapFragment) fm.findFragmentById(R.id.map_layout);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map_layout, fragment).commit();
        }
    }

    @Override
    public void onPageSelected() {
        infoInvalidated = true;
        zoomInvalidated = true;
        update();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map == null) {
            fragment.getMapAsync(this);
        }
        onPageSelected();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        zoomInvalidated = true;
        zoomToPlace();
        /* The fine location should be granted by HuntActivity, which needs the same permission */
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
    }

    private void update() {
        if (zoomInvalidated) {
            zoomToPlace();
        }
        if (infoInvalidated) {
            updateInformation();
            updatePlayerPin();
        }
        // TODO: 31.3.16 zoptimalizovat
        List<Target> targets = HuntOfferFragment.getTargets();
        List<Target> filter = new ArrayList<>();
        for (Target target : targets) {
            if (target.getState() == Target.TargetState.ACCEPTED) {
                filter.add(target);
            }
        }
        HuntActionFragment.targets = filter;
            updateTargetMarks();
    }

    private void zoomToPlace() {
        if (!targetReady || map == null) {
            return;
        }

        /* Remove the old highlighted area */
        if (mCircle != null) {
            mCircle.remove();
        }

        /* Add new active zone around the Place */
        LatLng ll = new LatLng(target.latitude, target.longitude);
        CircleOptions co = new CircleOptions()
                .center(ll)
                .radius(HuntActivity.RADIUS)
                .strokeColor(Color.argb(230, 230, 0, 0))
                .fillColor(Color.argb(80, 0, 0, 255));
        mCircle = map.addCircle(co);

        /* Add special marker for the position of the target */
        map.addMarker(new MarkerOptions()
                .position(new LatLng(target.latitude, target.longitude))
                .title(target.getName()));

        /* Move camera to the Place's position */
        float zoom = (float) (10f - Math.log(HuntActivity.RADIUS / 10000f) / Math.log(2f));
        zoom = Math.min(20, Math.max(zoom, 1));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .zoom(zoom)
                .target(ll)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        zoomInvalidated = false;
    }

    private String getTargetLatitude() {
        return targetReady ? String.format("%.6f", target.latitude) + "N" : "??N";
    }

    private String getTargetLongitude() {
        return targetReady ? String.format("%.6f", target.longitude) + "N" : "??N";
    }

    private String getPlayerLatitude() {
        return mLastLocation == null ? "??N" : String.format("%.6f", mLastLocation.getLatitude()) + "N";
    }

    private String getPlayerLongitude() {
        return mLastLocation == null ? "??E" : String.format("%.6f", mLastLocation.getLongitude()) + "E";
    }

    private String getTargetDistance() {
        if (mLastLocation == null || !targetReady) {
            return "??m";
        }
        Location placeLoc = new Location("unknown");
        placeLoc.setLatitude(target.latitude);
        placeLoc.setLongitude(target.longitude);
        return mLastLocation.distanceTo(placeLoc) + "m";
    }

    /**
     * Updates information in the textViews.
     */
    private void updateInformation() {
        targetLatitudeTextView.setText(getTargetLatitude());
        targetLongitudeTextView.setText(getTargetLongitude());
        playerLatitudeTextView.setText(getPlayerLatitude());
        playerLongitudeTextView.setText(getPlayerLongitude());
        distanceTextView.setText(getTargetDistance());

        infoInvalidated = false;
    }

    /**
     * Updates position of the player's pin on the map.
     */
    private void updatePlayerPin() {
        if (map != null && mLastLocation != null) {
            if (playerMarker != null) {
                playerMarker.remove();
            }
            MarkerOptions mo = new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            playerMarker = map.addMarker(mo);
        }
    }

    private void updateTargetMarks() {
        if (map == null) {
            return;
        }
        /* Remove previous marks */
        for (Marker marker : targetMarks) {
            marker.remove();
        }
        targetMarks.clear();
        /* Add new marks */
        for (Target target : targets) {
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(target.latitude, target.longitude))
                    .title(target.getName())
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass));
            targetMarks.add(map.addMarker(options));
        }
    }

    /**
     * Change the location of the player.
     *
     * @param location The new location of the player.
     */
    public void changeLocation(Location location) {
        mLastLocation = location;
        infoInvalidated = true;
        updateInformation();
    }

    /**
     * Change target, which this fragment should activate on the map.
     *
     * @param target The new target, which this fragment should activate on the map.
     */
    public static void changeTarget(Target target) {
        if (target != null) {
            targetReady = true;
            HuntActionFragment.target = target;
        } else {
            targetReady = false;
        }
        infoInvalidated = true;
        zoomInvalidated = true;
    }

}
