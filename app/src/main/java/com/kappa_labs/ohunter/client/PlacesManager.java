package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.entities.SImage;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.FillPlacesRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by kappa on 9.3.16.
 */
public class PlacesManager {

    private static final String TAG = "PlacesManager";

    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 200;

    private Context mContext;
    private PlacesManagerListener mListener;
    private Player mPlayer;
    private ArrayList<String> placeIDs;
    /* Counts the number of pending tasks */
    private int mCounter;
    private static LruCache<String, Place> mPlacesCache;
//    private static LruCache<String, Bitmap> mPreviewCache;

    public PlacesManager(Context context, PlacesManagerListener listener, Player player, ArrayList<String> placeIDs) {
        this.mContext = context;
        this.mListener = listener;
        this.mPlayer = player;
        this.placeIDs = placeIDs;

        initMemoryCache();
    }

    private  static void initMemoryCache() {
        /* Clear the cache */
        if (mPlacesCache != null) {
            mPlacesCache.evictAll();
        }

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        /* Use specified part of the available memory just for this memory cache */
        final int cacheSize = maxMemory / 4;

        Log.d(TAG, "available max memory = " + maxMemory + "; cacheSize = " + cacheSize);

        mPlacesCache = new LruCache<String, Place>(cacheSize) {
            @Override
            protected int sizeOf(String key, Place value) {
                /* Every place stores some number of photos, these are the most memory intensive objects */
                return DEFAULT_HEIGHT * DEFAULT_WIDTH * 4 / 1024 * value.getNumberOfPhotos();
            }
        };
//        mPreviewCache = new LruCache<String, Bitmap>(cacheSize) {
//            @Override
//            protected int sizeOf(String key, Bitmap value) {
//                return super.sizeOf(key, value);
//            }
//        };
    }

    public void preparePlaces() {
        mListener.onPreparationStarted();

        /* Randomize order of the given places */
        Collections.shuffle(placeIDs, new Random(System.nanoTime()));

        //TODO: nejak vyresit, asynctask ma pouze frontu pouze 128 pozadavku
        // redukce poctu mist
        while (placeIDs.size() > 30) {
            placeIDs.remove(0);
        }

        mCounter = placeIDs.size();
        Log.d(TAG, "mam " + mCounter + " mist k tasknuti");

        /* No Places are available */
        if (mCounter == 0) {
            mListener.onPreparationEnded();
            return;
        }

        Place place;
        for (String placeID : placeIDs) {
            /* Check if place can be loaded from local file */
            if ((place = SharedDataManager.getPlace(mContext, placeID)) != null) {
                mListener.onPlaceReady(place);
                if (--mCounter == 0) {
                    mListener.onPreparationEnded();
                }
//                Log.d(TAG, "mcounter = "+mCounter);
                continue;
            }
            /* Otherwise retrieve the place from server */
            Request request = new FillPlacesRequest(
                    mPlayer, new String[]{placeID}, Photo.DAYTIME.DAY, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            Utils.RetrieveResponseTask responseTask =
                    Utils.getInstance().new RetrieveResponseTask(new Utils.OnResponseTaskCompleted() {
                        @Override
                        public void onResponseTaskCompleted(Response response, OHException ohex, int code) {
                            if (ohex == null && response != null && response.places != null && response.places.length > 0) {
                                /* Save the result locally */
                                SharedDataManager.addPlace(mContext, response.places[0]);
                                /* Let the listener do something with the new place */
                                mListener.onPlaceReady(response.places[0]);
                            } else if (ohex != null) {
                                //TODO: zkontroluj ohex zpravu a pripadne opakuj request
                            } else {
                                //remove
                            }
                            if (--mCounter == 0) {
                                mListener.onPreparationEnded();
                            }
//                            Log.d(TAG, "mcounter = "+mCounter);
                        }
                    },
                            null,
                            -1);
            responseTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
        }
    }

    public static Place getPlace(Context context, String placeID) {
        if (mPlacesCache == null) {
            initMemoryCache();
        }
        Place place = mPlacesCache.get(placeID);
        if (place == null) {
            place = SharedDataManager.getPlace(context, placeID);
            if (place != null) {
                mPlacesCache.put(placeID, place);
            }
        }
        return place;
    }

    //TODO: nebude se muset nacitat cele misto, pouze jedna preview fotka
//    public static Bitmap getPreview(Context context, TargetTileView view) {
//        if (mPreviewCache == null) {
//            initMemoryCache();
//        }
//        Bitmap bitmap = mPreviewCache.get(view.getPlaceID());
//        if (bitmap == null) {
//            Log.d(TAG, "ctu ze souboru, potom nactu do LRU...");
//            Place place = SharedDataManager.getPlace(context, view.getPlaceID());
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
        void onPlaceReady(Place place);
    }
}
