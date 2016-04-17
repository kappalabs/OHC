package layout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.activities.HuntActivity;
import com.kappa_labs.ohunter.client.activities.MainActivity;
import com.kappa_labs.ohunter.client.adapters.PageChangeAdapter;
import com.kappa_labs.ohunter.client.adapters.TileAdapter;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.PlacesManager;
import com.kappa_labs.ohunter.client.utilities.PointsManager;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.client.views.TargetTileView;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A {@link Fragment} subclass to provide offer menu of possible targets to user.
 * Activities that contain this fragment must implement the
 * {@link HuntOfferFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntOfferFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntOfferFragment extends Fragment implements PageChangeAdapter {

    private static final String TAG = "HuntOfferFragment";

    private static OnFragmentInteractionListener mListener;

    private static List<Target> targets = new ArrayList<>();
    private static TileAdapter mAdapter;

    private ProgressBar fetchingProgressBar;

    private static int selectedIndex = -1;
    private static boolean loadingTargets;
    private static PlacesManager manager;


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
    public void onSaveInstanceState(Bundle outState) {
        SharedDataManager.saveTargets(getContext(), targets.toArray(new Target[targets.size()]));

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        View view = inflater.inflate(R.layout.fragment_hunt_offer, container, false);

        fetchingProgressBar = (ProgressBar) view.findViewById(R.id.progressBar_fetching);
        GridView offerGridView = (GridView) view.findViewById(R.id.gridView_offer);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            offerGridView.setNumColumns(SharedDataManager.getOfferColumnsPortrait(getContext()));
        } else {
            offerGridView.setNumColumns(SharedDataManager.getOfferColumnsLandscape(getContext()));
        }

        if (mAdapter == null) {
            mAdapter = new TileAdapter(getContext(), targets);
        }
        offerGridView.setAdapter(mAdapter);

        /* Targets are initiated only when no target is available */
        if (targets.isEmpty() && !loadingTargets) {
            loadingTargets = true;
            initTargets();
        }
        /* Show the progress of initializing targets on the offer screen */
        if (loadingTargets) {
            fetchingProgressBar.setVisibility(View.VISIBLE);
        } else {
            fetchingProgressBar.setVisibility(View.GONE);
        }
        updateSelection();

        /* Short click selects the tile */
        offerGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TargetTileView) {
                    TargetTileView tile = (TargetTileView) view;
                    if (selectedIndex != position) {
                        /* Select the tile */
                        selectedIndex = position;
                        for (Target iTile : targets) {
                            iTile.setHighlighted(false);
                        }
                        mAdapter.notifyDataSetChanged();
                        tile.setHighlighted(true);
                        tile.update();

                        /* Notify the listener */
                        if (mListener != null) {
                            mListener.onTargetChanged(tile.getTarget());
                            mListener.onItemSelected(tile.getState());
                        }
                    } else {
                        /* Unselect the tile */
                        selectedIndex = -1;
                        for (Target iTile : targets) {
                            iTile.setHighlighted(false);
                        }
                        mAdapter.notifyDataSetChanged();
                        if (mListener != null) {
                            mListener.onItemUnselected();
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
                    if (selectedIndex != position) {
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
//                            mListener.onTargetChanged(tile.getPlace());
//                            mListener.onTargetChanged(PlacesManager.getPlace(getContext(), tile.getPlaceID()));
                            mListener.onTargetChanged(tile.getTarget());
                        }
                    }
                    if (mListener != null) {
                        mListener.onRequestNextPage();
                    }
                }

                return true;
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Target target = getSelectedTarget();
        if (target != null) {
            mListener.onTargetChanged(target);
            mListener.onItemSelected(target.getState());
        } else if (!loadingTargets) {
            mListener.onItemUnselected();
        }
    }

    private void initTargets() {
        /* This class will prepare the places - load from local files or retrieve them from Internet */
        final Target[] _targets = new Target[HuntActivity.radarPlaceIDs.size()];
        manager = new PlacesManager(getContext(), new PlacesManager.PlacesManagerListener() {
            @Override
            public void onPreparationStarted() {
                fetchingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPreparationEnded() {
                loadingTargets = false;
                fetchingProgressBar.setVisibility(View.GONE);
                if (mListener != null) {
                    mListener.onItemUnselected();
                }

                /* No target is available */
                if (targets.isEmpty()) {
                    Wizard.noTargetAvailableDialog(getContext(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    });
                    return;
                }

                /* Remove points from the player for starting a new area (hunt) */
                PointsManager manager = MainActivity.getPointsManager();
                manager.removePoints(manager.getBeginAreaCost());
                manager.updateInDatabase(getContext(), new ResponseTask.OnResponseTaskCompleted() {
                    @Override
                    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
                        if (ohex != null) {
                            Toast.makeText(getContext(), getString(R.string.ohex_general) + " " + ohex,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, getString(R.string.ohex_general) + ohex);
                            return;
                        }
                        if (response == null) {
                            // TODO: 27.3.16 zkusit znovu, nebo vratit na predchozi aktivitu
                            Log.e(TAG, "Problem on client side when starting a new area");
                            Toast.makeText(getContext(), getString(R.string.server_unreachable_error),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        /* Divide given places into two groups (unavailable and deferred) */
                        List<Integer> range = new ArrayList<>();
                        for (int i = 0; i < targets.size(); i++) {
                            range.add(i);
                        }
                        /* Randomly pick few targets and open them up */
                        Random random = new Random();
                        int min = Math.min(targets.size(), SharedDataManager.DEFAULT_NUM_AVAILABLE);
                        while (min > 0) {
                            int index = random.nextInt(range.size());
                            targets.get(range.remove(index)).setState(Target.TargetState.OPENED);
                            --min;
                        }

                        /* Sort targets based on their state  */
                        Collections.sort(targets);
                        mAdapter.notifyDataSetChanged();

                        SharedDataManager.initNewHunt(getContext(), true, System.currentTimeMillis());
                        Wizard.gameInitializedDialog(getActivity());
                    }
                });
            }

            @Override
            public void onPlaceReady(Place place) {
                targets.add(new Target(place));
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
            loadingTargets = false;
            fetchingProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Cancels all the target download tasks.
     */
    public static void cancelDownloadTasks() {
        if (manager != null && loadingTargets) {
            manager.cancelTask();
            loadingTargets = false;
            manager = null;
        }
    }

    /**
     * Randomly selects given amount of unavailable targets and makes them available to accept.
     *
     * @param amount Amount of targets to open up.
     * @return Number of targets, that were opened up.
     */
    public static int randomlyOpenTargets(int amount) {
        List<Target> unavailables = new ArrayList<>();
        for (Target target : targets) {
            if (target.getState() == Target.TargetState.UNAVAILABLE) {
                unavailables.add(target);
            }
        }
        int numOpened = 0;
        while (numOpened < unavailables.size() && numOpened < amount) {
            Collections.shuffle(unavailables);
            unavailables.get(numOpened++).openUp();
        }
        return numOpened;
    }

    /**
     * Randomly selects unavailable target and makes it available to accept.
     *
     * @return True on success, false if no unavailable target is available.
     */
    public static boolean randomlyOpenTarget() {
        return randomlyOpenTargets(1) == 1;
    }

    /**
     * Gets all the targets of the current hunt.
     *
     * @return All the targets of the current hunt.
     */
    public static List<Target> getTargets() {
        return targets;
    }

    /**
     * Save targets list to local file.
     *
     * @param context Context of the caller.
     */
    public static void saveTargets(Context context) {
        SharedDataManager.saveTargets(context, targets.toArray(new Target[targets.size()]));
    }

    /**
     * Clear the list containing targets.
     */
    public static void clearTargets() {
        targets.clear();
    }

    /**
     * Try to get the currently selected target. Return selected target if available, null otherwise.
     *
     * @return The selected target if available, null otherwise.
     */
    public static Target getSelectedTarget() {
        if (selectedIndex >= 0 && selectedIndex < targets.size()) {
            return targets.get(selectedIndex);
        }
        return null;
    }

    /**
     * Gets the Place ID of currently selected target.
     *
     * @return The Place ID of currently selected target.
     */
    public static String getSelectedTargetPlaceID() {
        Target target = getSelectedTarget();
        if (target != null) {
            return target.getPlaceID();
        }
        return null;
    }

    /**
     * Sorts the targets according to their state importance.
     */
    public static void sortTargets() {
        Collections.sort(targets);
        updateSelection();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
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
     * Rotates currently selected target if possible, i.e. some target is selected.
     */
    public void rotateAllTiles() {
        if (mAdapter != null) {
            for (Target iTarget : targets) {
                iTarget.changeRotation();
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Updates the activated and selected place index from current targets order.
     */
    private static void updateSelection() {
        selectedIndex = -1;
        boolean gotFirst = false;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).isHighlighted()) {
                selectedIndex = i;
                if (gotFirst) {
                    return;
                }
                gotFirst = true;
            }
        }
    }

    /**
     * Finds target with given place ID.
     *
     * @param placeID Place ID of the target.
     * @return The target with given place ID, null if does not exist.
     */
    public static Target getTargetByID(String placeID) {
        for (Target target : targets) {
            if (Objects.equals(target.getPlaceID(), placeID)) {
                return target;
            }
        }
        return null;
    }

    /**
     * Tries to change the state of currently selected target. Returns true on success, false on fail.
     * Does nothing on fail.
     *
     * @param target The target, whose state will be changed.
     * @param state The new state to set.
     * @return True on success, false on fail.
     */
    public static boolean restateTarget(Target target, Target.TargetState state) {
        boolean isOk;
        isOk = target != null && target.changeState(state);
        updateSelection();
        mAdapter.notifyDataSetChanged();

        /* Inform the listener about the change of state */
        if (mListener != null && isOk) {
            mListener.onItemSelected(target.getState());
        }

        return isOk;
    }

    /**
     * Tries to change the state of a target specified by given place ID. Returns true on success, false on fail.
     * Does nothing on fail.
     *
     * @param placeID The place ID of a target, whose state will be changed.
     * @param state The new state to set.
     * @return True on success, false on fail.
     */
    public static boolean restateTarget(String placeID, Target.TargetState state) {
        return restateTarget(getTargetByID(placeID), state);
    }

    /**
     * Tries to change the state of currently selected target. Returns true on success, false on fail.
     * Does nothing on fail.
     *
     * @param state The new state to set.
     * @return True on success, false on fail.
     */
    public static boolean restateSelectedTarget(Target.TargetState state) {
        return restateTarget(getSelectedTarget(), state);
    }

    /**
     * Changes the background image of selected target tile to photo represented by given index.
     *
     * @param index Index of the photo to be set onto background.
     */
    public static void changeSelectedTargetPhoto(int index) {
        Target target = getSelectedTarget();
        if (target == null) {
            return;
        }
        target.setPhotoIndex(index);
    }

    @Override
    public void onPageSelected() {
        mAdapter.notifyDataSetChanged();
        if (!loadingTargets) {
            fetchingProgressBar.setVisibility(View.GONE);
        }
        if (mListener == null) {
            return;
        }

        Target target = getSelectedTarget();
        if (target != null) {
            mListener.onItemSelected(target.getState());
        } else {
            mListener.onItemUnselected();
        }
    }

    /**
     * Check if selected target exists.
     *
     * @return True if selected target exists, false otherwise.
     */
    public static boolean hasSelectedTarget() {
        return getSelectedTarget() != null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
        /* Resolves problems with rotation when async task is running*/
        setRetainInstance(true);
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
        void onTargetChanged(Target target);
        void onItemSelected(Target.TargetState targetState);
        void onRequestNextPage();
        void onItemUnselected();
    }

}
