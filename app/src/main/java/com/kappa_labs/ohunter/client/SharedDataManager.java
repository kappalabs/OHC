package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class managing access to shared values from SharedPreferences and private files
 * among all the Activities.
 */
public class SharedDataManager {

    public static final String TAG = "PreferencesManager";

    /**
     * Default number of green places in the initial offer.
     */
    public static final int DEFAULT_NUM_GREENS = 6;

    private static final String HUNT_READY_KEY = "hunt_ready";
    private static final String START_TIME_KEY = "start_time";
    private static final String PHOTOS_SET_KEY = "photos_set";
    private static final String REQUESTS_SET_KEY = "requests_set";
    private static final String LAST_NICKNAME_KEY = "last_nickname";
    private static final String LAST_SERVER_KEY = "last_server";
    private static final String SERVER_HISTORY_KEY = "server_history";

    private static final String PHOTO_PREFIX = "photo_";
    private static final String PLACE_PREFIX = "place_";

    private static final String SHARED_PREFERENCES_FILENAME = "main_preferences";
    private static final String PLACE_SHARED_DATA_FILENAME = "place_preferences_";
    private static final String PLAYER_FILENAME = "player";
    private static final String PLACE_FILENAME = "place";
    private static final String TARGETS_FILENAME = "targets";
    private static final String COMPARE_REQUEST_FILENAME = "compare_request";

    private static SharedPreferences mPreferences;
    private static Player mPlayer;
    private static Boolean huntReady;
    private static Long startTime;


