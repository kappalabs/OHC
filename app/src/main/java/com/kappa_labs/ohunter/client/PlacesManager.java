package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.View;

import com.google.android.gms.location.places.Places;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.FillPlacesRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;

/**
 * Created by kappa on 9.3.16.
 */
public class PlacesManager {

    private String TAG = "PlacesManager";

    private Context mContext;
    private PlacesManagerListener mListener;
    private Player mPlayer;
    private ArrayList<String> placeIDs;
    /* Counts the number of pending tasks */
    private int mCounter;

    public PlacesManager(Context context, PlacesManagerListener listener, Player player, ArrayList<String> placeIDs) {
        this.mContext = context;
        this.mListener = listener;
        this.mPlayer = player;
        this.placeIDs = placeIDs;
    }

    public void preparePlaces() {
        mListener.onPreparationStarted();

        //TODO: nejak vyresit, asynctask ma pouze frontu pouze 128 pozadavku
        // redukce poctu mist
        while (placeIDs.size() > 30) {
            placeIDs.remove(0);
        }

        mCounter = placeIDs.size();
        Log.d(TAG, "mam "+mCounter+" mist k tasknuti");

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
                Log.d(TAG, "mcounter = "+mCounter);
                continue;
            }
            /* Otherwise retrieve the place from server */
            Request request = new FillPlacesRequest(
                    mPlayer, new String[]{placeID}, Photo.DAYTIME.DAY, 800, 480);
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
                            Log.d(TAG, "mcounter = "+mCounter);
                        }
                    },
                            null,
                            -1);
            responseTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
        }
    }

    public static Place getPlace(Context context, String placeID) {
        //TODO: cachovani do LRU!
        return SharedDataManager.getPlace(context, placeID);
    }

    public interface PlacesManagerListener {
        void onPreparationStarted();
        void onPreparationEnded();
        void onPlaceReady(Place place);
    }
}
