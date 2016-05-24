package com.kappa_labs.ohunter.client.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.kappa_labs.ohunter.client.activities.DummyApplication;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.FillPlacesRequest;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Class for managing download and access to places. Places are cached to support faster access.
 * Download is done by asynchronous tasks for better UI smoothness.
 */
public class TargetsManager {

    private static final String TAG = "TargetsManager";

//    public static final int DEFAULT_WIDTH = 480;
//    public static final int DEFAULT_HEIGHT = 320;
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;

    /**
     * Default number of targets to be opened when a target is completed.
     */
    public static final int DEFAULT_NUM_OPENED = 2;
    /**
     * Default number of targets, which will be added to the total number of the acceptable when new
     * target is completed.
     */
    public static final int DEFAULT_INCREMENT_ACCEPTABLE = 1;
    /**
     * Desired number of targets, which should be downloaded.
     */
    public static final int DESIRED_NUMBER_OF_TARGETS = 30;

    private PlacesManagerListener mListener;
    private Player mPlayer;
    private List<String> placeIDs;
    private int availableCount;
    private int preparedCount;
    private ResponseTask[] mTasks;


    /**
     * Creates a new manager to retrieve targets from the server.
     *
     * @param listener The listener on this manager.
     * @param player Player requesting the targets.
     * @param placeIDs Place IDs of targets available for download.
     */
    public TargetsManager(PlacesManagerListener listener, Player player, List<String> placeIDs) {
        this.mListener = listener;
        this.mPlayer = player;
        this.placeIDs = placeIDs;
    }

    /**
     * Start downloading targets from the server.
     */
    public void prepareTargets() {
        mListener.onPreparationStarted();

        /* Photos of targets will be saved externally, they need to initialize the manager */
        PhotosManager.init();

        /* Randomize order of the given places */
        Collections.shuffle(placeIDs, new Random(System.nanoTime()));

        preparedCount = 0;
        availableCount = placeIDs.size();
        Log.d(TAG, "Available number of targets for download is " + availableCount + ".");
        mTasks = new ResponseTask[availableCount];

        prepareNextPlace();
    }

    private void prepareNextPlace() {
        /* No more available targets */
        if (availableCount-- == 0 || preparedCount >= DESIRED_NUMBER_OF_TARGETS) {
            mTasks = null;
            mListener.onPreparationEnded();
            return;
        }
        Target target;
        String placeID = placeIDs.get(availableCount);
        /* Check if place can be loaded from local file */
        if ((target = SharedDataManager.getTarget(DummyApplication.getContext(), placeID)) != null) {
            mListener.onTargetReady(target);
            if (++preparedCount >= DESIRED_NUMBER_OF_TARGETS) {
                mTasks = null;
                mListener.onPreparationEnded();
            }
            return;
        }
        /* Otherwise retrieve the place from server */
        Request request = new FillPlacesRequest(
                mPlayer,
                new String[]{placeID},
                SharedDataManager.getPreferredDaytime(DummyApplication.getContext()),
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT
        );
        ResponseTask task = new ResponseTask(null, this, new ResponseTask.OnResponseTaskCompleted() {
            @Override
            public void onResponseTaskCompleted(Request request, Response response, OHException ohException, Object data) {
                mTasks[preparedCount] = null;
                if (ohException == null && response != null && response.places != null && response.places.length > 0) {
                    Bitmap icon = BitmapFactory.decodeResource(DummyApplication.getContext().getResources(), android.R.drawable.ic_menu_compass);
                    Bitmap mutableIcon = icon;
                    if (!icon.isMutable()) {
                        mutableIcon = icon.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    /* Create Target object from retrieved Place */
                    Place retPlace = response.places[0];
                    Target retTarget = new Target(retPlace, mutableIcon);
                    /* Save the result locally */
                    SharedDataManager.addTarget(DummyApplication.getContext(), retTarget);
                    PhotosManager.addPhotosOfTarget(retPlace.getID(), retPlace.getPhotos());
                    retPlace.getPhotos().clear();
                    /* Let the listener do something with the new place */
                    mListener.onTargetReady(retTarget);
                    preparedCount++;
                } else if (ohException != null) {
                    Wizard.informOHException(DummyApplication.getContext(), ohException);
                }
                ((TargetsManager) data).prepareNextPlace();
            }
        });
        task.execute(request);
        mTasks[preparedCount] = task;
    }

    /**
     * Cancels all the task downloading the targets.
     */
    public void cancelTask() {
        if (mTasks != null) {
            for (ResponseTask task : mTasks) {
                if (task != null) {
                    task.cancel(true);
                }
            }
        }
    }

    /**
     * Interface for listener on targets manager.
     */
    public interface PlacesManagerListener {
        /**
         * Called when the preparation of targets starts.
         */
        void onPreparationStarted();

        /**
         * Called when all the possible targets are prepared.
         */
        void onPreparationEnded();

        /**
         * Called when a new target is prepared.
         *
         * @param target The new prepared target.
         */
        void onTargetReady(Target target);
    }

}
