package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Class managing access to shared values from SharedPreferences and private files
 * among all the Activities.
 */
public class SharedDataManager {

    private static final String TAG = "PreferencesManager";
    private static final String SHARED_DATA_FILENAME = "SHARED_DATA_FILENAME";
    private static final String PLAYER_FILENAME = "PLAYER_FILENAME";
    private static final String ACTIVE_PLACE_FILENAME = "ACTIVE_PLACE_FILENAME";
    private static final String PLACE_FILENAME = "PLACE_FILENAME";

    private static SharedPreferences mPreferences;
    private static Player mPlayer;
//    private static Place mActivePlace;
    private static ArrayList<String> mPlacesIDs = new ArrayList<>();
    public static ArrayList<String> greenIDs, redIDs;


    private SharedDataManager() { /* Non-instantiable class */ }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = context.getSharedPreferences(SHARED_DATA_FILENAME, Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    private static boolean writeObject(Context context, Object object, String filename) {
        FileOutputStream outputStream = null;
        try {
            /* Try to write object to file */
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(object);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot write the object to file\'" + filename + "\': " + e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close the file\'" + filename + "\': " + e);
                }
            }
        }
        return false;
    }

    private static Object readObject(Context context, String filename) {
        FileInputStream inputStream = null;
        try {
            /* Try to read object from file */
            inputStream = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            return ois.readObject();
        } catch (Exception e) {
            /* File is unavailable */
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close the file\'" + filename + "\': " + e);
                }
            }
        }
    }

    /**
     * Get currently set Player. If possible, use cashed Player object, otherwise load it from local
     * private file.
     *
     * @param context Context of the caller.
     * @return The currently set Player object, null if player is not set or cannot be read.
     */
    public static Player getPlayer(Context context) {
        /* If possible, return cashed Player object */
        if (mPlayer != null) {
            return mPlayer;
        }
        /* Otherwise try to read the object from file */
        Object object = readObject(context, PLAYER_FILENAME);
        if (object != null && object instanceof Player) {
            mPlayer = (Player) object;
        }
        return mPlayer;
    }

    /**
     * Save the given player to private local file.
     *
     * @param context Context of the caller.
     * @param player Player to be saved/set.
     * @return True if save was successful, false otherwise.
     */
    public static boolean setPlayer(Context context, Player player) {
        mPlayer = player;
        return writeObject(context, player, PLAYER_FILENAME);
    }

    public static Place getPlace(Context context, String placeID) {
        //TODO: nejak cashovat?
//        /* If possible, return cashed Place object */
//        if (mPlayer != null) {
//            return mPlayer;
//        }
        /* Otherwise try to read the object from file */
        Object object = readObject(context, placeID + "/" + PLACE_FILENAME);
        if (object != null && object instanceof Place) {
            return (Place) object;
        }
        return null;
    }

    public static boolean addPlace(Context context, Place place) {
        mPlacesIDs.add(place.getID());
        return writeObject(context, place, place.getID() + "/" + PLACE_FILENAME);
    }

    public static Place getActivePlace(Context context) {
//        if (mActivePlace != null) {
//            return mActivePlace;
//        }
//        /* Otherwise try to read the object from file */
//        Object object = readObject(context, ACTIVE_PLACE_FILENAME);
//        if (object != null && object instanceof Place) {
//            mActivePlace = (Place) object;
//        }
//        return mActivePlace;
        return null;
    }

    public static boolean setActivePlace(Context context, Place place) {
        //TODO:
        return false;
    }

    public static boolean addBitmapToPlace(Context context, Place place, Bitmap photo) {
        writeObject(context, photo,
                place.getID() + "/" + ((Long)(System.currentTimeMillis()/1000)).toString() + ".jpg");
        return false;
    }

    public static ArrayList<Bitmap> getBitmapsForPlace(Context context, Place place) {
        ArrayList<Bitmap> photos = new ArrayList<>();
//        writeObject(context, photo,
//                place.getID() + "/" + ((Long) (System.currentTimeMillis() / 1000)).toString() + ".jpg");
        return photos;
    }

}
