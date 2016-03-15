package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Class managing access to shared values from SharedPreferences and private files
 * among all the Activities.
 */
public class SharedDataManager {

    /**
     * Default number of green places in the initial offer.
     */
    public static final int DEFAULT_NUM_GREENS = 6;
    private static final String SELECTED_PLACE_ID_KEY = "SELECTED_PLACE_ID_KEY";
    private static final String ACTIVATED_PLACE_ID_KEY = "ACTIVATED_PLACE_ID_KEY";
    private static final String HUNT_READY_KEY = "HUNT_READY_KEY";
    private static final String START_TIME_KEY = "START_TIME_KEY";

    private static final String TAG = "PreferencesManager";
    private static final String SHARED_DATA_FILENAME = "SHARED_DATA_FILENAME";
    private static final String PLAYER_FILENAME = "PLAYER_FILENAME";
//    private static final String ACTIVE_PLACE_FILENAME = "ACTIVE_PLACE_FILENAME";
    private static final String PLACE_FILENAME = "PLACE_FILENAME";
    private static final String TARGETS_FILENAME = "TARGETS_FILENAME";

    private static SharedPreferences mPreferences;
    private static Player mPlayer;
//    private static Place mActivePlace;
    private static ArrayList<String> mPlacesIDs = new ArrayList<>();
    public static ArrayList<String> greenIDs, redIDs;
    private static String selectedPlaceID, activatedPlaceID;
    private static Boolean huntReady;
    private static Long startTime;


    private SharedDataManager() { /* Non-instantiable class */ }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = context.getSharedPreferences(SHARED_DATA_FILENAME, Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    private static boolean writeObject(Context context, Object object, String filename, String directory) {
        FileOutputStream outputStream = null;
        try {
            File file;
            if (directory != null) {
                File dirFile = context.getDir(directory, Context.MODE_PRIVATE);
                file = new File(dirFile, filename);
                outputStream = new FileOutputStream(file);
            } else {
                outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            }

            /* Try to write object to file */
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(object);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot write the object to file \'" + filename + "\': " + e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close the file \'" + filename + "\': " + e);
                }
            }
        }
        return false;
    }

    private static Object readObject(Context context, String filename, String directory) {
        FileInputStream inputStream = null;
        try {
            File file;
            if (directory != null) {
                File dirFile = context.getDir(directory, Context.MODE_PRIVATE);
                file = new File(dirFile, filename);
                inputStream = new FileInputStream(file);
            } else {
                inputStream = context.openFileInput(filename);
            }

            /* Try to read object from file */
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            return ois.readObject();
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) {
                Log.e(TAG, "Cannot read object: " + e);
            }
            /* File is unavailable */
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close the file \'" + filename + "\': " + e);
                }
            }
        }
    }

    public static void setStartTime(Context context, long time) {
        if (startTime == null || startTime != time) {
            startTime = time;
            getSharedPreferences(context).edit().putLong(START_TIME_KEY, time).commit();
        }
    }

    public static Long getStartTime(Context context) {
        if (startTime == null) {
            startTime = getSharedPreferences(context).getLong(START_TIME_KEY, 0);
            if (startTime == 0) {
                startTime = null;
            }
        }
        return startTime;
    }

    public static void initNewHunt(Context context, boolean ready, long time) {
        //TODO: smazani predchozich nacachovanych souboru
        saveActivatedPlaceID(context, null);
        saveSelectedPlaceID(context, null);
        setStartTime(context, time);
        if (huntReady == null || huntReady != ready) {
            huntReady = ready;
            getSharedPreferences(context).edit().putBoolean(HUNT_READY_KEY, ready).commit();
        }
    }

    public static boolean isHuntReady(Context context) {
        if (huntReady == null) {
            huntReady = getSharedPreferences(context).getBoolean(HUNT_READY_KEY, false);
        }
        return huntReady;
    }

    public static void saveSelectedPlaceID(Context context, String placeID) {
        if (!Objects.equals(selectedPlaceID, placeID)) {
            selectedPlaceID = placeID;
            getSharedPreferences(context).edit().putString(SELECTED_PLACE_ID_KEY, placeID).commit();
        }
    }

    public static String getSelectedPlaceID(Context context) {
        if (selectedPlaceID == null) {
            selectedPlaceID = getSharedPreferences(context).getString(SELECTED_PLACE_ID_KEY, null);
        }
        return selectedPlaceID;
    }

    public static void saveActivatedPlaceID(Context context, String placeID) {
        if (!Objects.equals(activatedPlaceID, placeID)) {
            activatedPlaceID = placeID;
            getSharedPreferences(context).edit().putString(ACTIVATED_PLACE_ID_KEY, placeID).commit();
        }
    }

    public static String getActivatedPlaceID(Context context) {
        if (activatedPlaceID != null) {
            activatedPlaceID = getSharedPreferences(context).getString(ACTIVATED_PLACE_ID_KEY, "");
        }
        return activatedPlaceID;
    }

    public static Target[] loadTargets(Context context) {
        Object object = readObject(context, TARGETS_FILENAME, null);
        if (object != null && object instanceof Target[]) {
            return (Target[]) object;
        }
        return null;
    }

    public static boolean saveTargets(Context context, Target[] targets) {
        return writeObject(context, targets, TARGETS_FILENAME, null);
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
        Object object = readObject(context, PLAYER_FILENAME, null);
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
        return writeObject(context, player, PLAYER_FILENAME, null);
    }

    public static Place getPlace(Context context, String placeID) {
        //TODO: nejak cashovat?
//        /* If possible, return cashed Place object */
//        if (mPlayer != null) {
//            return mPlayer;
//        }
//        Log.d(TAG, "ctu place "+placeID+" z adresare \""+(placeID + "/" + PLACE_FILENAME)+"\"");
        /* Otherwise try to read the object from file */
        Object object = readObject(context, PLACE_FILENAME, placeID);
        if (object != null && object instanceof Place) {
            return (Place) object;
        }
        return null;
    }

    public static boolean addPlace(Context context, Place place) {
        mPlacesIDs.add(place.getID());
//        Log.d(TAG, "ukladam place " + place.getID() + " do adresare \"" + (place.getID() + "/" + PLACE_FILENAME) + "\"");
        return writeObject(context, place, PLACE_FILENAME, place.getID());
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
                place.getID(), ((Long)(System.currentTimeMillis()/1000)).toString() + ".jpg");
        return false;
    }

    public static ArrayList<Bitmap> getBitmapsForPlace(Context context, Place place) {
        ArrayList<Bitmap> photos = new ArrayList<>();
//        writeObject(context, photo,
//                place.getID() + "/" + ((Long) (System.currentTimeMillis() / 1000)).toString() + ".jpg");
        return photos;
    }

    public void remove(Context context, String key) {
        getSharedPreferences(context).edit().remove(key).commit();
    }

    public boolean clear(Context context) {
        return getSharedPreferences(context).edit().clear().commit();
    }

}
