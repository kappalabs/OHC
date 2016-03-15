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
import java.util.List;
import java.util.Random;

import com.kappa_labs.ohunter.client.PageChangeAdapter;
import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.SharedDataManager;

/**
 * A {@link Fragment} subclass to provide offer menu of possible targets to user.
 * Activities that contain this fragment must implement the
 * {@link HuntOfferFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntOfferFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntOfferFragment extends Fragment implements PageChangeAdapter {

//    private static final String TAG = "HuntOfferFragment";
    private static final String SELECTED_INDEX_KEY = "SELECTED_INDEX_KEY";
    private static final String ACTIVATED_INDEX_KEY = "ACTIVATED_INDEX_KEY";

    private OnFragmentInteractionListener mListener;

    private List<Target> targets = new ArrayList<>();
    private TileAdapter mAdapter;

    private ProgressBar fetchingProgressBar;

    private int selectedIndex = -1;
    private int activatedIndex = -1;


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

        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SELECTED_INDEX_KEY)) {
                selectedIndex = savedInstanceState.getInt(SELECTED_INDEX_KEY);
            }
            if (savedInstanceState.keySet().contains(ACTIVATED_INDEX_KEY)) {
                activatedIndex = savedInstanceState.getInt(ACTIVATED_INDEX_KEY);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_INDEX_KEY, selectedIndex);
        outState.putInt(ACTIVATED_INDEX_KEY, activatedIndex);
        SharedDataManager.saveTargets(getContext(), targets.toArray(new Target[targets.size()]));

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        View view = inflater.inflate(R.layout.fragment_hunt_offer, container, false);

        fetchingProgressBar = (ProgressBar) view.findViewById(R.id.progressBar_fetching);
        fetchingProgressBar.setVisibility(View.GONE);
        GridView offerGridView = (GridView) view.findViewById(R.id.gridView_offer);

        // TODO: nastavit pocet sloupcu GridView tak, aby velikosti dlazdic byly vhodne velke (ale v teto metode to nejde)
        mAdapter = new TileAdapter(getContext(), targets);
        offerGridView.setAdapter(mAdapter);

        /* This class will prepare the places - load from local files or retrieve them from Internet */
        final Target[] _targets = new Target[HuntActivity.radarPlaceIDs.size()];
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

                /* Divide given places into two groups (green and red) */
                List<Integer> range = new ArrayList<>();
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

                /* Sort places  */
                Collections.sort(targets);
                mAdapter.notifyDataSetChanged();

                SharedDataManager.initNewHunt(getContext(), true);
                SharedDataManager.setStartTime(getContext(), System.currentTimeMillis());
            }

            @Override
            public void onPlaceReady(Place place) {
                targets.add(new Target(place.getID()));
                SharedDataManager.saveTargets(getContext(), targets.toArray(_targets));
//                SharedDataManager.saveTargets(getContext(), targets.toArray(new Target[targets.size()]));
                mAdapter.notifyDataSetChanged();
            }
        }, SharedDataManager.getPlayer(getContext()), HuntActivity.radarPlaceIDs);
        /* Do not download data again if the game is already running */
        if (!SharedDataManager.isHuntReady(getContext())) {
            manager.preparePlaces();
        } else {
            Target[] loaded = SharedDataManager.loadTargets(getContext());
            if (loaded != null) {
                for (Target target : loaded) {
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
            mAdapter.notifyDataSetChanged();
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
                    tile.setHighlighted(true);
                    tile.update();

                    /* Notify the listener */
                    if (mListener != null) {
                        mListener.onPlaceSelected(tile.getPlace());
                        mListener.onItemSelected(tile.getState());
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
                    /* Change highlighted items */
                    for (Target iTile : targets) {
                        iTile.setHighlighted(false);
                    }
                    mAdapter.notifyDataSetChanged();
                    tile.setHighlighted(true);
                    tile.update();

                    /* Notify the listener and request to show the next page (with place information) */
                    if (mListener != null) {
                        mListener.onPlaceSelected(tile.getPlace());
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

//    /**
//     * Try to get the currently activated target. Return selected target if available, null otherwise.
//     *
//     * @return The activated target if available, null otherwise.
//     */
//    private Target getActivatedTarget() {
//        if (targets != null && activatedIndex >= 0 && activatedIndex < targets.size()) {
//            return targets.get(activatedIndex);
//        }
//        return null;
//    }

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
     * Updates the activated and selected place index from current targets order.
     */
    private void updateSelection() {
        selectedIndex = -1;
        activatedIndex = -1;
        boolean gotFirst = false;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).isHighlighted()) {
                selectedIndex = i;
                if (gotFirst) {
                    return;
                }
                gotFirst = true;
            }
            if (targets.get(i).isActivated()) {
                activatedIndex = i;
                if (gotFirst) {
                    return;
                }
                gotFirst = true;
            }
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
                if (iTile != tile) {
                    iTile.deactivate();
                }
            }
            isOk = tile.activate();
            if (isOk) {
                SharedDataManager.saveActivatedPlaceID(getContext(), tile.getPlaceID());
            }

            /* Rearrange the tiles */
            Collections.sort(targets);
            updateSelection();
            mAdapter.notifyDataSetChanged();

            /* Inform the listener about the change of state */
            if (mListener != null) {
                mListener.onItemSelected(tile.getState());
            }
        }
        return isOk;
    }

    public boolean deactivateSelectedTarget() {
        boolean isOk = false;
        Target tile = getSelectedTarget();
        if (tile != null) {
            isOk = tile.deactivate();
            if (isOk) {
                SharedDataManager.saveActivatedPlaceID(getContext(), null);
            }

            /* Rearrange the tiles */
            Collections.sort(targets);
            updateSelection();
            mAdapter.notifyDataSetChanged();

            /* Inform the listener about the change of state */
            if (mListener != null) {
                mListener.onItemSelected(tile.getState());
            }
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
        /* Rearrange the tiles */
        Collections.sort(targets);
        updateSelection();
        mAdapter.notifyDataSetChanged();

        /* Inform the listener about the change of state */
        if (mListener != null && isOk) {
            mListener.onItemSelected(target.getState());
        }

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
        /* Rearrange the tiles */
        Collections.sort(targets);
        updateSelection();
        mAdapter.notifyDataSetChanged();

        /* Inform the listener about the change of state */
        if (mListener != null && isOk) {
            mListener.onItemSelected(target.getState());
        }

        return isOk;
    }

    @Override
    public void onPageSelected() {
        if (mListener == null) {
            return;
        }

        Target target = getSelectedTarget();
        if (target != null) {
            mListener.onItemSelected(target.getState());
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
        void onPlaceSelected(Place place);
        void onItemSelected(Target.TargetState targetState);
        void onRequestNextPage();
    }

}
