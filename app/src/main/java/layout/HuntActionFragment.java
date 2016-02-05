package layout;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kappa_labs.ohunter.lib.entities.Place;

import client.ohunter.fojjta.cekuj.net.ohunter.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HuntActionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntActionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntActionFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_PARAM_PLACE = "place_param";

    /* TODO: presunout jinam, radius v metrech */
    private static final double RADIUS = 150.0;

    private Location mLastLocation;
    private Circle mCircle;
    private Place mPlace;
    private SupportMapFragment fragment;
    private GoogleMap map;

    private TextView latitudeTextView, longitudeTextView;
    private TextView distanceTextView;

    private OnFragmentInteractionListener mListener;


    public HuntActionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param place Place being hunted.
     * @return A new instance of fragment HuntActionFragment.
     */
    public static HuntActionFragment newInstance(Place place) {
        HuntActionFragment fragment = new HuntActionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM_PLACE, place);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPlace = (Place) getArguments().getSerializable(ARG_PARAM_PLACE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hunt_action, container, false);

        latitudeTextView = (TextView) view.findViewById(R.id.textView_target_latitude);
        longitudeTextView = (TextView) view.findViewById(R.id.textView_target_longitude);
        distanceTextView = (TextView) view.findViewById(R.id.textView_distance);

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
    public void onResume() {
        super.onResume();
        if (map == null) {
            fragment.getMapAsync(this);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        /* TODO: zazoomovani na pozici hrace */
        if (mPlace != null) {
            if (mCircle != null) {
                mCircle.remove();
            }

            /* Add new area around the Place */
            LatLng ll = new LatLng(mPlace.latitude, mPlace.longitude);
            CircleOptions co = new CircleOptions()
                    .center(ll)
//                    .radius(mPlace.radius * 1000)
                    .radius(RADIUS)
                    .strokeColor(Color.argb(230, 230, 0, 0))
                    .fillColor(Color.argb(80, 0, 0, 255));
            mCircle = map.addCircle(co);

            map.addMarker(new MarkerOptions()
                    .position(new LatLng(mPlace.latitude, mPlace.longitude))
                    .title("Target"));

            /* Move camera to the Place's position */
            float zoom = (float) (10f - Math.log((double)RADIUS / 10000f) / Math.log(2f));
            zoom = Math.min(20, Math.max(zoom, 1));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .zoom(zoom)
                    .target(ll)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private String getMyLatitude() {
        return mLastLocation == null ? "N" : mLastLocation.getLatitude()+"N";
    }

    private String getMyLongitude() {
        return mLastLocation == null ? "E" : mLastLocation.getLongitude()+"E";
    }

    private String getTargetDistance() {
        if (mLastLocation == null || mPlace == null) {
            return "??";
        }
        Location placeLoc = new Location("unknown");
        placeLoc.setLatitude(mPlace.latitude);
        placeLoc.setLongitude(mPlace.longitude);
        return mLastLocation.distanceTo(placeLoc)+"m";
    }

    private void updateLocation() {
        if (latitudeTextView == null || longitudeTextView == null) {
            return;
        }
        latitudeTextView.setText(getMyLatitude());
        longitudeTextView.setText(getMyLongitude());
        distanceTextView.setText(getTargetDistance());
    }

    public void setLocation(Location location) {
        mLastLocation = location;
        updateLocation();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

    }
}
