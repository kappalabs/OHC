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

    private static final String TAG = "HuntOfferFragment";

    private static final String PARAM_GREENS_KEY = "param_green_key";
    private static final String PARAM_RED_KEY = "param_red_key";
    private static final String SELECTED_GREEN_INDX_KEY = "selected_green_indx_key";
    private static final String SELECTED_RED_INDX_KEY = "selected_red_indx_key";
    private static final String ACTIVATED_INDX_KEY = "activated_indx_key";

    private static final int SELECTED_GREEN_COLOR = Color.GREEN;
    private static final int SELECTED_RED_COLOR = Color.RED;
    private static final int ACTIVATED_PLACE_COLOR = Color.YELLOW;
    private static final int UNSELECTED_PLACE_COLOR = Color.TRANSPARENT;

    private static final int UNKNOWN_INDEX = -1;

    public static ArrayList<Place> mParamGreen;
    public static ArrayList<Place> mParamRed;

    private ArrayAdapter<Place> mGreenAdapter;
    private ArrayAdapter<Place> mRedAdapter;

    private OnFragmentInteractionListener mListener;

    private ListView hmenuGreenListview;
    private ListView hmenuRedListview;

    private int selectedGreenIndex = UNKNOWN_INDEX;
    private int selectedRedIndex = UNKNOWN_INDEX;
    private int activatedIndex = UNKNOWN_INDEX;


    public HuntOfferFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of this fragment.
     *
     * @return A new instance of fragment HuntOfferFragment.
     */
    public static HuntOfferFragment newInstance() {
        return new HuntOfferFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SELECTED_GREEN_INDX_KEY)) {
                selectedGreenIndex = savedInstanceState.getInt(SELECTED_GREEN_INDX_KEY);
            }
            if (savedInstanceState.keySet().contains(SELECTED_RED_INDX_KEY)) {
                selectedRedIndex = savedInstanceState.getInt(SELECTED_RED_INDX_KEY);
            }
            if (savedInstanceState.keySet().contains(ACTIVATED_INDX_KEY)) {
                activatedIndex = savedInstanceState.getInt(ACTIVATED_INDX_KEY);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_GREEN_INDX_KEY, selectedGreenIndex);
        outState.putInt(SELECTED_RED_INDX_KEY, selectedRedIndex);
        outState.putInt(ACTIVATED_INDX_KEY, activatedIndex);

        super.onSaveInstanceState(outState);
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
                /* Obarveni polozky */
                selectedGreenIndex = position;
                selectedRedIndex = UNKNOWN_INDEX;
                updateHighlights();

                /* Ohlaseni udalosti */
                if (mListener != null) {
                    mListener.onItemSelected(mParamGreen.get(position));
                }
            }
        });
        hmenuGreenListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /* Odbarveni polozky */
                if (selectedGreenIndex == position) {
                    selectedGreenIndex = UNKNOWN_INDEX;
                } else if (position < selectedGreenIndex) {
                    --selectedGreenIndex;
                }
                if (activatedIndex == position) {
                    activatedIndex = UNKNOWN_INDEX;
                } else if (position < activatedIndex) {
                    --activatedIndex;
                }
                updateHighlights();

                /* Ohlaseni udalosti a presunuti polozky do vedlejsiho sloupce */
                if (mListener != null) {
                    if (selectedGreenIndex == UNKNOWN_INDEX) {
                        mListener.onItemUnselected();
                    } else {
                        mListener.onItemSelected(mParamGreen.get(selectedGreenIndex));
                    }
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
                /* Obarveni polozky */
                selectedGreenIndex = UNKNOWN_INDEX;
                selectedRedIndex = position;
                updateHighlights();

                /* Ohlaseni udalosti */
                if (mListener != null) {
                    mListener.onItemSelected(mParamRed.get(position));
                }
            }
        });
        hmenuRedListview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /* Odbarveni polozky */
                if (selectedRedIndex == position) {
                    selectedRedIndex = UNKNOWN_INDEX;
                } else if (position < selectedRedIndex) {
                    --selectedRedIndex;
                }
                if (activatedIndex == position) {
                    activatedIndex = UNKNOWN_INDEX;
                } else if (position < activatedIndex) {
                    --activatedIndex;
                }
                updateHighlights();

                /* Ohlaseni udalosti a presunuti polozky do vedlejsiho sloupce */
                if (mListener != null) {
                    if (selectedRedIndex == UNKNOWN_INDEX) {
                        mListener.onItemUnselected();
                    } else {
                        mListener.onItemSelected(mParamRed.get(selectedRedIndex));
                    }
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
                if (activatedIndex != UNKNOWN_INDEX && position == activatedIndex) {
                    view.setBackgroundColor(ACTIVATED_PLACE_COLOR);
                } else if (selectedGreenIndex != UNKNOWN_INDEX && position == selectedGreenIndex) {
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

        //NOTE: java.lang.NullPointerException: Attempt to invoke interface method 'int java.util.List.size()' on a null object reference
        // nastala po presunu na druhou page ze treti, OHunter byl nejspis aktivni, byla vyvolan spravce SM z odemykaci obrazovky...
        hmenuGreenListview.setAdapter(mGreenAdapter);
        hmenuRedListview.setAdapter(mRedAdapter);

        return view;
    }

    @Override
    public void onPageSelected() {
//        updatePlaceSelection();
    }

    /**
     * Check if activated/hunted Place object exists.
     *
     * @return True if activated/hunted Place object exists, false otherwise.
     */
    public boolean hasActivatedPlace() {
        return activatedIndex >= 0 && activatedIndex < mParamGreen.size();
    }

    /**
     * Get currently activated/hunted Place object.
     *
     * @return The currently activated/hunted Place object.
     */
    public Place getActivePlace() {
        return hasActivatedPlace() ? mParamGreen.get(activatedIndex) : null;
    }

    private void updateHighlights() {
        clearHighlighted();
        if (activatedIndex != UNKNOWN_INDEX) {
            getViewByPosition(activatedIndex, hmenuGreenListview).setBackgroundColor(ACTIVATED_PLACE_COLOR);
        }
        if (selectedGreenIndex != UNKNOWN_INDEX && selectedGreenIndex != activatedIndex) {
            getViewByPosition(selectedGreenIndex, hmenuGreenListview).setBackgroundColor(SELECTED_GREEN_COLOR);
        }
        if (selectedRedIndex != UNKNOWN_INDEX) {
            getViewByPosition(selectedRedIndex, hmenuRedListview).setBackgroundColor(SELECTED_RED_COLOR);
        }
    }

    private void clearHighlighted() {
        for (int i = 0; i < hmenuGreenListview.getChildCount(); i++) {
            hmenuGreenListview.getChildAt(i).setBackgroundColor(UNSELECTED_PLACE_COLOR);
        }
        for (int i = 0; i < hmenuRedListview.getChildCount(); i++) {
            hmenuRedListview.getChildAt(i).setBackgroundColor(UNSELECTED_PLACE_COLOR);
        }
    }

    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
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

    public void activateSelectedPlace() {
        if (selectedGreenIndex != UNKNOWN_INDEX) {
            activatedIndex = selectedGreenIndex;
        }
        updateHighlights();
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
