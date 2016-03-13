package layout;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.kappa_labs.ohunter.client.HuntActivity;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.TargetTileView;
import com.kappa_labs.ohunter.client.PlacesManager;
import com.kappa_labs.ohunter.client.TileAdapter;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import com.kappa_labs.ohunter.client.PageChangeAdapter;
import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.SharedDataManager;

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
//    private static final String SELECTED_GREEN_INDX_KEY

    private OnFragmentInteractionListener mListener;

    private ArrayList<Target> targets = new ArrayList<>();
    private TileAdapter mAdapter;

    private ProgressBar fetchingProgressBar;
    GridView offerGridView;

    private int selectedIndex;
    private int activatedIndex;


    public HuntOfferFragment() {
        /* Required empty public constructor */
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
        //TODO: ulozit do sharedmanageru selected
//        if (savedInstanceState != null) {
//            if (savedInstanceState.keySet().contains(SELECTED_GREEN_INDX_KEY)) {
//                selectedGreenIndex = savedInstanceState.getInt(SELECTED_GREEN_INDX_KEY);
//            }
//            if (savedInstanceState.keySet().contains(SELECTED_RED_INDX_KEY)) {
//                selectedRedIndex = savedInstanceState.getInt(SELECTED_RED_INDX_KEY);
//            }
//            if (savedInstanceState.keySet().contains(ACTIVATED_INDX_KEY)) {
//                activatedIndex = savedInstanceState.getInt(ACTIVATED_INDX_KEY);
//            }
//        }
    }

    private void updateValuesFromPreferences() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

