package com.kappa_labs.ohunter.client.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.activities.PrepareHuntActivity;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.FillPlacesRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Class for managing download and access to places. Places are cached to support faster access.
 * Download is done by asynchronous tasks for better UI smoothness.
 */
public class TargetsManager {

    private static final String TAG = "TargetsManager";

    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 200;

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

    private Context mContext;
    private PlacesManagerListener mListener;
    private Player mPlayer;
    private List<String> placeIDs;
    private int availableCount;
    private int retrievedCount;
    private static LruCache<String, Target> mTargetsCache;
//    private static LruCache<String, Bitmap> mPreviewCache;
    private List<ResponseTask> mTasks;


    public TargetsManager(Context context, PlacesManagerListener listener, Player player, List<String> placeIDs) {
        this.mContext = context;
        this.mListener = listener;
        this.mPlayer = player;
        this.placeIDs = placeIDs;

        initMemoryCache();
    }

    private static void initMemoryCache() {
        /* Clear the cache */
        if (mTargetsCache != null) {
            mTargetsCache.evictAll();
        }

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        /* Use specified part of the available memory just for this memory cache */
        final int cacheSize = maxMemory / 4;

        Log.d(TAG, "available max memory = " + maxMemory + "; cacheSize = " + cacheSize);

        mTargetsCache = new LruCache<String, Target>(cacheSize) {
            @Override
            protected int sizeOf(String key, Target target) {
                /* Every place stores some number of photos, these are the most memory intensive objects */
                return DEFAULT_HEIGHT * DEFAULT_WIDTH * 4 / 1024 * target.getNumberOfPhotos();
            }
        };
//        mPreviewCache = new LruCache<String, Bitmap>(cacheSize) {
//            @Override
//            protected int sizeOf(String key, Bitmap value) {
//                return super.sizeOf(key, value);
//            }
//        };
    }

    /**
     * Start downloading targets from the server.
     */
    public void prepareTargets() {
        mListener.onPreparationStarted();
        mTasks = new ArrayList<>();

        /* Randomize order of the given places */
        Collections.shuffle(placeIDs, new Random(System.nanoTime()));

        retrievedCount = 0;
        availableCount = placeIDs.size();
        Log.d(TAG, "Available number of targets for download is " + availableCount + ".");

        prepareNextPlace();
    }

    private void prepareNextPlace() {
        /* No more available targets */
        if (availableCount-- == 0 || retrievedCount >= DESIRED_NUMBER_OF_TARGETS) {
            mListener.onPreparationEnded();
            return;
        }
        Target target;
        String placeID = placeIDs.get(availableCount);
        /* Check if place can be loaded from local file */
        if ((target = SharedDataManager.getTarget(mContext, placeID)) != null) {
            mListener.onPlaceReady(target);
            if (--availableCount == 0) {
                mListener.onPreparationEnded();
            }
            return;
        }
        /* Otherwise retrieve the place from server */
        Request request = new FillPlacesRequest(
                mPlayer,
                new String[]{placeID},
                PrepareHuntActivity.preferredDaytime,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT
        );
        ResponseTask task = new ResponseTask(null, this, new ResponseTask.OnResponseTaskCompleted() {
            @Override
            public void onResponseTaskCompleted(Request request, Response response, OHException ohException, Object data) {
                if (ohException == null && response != null && response.places != null && response.places.length > 0) {
                    Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(), android.R.drawable.ic_menu_compass);
                    Bitmap mutableIcon = icon;
                    if (!icon.isMutable()) {
                        mutableIcon = icon.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    /* Create Target object from retrieved Place */
                    Target retTarget = new Target(response.places[0], mutableIcon);
                    /* Save the result locally */
                    SharedDataManager.addTarget(mContext, retTarget);
                    /* Let the listener do something with the new place */
                    mListener.onPlaceReady(retTarget);
                    retrievedCount++;
                } else if (ohException != null) {
                    Log.e(TAG, ohException.getMessage());
                    Toast.makeText(mContext, ohException.getMessage(), Toast.LENGTH_SHORT).show();
                }
                ((TargetsManager) data).prepareNextPlace();
            }
        });
        task.execute(request);
        mTasks.add(task);
    }

    /**
     * Cancels all the task downloading the targets.
     */
    public void cancelTask() {
        for (ResponseTask task : mTasks) {
            task.cancel(true);
        }
    }

    /**
     * Gets the Target object for given placeID. Targets are cached, if possible.
     * If placeID is null, return null, otherwise get the Target from cache or local file.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place to retrieve.
     * @return The target for given Place ID or null if not available or ID is null.
     */
    public static Target getTarget(Context context, String placeID) {
        if (placeID == null) {
            return null;
        }
        if (mTargetsCache == null) {
            initMemoryCache();
        }
        Target target = mTargetsCache.get(placeID);
        if (target == null) {
            target = SharedDataManager.getTarget(context, placeID);
            if (target != null) {
                mTargetsCache.put(placeID, target);
            }
        }
        return target;
    }

    //TODO: nebude se muset nacitat cele misto, pouze jedna preview fotka
//    public static Bitmap getPreview(Context context, TargetTileView view) {
//        if (mPreviewCache == null) {
//            initMemoryCache();
//        }
//        Bitmap bitmap = mPreviewCache.get(view.getPlaceID());
//        if (bitmap == null) {
//            Log.d(TAG, "ctu ze souboru, potom nactu do LRU...");
//            Place place = SharedDataManager.getTarget(context, view.getPlaceID());
//
//            if (place != null && place.getNumberOfPhotos() > 0) {
//                BitmapWorkerTask task = new BitmapWorkerTask(view);
//                task.execute(place.getPhoto(0).sImage);
//            }
//        } else {
//            Log.d(TAG, "super, ctu z LRU yay!!!");
//        }
//        return bitmap;
//    }
//
//
//    private static class BitmapWorkerTask extends AsyncTask<SImage, Void, Bitmap> {
//
//        private TargetTileView mView;
//
//        public BitmapWorkerTask(TargetTileView view) {
//            this.mView = view;
//        }
//
//        @Override
//        protected Bitmap doInBackground(SImage... params) {
//            byte[] imgBytes = params[0].getBytes();
//            if (imgBytes == null) {
//                Log.e(TAG, "mam null simage, interesting...");
//                return null;
//            }
//            return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap bitmap) {
//            if (mView != null) {
//                mView.setPreview(cropBitmap(bitmap));
//            }
//        }
//
//        private Drawable cropBitmap(Bitmap bitmap) {
//            Bitmap cropped;
//            if (bitmap.getWidth() >= bitmap.getHeight()){
//                cropped = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0,
//                        bitmap.getHeight(), bitmap.getHeight()
//                );
//            } else{
//                cropped = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
//                        bitmap.getWidth(), bitmap.getWidth()
//                );
//            }
//            mPreviewCache.put(mView.getPlaceID(), cropped);
//            return new BitmapDrawable(mContext.getResources(), cropped);
//        }
//    }

    public interface PlacesManagerListener {
        void onPreparationStarted();
        void onPreparationEnded();
        void onPlaceReady(Target target);
    }
}