    private SharedDataManager() { /* Non-instantiable class */ }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    /**
     * Gets SharedPreferences specific for given place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place.
     * @return The SharedPreferences specific for given place ID.
     */
    private static SharedPreferences getPreferencesForPlace(Context context, String placeID) {
        return context.getSharedPreferences(PLACE_SHARED_DATA_FILENAME + placeID, Context.MODE_PRIVATE);
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

    private static boolean removeObject(Context context, String filename, String directory) {
        try {
            File file;
            if (directory != null) {
                File dirFile = context.getDir(directory, Context.MODE_PRIVATE);
                file = new File(dirFile, filename);
                return file.delete();
            } else {
                return context.deleteFile(filename);
            }

        } catch (Exception e) {
            Log.e(TAG, "Cannot remove object: " + e);
            return false;
        }
    }

    /**
     * Gets the last used nickname from shared preferences.
     *
     * @param context Context of the caller.
     * @return The last used nickname from shared preferences.
     */
    public static String getLastNickname(Context context) {
        return getSharedPreferences(context).getString(LAST_NICKNAME_KEY, "");
    }

    /**
     * Sets the last used nickname to shared preferences.
     *
     * @param context Context of the caller.
     * @param nickname The last used nickname.
     */
    public static void setLastNickname(Context context, String nickname) {
        getSharedPreferences(context).edit().putString(LAST_NICKNAME_KEY, nickname).commit();
    }

    /**
     * Gets the last used server from shared preferences.
     *
     * @param context Context of the caller.
     * @return The last used server from shared preferences.
     */
    public static String getLastServer(Context context) {
        return getSharedPreferences(context).getString(LAST_SERVER_KEY, context.getString(R.string.server_address_template));
    }

    /**
     * Sets the last used server to shared preferences.
     *
     * @param context Context of the caller.
     * @param server The last used server.
     */
    public static void setLastServer(Context context, String server) {
        getSharedPreferences(context).edit().putString(LAST_SERVER_KEY, server).commit();
    }

    /**
     * Gets the history of used servers from shared preferences.
     *
     * @param context Context of the caller.
     * @return The history of used servers from shared preferences.
     */
    public static Set<String> getServerHistory(Context context) {
        return getSharedPreferences(context).getStringSet(SERVER_HISTORY_KEY, new HashSet<String>());
    }

    /**
     * Sets the history of used servers to shared preferences.
     *
     * @param context Context of the caller.
     * @param history The history of used servers.
     */
    public static void setServerHistory(Context context, Set<String> history) {
        getSharedPreferences(context).edit().putStringSet(SERVER_HISTORY_KEY, history).commit();
    }

    /**
     * Saves the start time of a hunt to shared preferences.
     *
     * @param context Context of the caller.
     * @param time The starting time of a hunt.
     */
    public static void setStartTime(Context context, long time) {
        if (startTime == null || startTime != time) {
            startTime = time;
            getSharedPreferences(context).edit().putLong(START_TIME_KEY, time).commit();
        }
    }

    /**
     * Gets the start time of a last hunt from shared preferences.
     *
     * @param context Context of the caller.
     * @return The starting time of a last hunt.
     */
    public static Long getStartTime(Context context) {
        if (startTime == null) {
            startTime = getSharedPreferences(context).getLong(START_TIME_KEY, 0);
            if (startTime <= 0) {
                startTime = null;
            }
        }
        return startTime;
    }

    public static synchronized void initNewHunt(Context context, boolean ready, long time) {
        //TODO: smazani predchozich nacachovanych souboru
//        saveActivatedPlaceID(context, null);
//        saveSelectedPlaceID(context, null);
        setStartTime(context, time);
        clearPendingRequests(context);
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

    /**
     * Loads serialized targets from the last hunt.
     *
     * @param context Context of the caller.
     * @return The de-serialized targets from the last hunt.
     */
    public static Target[] loadTargets(Context context) {
        Object object = readObject(context, TARGETS_FILENAME, null);
        if (object != null && object instanceof Target[]) {
            return (Target[]) object;
        }
        return null;
    }

    /**
     * Serializes the given targets to local file.
     *
     * @param context Context of the caller.
     * @param targets The targets to serialize.
     * @return True on success, false on fail.
     */
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

    /**
     * Converts place ID to name of directory, where data for this place should be stored.
     *
     * @param placeID Place ID to convert.
     * @return The place ID converted to name of directory.
     */
    private static String getDirectoryForPlace(String placeID) {
        return PLACE_PREFIX + placeID;
    }

    /**
     * Converts photo ID to name of file, where the photo should be stored.
     *
     * @param photoID Photo ID to convert.
     * @return The photo ID converted to name of file.
     */
    private static String getFilenameForPhoto(String photoID) {
        return PHOTO_PREFIX + photoID;
    }

    /**
     * Loads a place for given Place ID from local file.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place to load.
     * @return The loaded Place if possible, null otherwise.
     */
    public static Place getPlace(Context context, String placeID) {
        if (placeID == null) {
            return null;
        }
        /* Otherwise try to read the object from file */
        Object object = readObject(context, PLACE_FILENAME, getDirectoryForPlace(placeID));
        if (object != null && object instanceof Place) {
            return (Place) object;
        }
        return null;
    }

    /**
     * Creates a new directory for given place and stores the place into it for later use.
     *
     * @param context Context of the caller.
     * @param place Place to store (serialize) for later use.
     * @return True on success, false on fail.
     */
    public static boolean addPlace(Context context, Place place) {
        return writeObject(context, place, PLACE_FILENAME, getDirectoryForPlace(place.getID()));
    }

    /**
     * Associates given photo object with place specified by its Place ID, stores it locally.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of associated place.
     * @param photo Photo to be stored (serialized).
     * @param photoID Unique name of the photo.
     * @return True on success, false on fail.
     */
    public static boolean addPhotoOfPlace(Context context, String placeID, Photo photo, String photoID) {
        /* Save the photo */
        boolean isOk = writeObject(context, photo, getFilenameForPhoto(photoID), getDirectoryForPlace(placeID));
        /* Save the location of the new photo */
        SharedPreferences preferences = getPreferencesForPlace(context, placeID);
        Set<String> set = preferences.getStringSet(PHOTOS_SET_KEY, new HashSet<String>());
        set.add(getFilenameForPhoto(photoID));
        preferences.edit().putStringSet(PHOTOS_SET_KEY, set).apply();

        return isOk;
    }

    /**
     * Retrieves all photos saved as associated with given place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place, that has the associated photos.
     * @return All the photos saved as associated with given place ID.
     */
    public static Photo[] getPhotosOfPlace(Context context, String placeID) {
        /* Retrieve location of all photos for given place ID */
        SharedPreferences preferences = getPreferencesForPlace(context, placeID);
        Set<String> photoNames = new HashSet<>();
        photoNames = preferences.getStringSet(PHOTOS_SET_KEY, photoNames);

        /* Read the photos from those locations */
        List<Photo> photos = new ArrayList<>();
        for (String photoName : photoNames) {
            Object object = readObject(context, photoName, getDirectoryForPlace(placeID));
            if (object != null && object instanceof Photo) {
                photos.add((Photo) object);
            }
        }
        return photos.toArray(new Photo[photos.size()]);
    }

    /**
     * Stores the request to compare photos locally. Only one compare request can be stored for one place.
     *
     * @param context Context of the caller.
     * @param request Request that will be stored.
     * @param placeID Place ID of the place for which the request was made.
     * @return True on success, false on fail.
     */
    public static boolean setCompareRequestForPlace(Context context, Request request, String placeID) {
        boolean isOk = writeObject(context, request, COMPARE_REQUEST_FILENAME, getDirectoryForPlace(placeID));
        Set<String> requests = getSharedPreferences(context).getStringSet(REQUESTS_SET_KEY, new HashSet<String>());
        requests.add(placeID);
        getSharedPreferences(context).edit().putStringSet(REQUESTS_SET_KEY, requests).apply();

        return isOk;
    }

    /**
     * Gets the request to compare photos for place specified by given Place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place, of which the compare request is requested.
     * @return The compare request if exists, null otherwise or on error.
     */
    public static Request getCompareRequestForPlace(Context context, String placeID) {
        Object object = readObject(context, COMPARE_REQUEST_FILENAME, getDirectoryForPlace(placeID));
        if (object != null && object instanceof Request) {
            return (Request) object;
        }
        return null;
    }

    /**
     * Removes the request object for the place specified by its ID from local files.
     *
     * @param context Context of the caller.
     * @param placeID The Place ID of the place, that the request is associated to.
     */
    public static void removeCompareRequestForPlace(Context context, String placeID) {
        /* Delete the request object */
        removeObject(context, COMPARE_REQUEST_FILENAME, getDirectoryForPlace(placeID));
        /* Remove the request from list of all pending requests */
        Set<String> requests = getSharedPreferences(context).getStringSet(REQUESTS_SET_KEY, new HashSet<String>());
        requests.remove(placeID);
        getSharedPreferences(context).edit().putStringSet(REQUESTS_SET_KEY, requests).apply();
    }

    /**
     * Returns place IDs of places with pending compare requests for current hunt.
     *
     * @param context Context of the caller.
     * @return Place IDs of places with pending compare requests for current hunt.
     */
    public static Set<String> getPendingCompareRequestsIDs(Context context) {
        return getSharedPreferences(context).getStringSet(REQUESTS_SET_KEY, new HashSet<String>());
    }

    /**
     * Clears the list of pending requests.
     *
     * @param context Context of the caller.
     */
    public static void clearPendingRequests(Context context) {
        getSharedPreferences(context).edit().remove(REQUESTS_SET_KEY).apply();
    }

    /**
     * Removes all photos associated to the place specified by given Place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the place whose photos will be removed.
     */
    public static void clearPhotosOfPlace(Context context, String placeID) {
        /* Retrieve location of all photos for given place ID */
        SharedPreferences preferences = getPreferencesForPlace(context, placeID);
        Set<String> photoNames = new HashSet<>();
        photoNames = preferences.getStringSet(PHOTOS_SET_KEY, photoNames);

        /* Remove those photo files */
        for (String photoName : photoNames) {
            removeObject(context, photoName, getDirectoryForPlace(placeID));
        }
        /* Remove the list of photos from preferences of this place */
        preferences.edit().remove(PHOTOS_SET_KEY).apply();
    }

    /**
     * Removes value witch given key from the preferences.
     *
     * @param context Context of the caller.
     * @param key The key of the value, that should be removed.
     */
    public void remove(Context context, String key) {
        getSharedPreferences(context).edit().remove(key).commit();
    }

//    public boolean clear(Context context) {
//        return getSharedPreferences(context).edit().clear().commit();
//    }

}