//        outState.putInt(SELECTED_GREEN_INDX_KEY, selectedGreenIndex);
//        outState.putInt(SELECTED_RED_INDX_KEY, selectedRedIndex);
//        outState.putInt(ACTIVATED_INDX_KEY, activatedIndex);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        View view = inflater.inflate(R.layout.fragment_hunt_offer, container, false);

        fetchingProgressBar = (ProgressBar) view.findViewById(R.id.progressBar_fetching);
        fetchingProgressBar.setVisibility(View.GONE);
        offerGridView = (GridView) view.findViewById(R.id.gridView_offer);

        // TODO: nastavit pocet sloupcu GridView tak, aby velikosti dlazdic byly vhodne velke (ale v teto metode to nejde)
        mAdapter = new TileAdapter(getContext(), targets);
        offerGridView.setAdapter(mAdapter);

        /* This class will prepare the places - load from local files or retrieve them from Internet */
        PlacesManager manager = new PlacesManager(getContext(), new PlacesManager.PlacesManagerListener() {
            @Override
            public void onPreparationStarted() {
                fetchingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPreparationEnded() {
                fetchingProgressBar.setVisibility(View.GONE);

                /* No place is available */
                if (targets.isEmpty()) {
                    //TODO: zobrazit nejakou zpravu pro uzivatele
                    return;
                }

                /* If game already started, no division is needed */
                if (SharedDataManager.isHuntReady(getContext())) {
                    return;
                }

                //TODO: provadet jinde - ne po kazdem navstiveni tohoto fragmentu!
                /* Divide given places into two groups (green and red) */
                ArrayList<Integer> range = new ArrayList<>();
                for (int i = 0; i < targets.size(); i++) {
                    range.add(i);
                    targets.get(i).setState(Target.TargetState.REJECTED);
                }
                /* Randomly pick green places */
                Random random = new Random();
                int min = Math.min(targets.size(), SharedDataManager.DEFAULT_NUM_GREENS);
                while (min > 0) {
                    int index = random.nextInt(range.size());
                    targets.get(range.remove(index)).setState(Target.TargetState.ACCEPTED);
                    --min;
                }

                //TODO: provadet pravidelne?
                /* Sort places  */
                Collections.sort(targets, new Comparator<Target>() {
                    @Override
                    public int compare(Target lhs, Target rhs) {
                        return lhs.getState().compare(rhs.getState());
                    }
                });

                mAdapter.notifyDataSetChanged();

                SharedDataManager.initNewHunt(getContext(), true);
                SharedDataManager.setStartTime(getContext(), System.currentTimeMillis());
            }

            @Override
            public void onPlaceReady(Place place) {
                targets.add(new Target(place.getID()));
                mAdapter.notifyDataSetChanged();
            }
        }, SharedDataManager.getPlayer(getContext()), HuntActivity.radarPlaceIDs);
        /* Do not download data again if the game is already running */
        if (!SharedDataManager.isHuntReady(getContext())) {
            manager.preparePlaces();
        } else {
            //TODO: nacti z lokalu
        }

        /* Short click selects the tile */
        offerGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TargetTileView) {
                    TargetTileView tile = (TargetTileView) view;
                    selectedIndex = position;
                    for (Target iTile : targets) {
                        iTile.setHighlighted(false);
                    }
                    mAdapter.notifyDataSetChanged();
//                    tile.changeRotation();
                    tile.setHighlighted(true);
                    tile.update();

                    /* Notify the listener */
                    if (mListener != null) {
                        mListener.onItemSelected(tile.getPlace());
                        if (tile.isAccepted()) {
                            mListener.onGreenSelected();
                        } else if (tile.isRejected()) {
                            mListener.onRedSelected();
                        }
                    }
                }
            }
        });

        /* On long click, go to the information page about selected place */
        offerGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TargetTileView) {
                    TargetTileView tile = (TargetTileView) view;
                    selectedIndex = position;

                    /* Notify the listener and request to show the next page (with place information) */
                    if (mListener != null) {
                        mListener.onItemSelected(tile.getPlace());
                        if (tile.isAccepted()) {
                            mListener.onGreenSelected();
                        } else if (tile.isRejected()) {
                            mListener.onRedSelected();
                        }
                        mListener.onRequestNextPage();
                    }
                }

                return true;
            }
        });

        return view;
    }

    /**
     * Try to get the currently selected target. Return selected target if available, null otherwise.
     *
     * @return The selected target if available, null otherwise.
     */
    private Target getSelectedTarget() {
        if (targets != null && selectedIndex >= 0 && selectedIndex < targets.size()) {
            return targets.get(selectedIndex);
        }
        return null;
    }

    /**
     * Try to get the currently activated target. Return selected target if available, null otherwise.
     *
     * @return The activated target if available, null otherwise.
     */
    private Target getActivatedTarget() {
        if (targets != null && activatedIndex >= 0 && activatedIndex < targets.size()) {
            return targets.get(activatedIndex);
        }
        return null;
    }

    /**
     * Rotates currently selected target if possible, i.e. some target is selected.
     */
    public void rotateSelectedTile() {
        Target tile = getSelectedTarget();
        if (tile != null && mAdapter != null) {
            tile.changeRotation();
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Activates currently selected target if possible, i.e. target state is sufficient
     * and some target is selected. Deactivates every other activated tile.
     *
     * @return True on success, false otherwise.
     */
    public boolean activateSelectedTarget() {
        boolean isOk = false;
        Target tile = getSelectedTarget();
        if (tile != null) {
            for (Target iTile : targets) {
                iTile.deactivate();
            }
            isOk = tile.activate();
            activatedIndex = selectedIndex;
            SharedDataManager.saveActivatedPlaceID(getContext(), tile.getPlaceID());
            mAdapter.notifyDataSetChanged();
        }
        return isOk;
    }

    /**
     * Rejects currently selected target if possible, i.e. target state is sufficient
     * and some target is selected.
     *
     * @return True on success, false otherwise.
     */
    public boolean rejectSelectedTarget() {
        boolean isOk;
        Target target = getSelectedTarget();
        isOk = target != null && target.reject();
        mAdapter.notifyDataSetChanged();

        return isOk;
    }

    /**
     * Accepts currently selected target if possible, i.e. target state is sufficient
     * and some target is selected.
     *
     * @return True on success, false otherwise.
     */
    public boolean acceptSelectedTarget() {
        boolean isOk;
        Target target = getSelectedTarget();
        isOk = target != null && target.accept();
        mAdapter.notifyDataSetChanged();

        return isOk;
    }

    @Override
    public void onPageSelected() {
        if (mListener == null) {
            return;
        }

//        if (SharedDataManager.getSelectedPlaceID(getContext()) != null) {
//            mListener.onItemSelected();
//        }
        Target target = getSelectedTarget();
        if (target != null) {
            if (target.isAccepted()) {
                mListener.onGreenSelected();
            } else if (target.isRejected()) {
                mListener.onRedSelected();
            }
        }
    }

//    /**
//     * Check if activated/hunted target exists.
//     *
//     * @return True if activated/hunted target exists, false otherwise.
//     */
//    public boolean hasActivatedTarget() {
//        return getActivatedTarget() != null;
//    }

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
        void onGreenSelected();
        void onRedSelected();
        void onRequestNextPage();
    }

}
