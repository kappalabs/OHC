package layout;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;

import client.ohunter.fojjta.cekuj.net.ohunter.PageChangeAdapter;
import client.ohunter.fojjta.cekuj.net.ohunter.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HuntOfferFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntOfferFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntOfferFragment extends Fragment implements PageChangeAdapter {

    private static final String PARAM_GREENS_KEY = "param_green_key";
    private static final String PARAM_RED_KEY = "param_red_key";
    private static final String SELECTED_GREEN_INDX_KEY = "selected_green_indx_key";
    private static final String SELECTED_RED_INDX_KEY = "selected_red_indx_key";

    private static final int SELECTED_GREEN_COLOR = Color.GREEN;
    private static final int SELECTED_RED_COLOR = Color.RED;
    private static final int ACTIVATED_PLACE_COLOR = Color.YELLOW;
    private static final int UNSELECTED_PLACE_COLOR = Color.TRANSPARENT;

    private static final int UNKNOWN_INDEX = -1;

    private ArrayList<Place> mParamGreen;
    private ArrayList<Place> mParamRed;

    private ArrayAdapter<Place> mGreenAdapter;
    private ArrayAdapter<Place> mRedAdapter;

    private OnFragmentInteractionListener mListener;

    private ListView hmenuGreenListview;
    private ListView hmenuRedListview;

    private int selectedGreenIndex = UNKNOWN_INDEX, selectedRedIndex = UNKNOWN_INDEX;

    private static final String TAG = "HuntOfferFragment";


    public HuntOfferFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of this fragment.
     *
     * @param green_places Green (selected) places in the ohunter-menu.
     * @param red_places Red (offer) places in the ohunter-menu.
     * @return A new instance of fragment HuntOfferFragment.
     */
    public static HuntOfferFragment newInstance(ArrayList<Place> green_places, ArrayList<Place> red_places) {
        HuntOfferFragment fragment = new HuntOfferFragment();
        Bundle args = new Bundle();
        args.putSerializable(PARAM_GREENS_KEY, green_places);
        args.putSerializable(PARAM_RED_KEY, red_places);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParamGreen = (ArrayList<Place>) getArguments().getSerializable(PARAM_GREENS_KEY);
            mParamRed = (ArrayList<Place>) getArguments().getSerializable(PARAM_RED_KEY);
        }
        if (savedInstanceState != null) {
            selectedGreenIndex = savedInstanceState.getInt(SELECTED_GREEN_INDX_KEY);
            selectedRedIndex = savedInstanceState.getInt(SELECTED_RED_INDX_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_hunt_offer, container, false);

        hmenuGreenListview = (ListView) view.findViewById(R.id.listView_hmenu_green);
        hmenuRedListview = (ListView) view.findViewById(R.id.listView_hmenu_red);

        /* GREEN ==================== */
        hmenuGreenListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(TAG, "green position: " + position);
                /* Obarveni polozky */
                clearHighlighted();
                view.setBackgroundColor(SELECTED_GREEN_COLOR);
                selectedGreenIndex = position;
                selectedRedIndex = UNKNOWN_INDEX;

                /* Ohlaseni udalosti */
                if (mListener != null) {
                    mListener.onItemSelected(mParamGreen.get(position));
                }
            }
        });
        hmenuGreenListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(TAG, "green long pos: " + position);
                /* Odbarveni polozky */
                clearHighlighted();
                selectedGreenIndex = UNKNOWN_INDEX;
                selectedRedIndex = UNKNOWN_INDEX;

                /* Ohlaseni udalosti a presunuti polozky do vedlejsiho sloupce */
                if (mListener != null) {
                    mListener.onItemUnselected();
                    Place removed = mParamGreen.get(position);
                    mGreenAdapter.remove(removed);
                    mListener.onGreenRejected(removed);
                    mRedAdapter.add(removed);
                    return true;
                }
                return false;
            }
        });

        /* RED ==================== */
        hmenuRedListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(TAG, "red position: " + position);
                /* Obarveni polozky */
                clearHighlighted();
                view.setBackgroundColor(SELECTED_RED_COLOR);
                selectedGreenIndex = UNKNOWN_INDEX;
                selectedRedIndex = position;

                /* Ohlaseni udalosti */
                if (mListener != null) {
                    mListener.onItemSelected(mParamRed.get(position));
                }
            }
        });
        hmenuRedListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(TAG, "red long pos: " + position);
                /* Odbarveni polozky */
                clearHighlighted();
                selectedGreenIndex = UNKNOWN_INDEX;
                selectedRedIndex = UNKNOWN_INDEX;

                /* Ohlaseni udalosti a presunuti polozky do vedlejsiho sloupce */
                if (mListener != null) {
                    mListener.onItemUnselected();
                    Place removed = mParamRed.get(position);
                    mRedAdapter.remove(removed);
                    mListener.onRedRejected(removed);
                    mGreenAdapter.add(removed);
                    return true;
                }
                return false;
            }
        });

        mGreenAdapter = new ArrayAdapter<Place>(hmenuGreenListview.getContext(),
                android.R.layout.simple_list_item_1, mParamGreen) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (selectedGreenIndex != UNKNOWN_INDEX && position == selectedGreenIndex) {
                    view.setBackgroundColor(SELECTED_GREEN_COLOR);
                } else {
                    view.setBackgroundColor(UNSELECTED_PLACE_COLOR);
                }
                return view;
            }
        };
        mRedAdapter = new ArrayAdapter<Place>(hmenuRedListview.getContext(),
                android.R.layout.simple_list_item_1, mParamRed) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (selectedRedIndex != UNKNOWN_INDEX && position == selectedRedIndex) {
                    view.setBackgroundColor(SELECTED_RED_COLOR);
                } else {
                    view.setBackgroundColor(UNSELECTED_PLACE_COLOR);
                }
                return view;
            }
        };

        hmenuGreenListview.setAdapter(mGreenAdapter);
        hmenuRedListview.setAdapter(mRedAdapter);

        return view;
    }

    @Override
    public void onPageSelected() {
//        updatePlaceSelection();
    }

    private void clearHighlighted() {
        for (int i = 0; i < hmenuGreenListview.getChildCount(); i++) {
            hmenuGreenListview.getChildAt(i).setBackgroundColor(UNSELECTED_PLACE_COLOR);
        }
        for (int i = 0; i < hmenuRedListview.getChildCount(); i++) {
            hmenuRedListview.getChildAt(i).setBackgroundColor(UNSELECTED_PLACE_COLOR);
        }
    }

//    private void updatePlaceSelection() {
//        if (mListener == null) {
//            return;
//        }
//        // TODO: zda se, ze neni nutne...
////        if (selectedGreenIndex != UNKNOWN_INDEX) {
////            mListener.onItemSelected(mParamGreen.get(selectedGreenIndex));
////        }
////        if (selectedRedIndex != UNKNOWN_INDEX) {
////            mListener.onItemSelected(mParamRed.get(selectedRedIndex));
////        }
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_GREEN_INDX_KEY, selectedGreenIndex);
        outState.putInt(SELECTED_RED_INDX_KEY, selectedRedIndex);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        void onItemSelected(Place place);
        void onItemUnselected();
        void onGreenRejected(Place place);
        void onRedRejected(Place place);
        void onRedAccepted(Place place);
    }
}
