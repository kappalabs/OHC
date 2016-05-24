package layout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.activities.DummyApplication;
import com.kappa_labs.ohunter.client.activities.HuntActivity;
import com.kappa_labs.ohunter.client.adapters.PageChangeAdapter;
import com.kappa_labs.ohunter.client.adapters.TileAdapter;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.PointsManager;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.TargetsManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.client.views.TargetTileView;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;

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

    private static OnFragmentInteractionListener mListener;

    private static List<Target> targets = new ArrayList<>();
    private static TileAdapter mAdapter;

    private static ProgressBar fetchingProgressBar;

    private static int selectedIndex = -1;
    private static boolean loadingTargets;
    private static TargetsManager manager;


    public HuntOfferFragment() {
        /* Required empty public constructor */
    }

    /**
     * Initiates private fields for a new game.
     */
    public static void initNewHunt() {
        selectedIndex = -1;
        loadingTargets = false;
        manager = null;
        for (Target target : targets) {
            Bitmap bitmap = target.getSelectedPhotoPreview();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                target.setSelectedPhoto(null);
            }
        }
        targets.clear();
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
        saveTargets(DummyApplication.getContext());

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
        offerGridView.setScrollbarFadingEnabled(true);
        offerGridView.setVerticalScrollBarEnabled(true);

        if (mAdapter == null) {
            mAdapter = new TileAdapter(DummyApplication.getContext(), targets);
        } else {
            mAdapter.connect(DummyApplication.getContext(), targets);
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
            fetchingProgressBar.clearAnimation();
            fetchingProgressBar.setVisibility(View.GONE);
        }
        updateSelection();
        mAdapter.notifyDataSetChanged();

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
        /* This class will prepare the places - load from local files or retrieve them from server */
        manager = new TargetsManager(new TargetsManager.PlacesManagerListener() {
            @Override
            public void onPreparationStarted() {
                if (fetchingProgressBar != null) {
                    fetchingProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPreparationEnded() {
                loadingTargets = false;
                if (fetchingProgressBar != null) {
                    fetchingProgressBar.clearAnimation();
                    fetchingProgressBar.setVisibility(View.GONE);
                }
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

                /* Increase the number of hunts */
                SharedDataManager.increaseHuntNumber(DummyApplication.getContext());

                /* Remove points from the player for starting a new area (hunt) */
                PointsManager manager = new PointsManager();
                manager.removePoints(manager.getBeginAreaCost());
                manager.updateInDatabase(DummyApplication.getContext(), new ResponseTask.OnResponseTaskCompleted() {
                    @Override
                    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
                        if (ohex != null) {
                            Wizard.informOHException(getContext(), ohex);
                            return;
                        }
                        if (response == null) {
                            Wizard.informNullResponse(getContext());
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

                        SharedDataManager.initNewHunt(DummyApplication.getContext(), true, System.currentTimeMillis());
                        Wizard.gameInitializedDialog(getContext());
                        saveTargets(DummyApplication.getContext());
                    }
                });
            }

            @Override
            public void onTargetReady(Target target) {
                targets.add(target);
                saveTargets(DummyApplication.getContext());
                mAdapter.notifyDataSetChanged();
                if (mListener != null) {
                    mListener.onTargetAdded();
                }
            }
        }, SharedDataManager.getPlayer(DummyApplication.getContext()), HuntActivity.radarPlaceIDs);
        /* Do not download data again if the game is already running */
        if (!SharedDataManager.isHuntReady(DummyApplication.getContext())) {
            manager.prepareTargets();
        } else {
            System.out.println("loading targets");
            Target[] loaded = SharedDataManager.loadTargets(DummyApplication.getContext());
            if (loaded != null) {
                for (Target target : loaded) {
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
            mAdapter.notifyDataSetChanged();
            loadingTargets = false;
            if (fetchingProgressBar != null) {
                fetchingProgressBar.clearAnimation();
                fetchingProgressBar.setVisibility(View.GONE);
            }
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
        Collections.shuffle(unavailables);
        while (numOpened < unavailables.size() && numOpened < amount) {
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
        if (targets != null) {
            SharedDataManager.saveTargets(context, targets.toArray(new Target[targets.size()]));
        } else {
            SharedDataManager.saveTargets(context, null);
        }
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
     * Selects a given target in the offer page if the target exists in the offer.
     *
     * @param selectedTarget The target to be selected.
     */
    public static void setSelectedTarget(Target selectedTarget) {
        int position = 0;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i) == selectedTarget) {
                position = i;
                break;
            }
        }
        if (selectedIndex == position) {
            /* Unselect */
            selectedTarget.setHighlighted(false);
            mAdapter.notifyDataSetChanged();

            selectedIndex = -1;

            /* Notify the listener */
            if (mListener != null) {
                mListener.onItemUnselected();
            }
        } else {
            /* Select the tile */
            selectedIndex = position;
            for (Target iTile : targets) {
                iTile.setHighlighted(false);
            }
            mAdapter.notifyDataSetChanged();
            selectedTarget.setHighlighted(true);

            /* Notify the listener */
            if (mListener != null) {
                mListener.onTargetChanged(selectedTarget);
                mListener.onItemSelected(selectedTarget.getState());
            }
        }
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
        if (!loadingTargets && fetchingProgressBar != null) {
            fetchingProgressBar.clearAnimation();
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
        if (mAdapter != null) {
            mAdapter.connect(DummyApplication.getContext(), targets);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (fetchingProgressBar != null) {
            fetchingProgressBar.clearAnimation();
            fetchingProgressBar.setVisibility(View.GONE);
            fetchingProgressBar = null;
        }
        mAdapter.disconnect();
        mListener = null;
    }

    /**
     * Interface which must be implemented by the parent activity.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Called when the target selection changes.
         *
         * @param target The new selected target.
         */
        void onTargetChanged(Target target);

        /**
         * Called when new target is added to the offer list.
         */
        void onTargetAdded();

        /**
         * Called when a target's tile is selected.
         *
         * @param targetState The state of selected target.
         */
        void onItemSelected(Target.TargetState targetState);

        /**
         * When next page should be shown (details for selected target are requested).
         */
        void onRequestNextPage();

        /**
         * When no target becomes selected.
         */
        void onItemUnselected();
    }

}
