package com.kappa_labs.ohunter.client.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class managing access to shared values from SharedPreferences and private files
 * among all the Activities.
 */
public class SharedDataManager {

    private static final String TAG = "SharedDataManager";

    /**
     * Default number of available targets in the initial offer.
     */
    public static final int DEFAULT_NUM_AVAILABLE = 6;
    /**
     * Default number of targets, which can be accepted in the initial offer.
     */
    public static final int DEFAULT_NUM_ACCEPTABLE = 4;

    private static final String HUNT_READY_KEY = "hunt_ready";
    private static final String START_TIME_KEY = "start_time";
    private static final String HUNT_NUMBER_KEY = "hunt_number";
    private static final String PHOTOS_SET_KEY = "photos_set";
    private static final String REQUESTS_SET_KEY = "requests_set";
    private static final String LAST_NICKNAME_KEY = "last_nickname";
    private static final String LAST_SERVER_KEY = "last_server";
    private static final String SERVER_HISTORY_KEY = "server_history";
    private static final String LAST_AREA_LONGITUDE_KEY = "last_area_longitude";
    private static final String LAST_AREA_LATITUDE_KEY = "last_area_latitude";
    private static final String LAST_AREA_RADIUS_KEY = "last_area_radius";
    private static final String NUM_ACCEPTABLE_KEY = "num_acceptable";
    private static final String PREFERRED_DAYTIME_KEY = "preferred_daytime";

    private static final String PHOTO_PREFIX = "photo_";
    private static final String TARGET_PREFIX = "target_";
    private static final String HISTORY_TARGET_PREFIX = "history_target_";
    private static final String HISTORY_REQUEST_PREFIX = "history_request_";

    private static final String TARGET_SHARED_DATA_FILENAME = "target_preferences_";
    private static final String PLAYER_FILENAME = "player";
    private static final String TARGET_FILENAME = "target";
    private static final String TARGETS_FILENAME = "targets";
    private static final String REQUEST_FILENAME = "request";

    private static final String HISTORY_DIRECTORY = "history";
    private final static String TARGET_PHOTOS_DIRECTORY = "photos_cache";

    private static SharedPreferences mPreferences;
    private static Player mPlayer;
    private static Boolean huntReady;
    private static Long startTime;
    private static Integer numAcceptable;


    private SharedDataManager() {
        /* Non-instantiable class */
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return mPreferences;
    }

    /**
     * Gets SharedPreferences specific for given place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target.
     * @return The SharedPreferences specific for given place ID.
     */
    private static SharedPreferences getPreferencesForTarget(Context context, String placeID) {
        return context.getSharedPreferences(TARGET_SHARED_DATA_FILENAME + placeID, Context.MODE_PRIVATE);
    }

    private static boolean writeObject(Context context, Object object, String filename, String directory) {
        FileOutputStream outputStream = null;
        try {
            File file;
            if (directory != null) {
                File subDir = new File(context.getFilesDir(), directory);
                if (!subDir.exists() && !subDir.mkdirs()) {
                    return false;
                }
                file = new File(subDir, filename);
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
                File subDir = new File(context.getFilesDir(), directory);
                file = new File(subDir, filename);
                inputStream = new FileInputStream(file);
            } else {
                inputStream = context.openFileInput(filename);
            }

            /* Try to read object from file */
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            return ois.readObject();
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) {
                Log.e(TAG, "Cannot read object \'" + filename + "\': " + e);
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
                File subDir = new File(context.getFilesDir(), directory);
                file = new File(subDir, filename);
                return file.delete();
            } else {
                return context.deleteFile(filename);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot remove object \'" + filename + "\': " + e);
            return false;
        }
    }

    private static boolean removeDirectory(Context context, String directory) {
        try {
            File subDir = new File(context.getFilesDir(), directory);
            File[] files = subDir.listFiles();
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            return subDir.delete();
        } catch (Exception e) {
            Log.e(TAG, "Cannot remove directory \'" + directory + "\': " + e);
            return false;
        }
    }

    private static boolean checkDirectory(Context context, String directory) {
        File subDir = new File(context.getFilesDir(), directory);
        return subDir.exists();
    }

    /**
     * Gets the number of targets, which are allowed to be accepted.
     *
     * @param context Context of the caller.
     * @return The number of targets, which are allowed to be accepted.
     */
    public static int getNumAcceptable(Context context) {
        if (numAcceptable == null) {
            numAcceptable = getSharedPreferences(context).getInt(NUM_ACCEPTABLE_KEY, DEFAULT_NUM_ACCEPTABLE);
        }
        return numAcceptable;
    }

    /**
     * Sets the number of targets, which are allowed to be accepted.
     *
     * @param context Context of the caller.
     * @param numAcceptable The number of targets, which are allowed to be accepted.
     */
    public static void setNumAcceptable(Context context, int numAcceptable) {
        SharedDataManager.numAcceptable = numAcceptable;
        getSharedPreferences(context).edit().putInt(NUM_ACCEPTABLE_KEY, numAcceptable).commit();
    }

    /**
     * Adds the number of targets, which are allowed to be accepted.
     *
     * @param context Context of the caller.
     * @param numAcceptable The number of targets, which will be added..
     */
    public static void addNumAcceptable(Context context, int numAcceptable) {
        if (SharedDataManager.numAcceptable == null) {
            getNumAcceptable(context);
        }
        SharedDataManager.numAcceptable += numAcceptable;
        getSharedPreferences(context).edit().putInt(NUM_ACCEPTABLE_KEY, SharedDataManager.numAcceptable).commit();
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
     * Sets the latitude of the last area/hunt.
     *
     * @param context Context of the caller.
     * @param latitude The latitude of the last area/hunt.
     */
    public static void setLastAreaLatitude(Context context, double latitude) {
        getSharedPreferences(context).edit().putLong(LAST_AREA_LATITUDE_KEY,
                Double.doubleToRawLongBits(latitude)).commit();
    }

    /**
     * Gets the latitude of the last area/hunt.
     *
     * @param context Context of the caller.
     */
    public static double getLastAreaLatitude(Context context) {
        return Double.longBitsToDouble(getSharedPreferences(context).getLong(LAST_AREA_LATITUDE_KEY, 0));
    }

    /**
     * Sets the longitude of the last area/hunt.
     *
     * @param context Context of the caller.
     * @param longitude The latitude of the last area/hunt.
     */
    public static void setLastAreaLongitude(Context context, double longitude) {
        getSharedPreferences(context).edit().putLong(LAST_AREA_LONGITUDE_KEY,
                Double.doubleToRawLongBits(longitude)).commit();
    }

    /**
     * Gets the longitude of the last area/hunt.
     *
     * @param context Context of the caller.
     */
    public static double getLastAreaLongitude(Context context) {
        return Double.longBitsToDouble(getSharedPreferences(context).getLong(LAST_AREA_LONGITUDE_KEY, 0));
    }

    /**
     * Sets the radius of the last area/hunt.
     *
     * @param context Context of the caller.
     * @param radius The latitude of the last area/hunt.
     */
    public static void setLastAreaRadius(Context context, int radius) {
        getSharedPreferences(context).edit().putInt(LAST_AREA_RADIUS_KEY, radius).commit();
    }

    /**
     * Gets the radius of the last area/hunt.
     *
     * @param context Context of the caller.
     */
    public static int getLastAreaRadius(Context context) {
        return getSharedPreferences(context).getInt(LAST_AREA_RADIUS_KEY, 0);
    }

    /**
     * Gets the preferred number of columns on offer page from user settings in portrait orientation.
     *
     * @param context Context of the caller.
     * @return The preferred number of columns on offer page from user settings in portrait orientation.
     */
    public static int getOfferColumnsPortrait(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString("offer_columns_portrait", "2"));
    }

    /**
     * Gets the preferred number of columns on offer page from user settings in landscape orientation.
     *
     * @param context Context of the caller.
     * @return The preferred number of columns on offer page from user settings in landscape orientation.
     */
    public static int getOfferColumnsLandscape(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString("offer_columns_landscape", "3"));
    }

    /**
     * Removes history photos only when the settings allow that. Sets the settings back when operation
     * is successful.
     *
     * @param context Context of the caller.
     */
    public static void tryRemoveHistoryPhotos(Context context) {
        boolean remove = getSharedPreferences(context).getBoolean("pref_delete_history_photos", false);
        if (checkDirectory(context, TARGET_PHOTOS_DIRECTORY) && remove) {
            List<Target> history = getTargetsFromHistory(context);
            for (Target target : history) {
                if (target != null && target.getState() == Target.TargetState.COMPLETED) {
                    target.removePhotos();
                }
            }
        }
        getSharedPreferences(context).edit().putBoolean("pref_delete_history_photos", false).commit();
    }

    /**
     * Removes history information only when the settings allow that. Sets the settings back when operation
     * is successful.
     *
     * @param context Context of the caller.
     */
    public static void tryRemoveHistoryInformation(Context context) {
        boolean remove = getSharedPreferences(context).getBoolean("pref_delete_history_information", false);
        if (checkDirectory(context, HISTORY_DIRECTORY) && remove) {
            List<Target> history = getTargetsFromHistory(context);
            for (Target target : history) {
                if (target != null && target.getState() == Target.TargetState.COMPLETED) {
                    target.removePhotos();
                    removeObject(context, HISTORY_TARGET_PREFIX + target.getPlaceID(), HISTORY_DIRECTORY);
                }
            }
        }
        getSharedPreferences(context).edit().putBoolean("pref_delete_history_information", false).commit();
    }

    /**
     * Reads value from settings, if the player wants to use debug mode for visiting targets.
     *
     * @param context Context of the caller.
     * @return If the player wants to use debug mode for visiting targets.
     */
    public static boolean debugActiveZone(Context context) {
        return getSharedPreferences(context).getBoolean("pref_debug_active_zone", false);
    }

    /**
     * Reads value from settings, if the player wants to show wizard dialog on new hunt.
     *
     * @param context Context of the caller.
     * @return If the player wants to show wizard dialog on new hunt.
     */
    public static boolean showWizardOnNewHunt(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_new_hunt_wizard", true);
    }

    /**
     * Reads value from settings, if the player wants to show wizard dialog on target lock.
     *
     * @param context Context of the caller.
     * @return If the player wants to show wizard dialog on target lock.
     */
    public static boolean showWizardOnTargetLocked(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_locked_wizard", true);
    }

    /**
     * Reads value from settings, if the player wants to show wizard dialog on target completion.
     *
     * @param context Context of the caller.
     * @return If the player wants to show wizard dialog on target completion.
     */
    public static boolean showWizardOnTargetCompleted(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_completed_wizard", true);
    }

    /**
     * Reads value from settings, if the player wants to show confirmation wizard dialog on target rejection.
     *
     * @param context Context of the caller.
     * @return If the player wants to show confirmation wizard dialog on target rejection.
     */
    public static boolean showConfirmationOfRejectTarget(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_reject_confirm", true);
    }

    /**
     * Reads value from settings, if the player wants to show confirmation wizard dialog when opening up target.
     *
     * @param context Context of the caller.
     * @return If the player wants to show confirmation wizard dialog when opening up target.
     */
    public static boolean showConfirmationOfOpenUpTarget(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_open_up_confirm", true);
    }

    /**
     * Reads value from settings, if the player wants to show confirmation wizard dialog when accepting target.
     *
     * @param context Context of the caller.
     * @return If the player wants to show confirmation wizard dialog when accepting target.
     */
    public static boolean showConfirmationOfAcceptTarget(Context context) {
        return getSharedPreferences(context).getBoolean("pref_show_accept_confirm", true);
    }

    /**
     * Reads value from settings, if the player wants to save the photos from camera externally to gallery.
     *
     * @param context Context of the caller.
     * @return If the player wants to save the photos from camera externally to gallery.
     */
    public static boolean storePhotosExternally(Context context) {
        return getSharedPreferences(context).getBoolean("pref_store_photos_externally", true);
    }

    /**
     * Gets the daytime preferred by user.
     *
     * @param context Context of the caller.
     * @return The daytime preferred by user.
     */
    public static Photo.DAYTIME getPreferredDaytime(Context context) {
        return Photo.DAYTIME.values()[getSharedPreferences(context).getInt(PREFERRED_DAYTIME_KEY, 0)];
    }

    /**
     * Sets the daytime preferred by user.
     *
     * @param context Context of the caller.
     * @param daytime The daytime preferred by user.
     */
    public static void setPreferredDaytime(Context context, Photo.DAYTIME daytime) {
        getSharedPreferences(context).edit().putInt(PREFERRED_DAYTIME_KEY, daytime.ordinal()).commit();
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

    /**
     * Gets the number of hunts from shared preferences.
     *
     * @param context Context of the caller.
     * @return The number of hunts from shared preferences.
     */
    public static int getHuntNumber(Context context) {
        return getSharedPreferences(context).getInt(HUNT_NUMBER_KEY, 0);
    }

    /**
     * Sets the number of hunts player played.
     *
     * @param context Context of the caller.
     * @param number The number of hunts.
     */
    public static void setHuntNumber(Context context, int number) {
        getSharedPreferences(context).edit().putInt(HUNT_NUMBER_KEY, number).commit();
    }

    /**
     * Increases the value storing the number of hunts by one.
     *
     * @param context Context of the caller.
     */
    public static void increaseHuntNumber(Context context) {
        setHuntNumber(context, getHuntNumber(context) + 1);
    }

    /**
     * Initializes the start time, clears the pending requests and prepares the shared values for new hunt.
     *
     * @param context Context of the caller.
     * @param ready Specifies if the huntReady boolean. If the hunt is ready to start immediately.
     * @param time Time of start of the new hunt.
     */
    public static synchronized void initNewHunt(Context context, boolean ready, long time) {
        setStartTime(context, time);
        clearPendingRequests(context);
        setNumAcceptable(context, DEFAULT_NUM_ACCEPTABLE);
        if (huntReady == null || huntReady != ready) {
            huntReady = ready;
            getSharedPreferences(context).edit().putBoolean(HUNT_READY_KEY, ready).commit();
        }
    }

    /**
     * Gets the value of huntReady boolean.
     *
     * @param context Context of the caller.
     * @return If the hunt is ready to start immediately.
     */
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
     * Converts place ID to name of directory, where data for this target should be stored.
     *
     * @param placeID Place ID to convert.
     * @return The place ID converted to name of directory.
     */
    private static String getDirectoryForTarget(String placeID) {
        return TARGET_PREFIX + placeID;
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
     * Loads a target for given Place ID from local file.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target to load.
     * @return The loaded Target if possible, null otherwise.
     */
    public static Target getTarget(Context context, String placeID) {
        if (placeID == null) {
            return null;
        }
        /* Otherwise try to read the object from file */
        Object object = readObject(context, TARGET_FILENAME, getDirectoryForTarget(placeID));
        if (object != null && object instanceof Place) {
            return (Target) object;
        }
        return null;
    }

    /**
     * Creates a new directory for given target and stores the target into it for later use.
     *
     * @param context Context of the caller.
     * @param target Target to store (serialize) for later use.
     * @return True on success, false on fail.
     */
    public static boolean addTarget(Context context, Target target) {
        return writeObject(context, target, TARGET_FILENAME, getDirectoryForTarget(target.getID()));
    }

    /**
     * Removes all the cache directories for targets which are available and all the preferences
     * which are associated with these targets.
     *
     * @param context Context of the caller.
     */
    public static void removeTargets(Context context) {
        /* Remove photos of all targets which are not in history */
        Target[] targets = loadTargets(context);
        if (targets != null) {
            for (Target target : targets) {
                if (target != null && target.getState() != Target.TargetState.LOCKED
                        && target.getState() != Target.TargetState.COMPLETED) {
                    target.removePhotos();
                }
            }
        }
        /* Remove directories of all targets */
        String[] fileList = context.getFilesDir().list();
        List<String> placeDirNames = new ArrayList<>();
        /* Locate the directories containing the target files */
        for (String fileName : fileList) {
            if (fileName.startsWith(TARGET_PREFIX)) {
                placeDirNames.add(fileName);
            }
        }
        /* Remove the located directories of cached targets */
        for (String dirName : placeDirNames) {
            removeDirectory(context, dirName);
            String placeID = dirName.substring(TARGET_PREFIX.length());
            getPreferencesForTarget(context, placeID).edit().clear().commit();
        }
        /* Remove their shared preferences */
        File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (String aChildren : children) {
            if (aChildren.startsWith(TARGET_SHARED_DATA_FILENAME)) {
                //noinspection ResultOfMethodCallIgnored
                new File(dir, aChildren).delete();
            }
        }
    }

    /**
     * Associates given photo object with target specified by its Place ID, stores it locally.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of associated target.
     * @param photo Photo to be stored (serialized).
     * @param photoID Unique name of the photo.
     * @return True on success, false on fail.
     */
    public static boolean addPhotoOfTarget(Context context, String placeID, Photo photo, String photoID) {
        /* Save the photo */
        boolean isOk = writeObject(context, photo, getFilenameForPhoto(photoID), getDirectoryForTarget(placeID));
        /* Save the location of the new photo */
        SharedPreferences preferences = getPreferencesForTarget(context, placeID);
        Set<String> set = preferences.getStringSet(PHOTOS_SET_KEY, new HashSet<String>());
        set.add(getFilenameForPhoto(photoID));
        preferences.edit().putStringSet(PHOTOS_SET_KEY, set).apply();

        return isOk;
    }

    /**
     * Adds given target to the history of completed or locked targets.
     *
     * @param context Context of the caller.
     * @param target Completed or locked target.
     */
    public static void addTargetToHistory(Context context, Target target) {
        writeObject(context, target, HISTORY_TARGET_PREFIX + target.getPlaceID(), HISTORY_DIRECTORY);
    }

    /**
     * Adds given request to the history and associates it with a given target.
     *
     * @param context Context of the caller.
     * @param placeID The place ID of a target, which the request is associated to.
     * @param request Request available for this target, null if the target should not have any requests
     */
    public static void addRequestToHistory(Context context, String placeID, Request request) {
        if (request != null) {
            writeObject(context, request, HISTORY_REQUEST_PREFIX + placeID, HISTORY_DIRECTORY);
        } else {
            removeObject(context, HISTORY_REQUEST_PREFIX + placeID, HISTORY_DIRECTORY);
        }
    }

    /**
     * Gets photo of target from cached file in internal memory.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target whose photo should be returned.
     * @param index Index of the photo to return.
     * @return The photo of target from cached file in internal memory.
     */
    public static Photo getTargetPhoto(Context context, String placeID, int index) {
        return (Photo) readObject(context, placeID + "_" + index, TARGET_PHOTOS_DIRECTORY);
    }

    /**
     * Saves given photo to cache in internal memory.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target whose photo should be saved.
     * @param index Index identifying the photo.
     * @param photo The photo to save.
     */
    public static void saveTargetPhoto(Context context, String placeID, int index, Photo photo) {
        writeObject(context, photo, placeID + "_" + index, TARGET_PHOTOS_DIRECTORY);
    }

    /**
     * Removes all target photos of target specified by its place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target whose photos should be removed.
     */
    public static void removeTargetPhotos(Context context, String placeID) {
        File dir = new File(context.getFilesDir() + "/" + TARGET_PHOTOS_DIRECTORY);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files) {
                if (file.startsWith(placeID + "_")) {
                    removeObject(context, file, TARGET_PHOTOS_DIRECTORY);
                }
            }
        }
    }

    /**
     * Gets all the targets saved in history. Always not null.
     *
     * @param context Context of the caller.
     * @return All the targets saved in history.
     */
    public static List<Target> getTargetsFromHistory(Context context) {
        List<Target> targets = new ArrayList<>();
        File dir = new File(context.getFilesDir() + "/" + HISTORY_DIRECTORY);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files) {
                if (file.startsWith(HISTORY_TARGET_PREFIX)) {
                    Target target = (Target) readObject(context, file, HISTORY_DIRECTORY);
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
        }
        return targets;
    }

    /**
     * Finds out, if some locked target is currently in history.
     *
     * @param context Context of the caller.
     * @return If some locked target is currently in history.
     */
    public static boolean isLockedTargetInHistory(Context context) {
        File dir = new File(context.getFilesDir() + "/" + HISTORY_DIRECTORY);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files) {
                if (file.startsWith(HISTORY_TARGET_PREFIX)) {
                    Target target = (Target) readObject(context, file, HISTORY_DIRECTORY);
                    if (target != null && target.getState() == Target.TargetState.LOCKED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets all requests from the history with Place ID of the target they belong to as a key.
     *
     * @param context Context of the caller.
     * @return All requests from the history with Place ID of the target they belong to as a key.
     */
    public static Map<String, Request> getRequestsFromHistory(Context context) {
        Map<String, Request> requests = new HashMap<>();
        File dir = new File(context.getFilesDir() + "/" + HISTORY_DIRECTORY);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files) {
                if (file.startsWith(HISTORY_REQUEST_PREFIX)) {
                    String placeID = file.substring(HISTORY_REQUEST_PREFIX.length());
                    requests.put(placeID, (Request) readObject(context, file, HISTORY_DIRECTORY));
                }
            }
        }
        return requests;
    }

    /**
     * Retrieves all photos saved as associated with given place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target, that has the associated photos.
     * @return All the photos saved as associated with given place ID.
     */
    public static Photo[] getPhotosOfTarget(Context context, String placeID) {
        /* Retrieve location of all photos for given place ID */
        SharedPreferences preferences = getPreferencesForTarget(context, placeID);
        Set<String> photoNames = new HashSet<>();
        photoNames = preferences.getStringSet(PHOTOS_SET_KEY, photoNames);

        /* Read the photos from those locations */
        List<Photo> photos = new ArrayList<>();
        for (String photoName : photoNames) {
            Object object = readObject(context, photoName, getDirectoryForTarget(placeID));
            if (object != null && object instanceof Photo) {
                photos.add((Photo) object);
            }
        }
        return photos.toArray(new Photo[photos.size()]);
    }

    /**
     * Stores the request associated with target. Only one request can be stored for one target.
     *
     * @param context Context of the caller.
     * @param request Request that will be stored.
     * @param placeID Place ID of the target for which the request was made.
     * @return True on success, false on fail.
     */
    public static boolean setRequestForTarget(Context context, Request request, String placeID) {
        boolean isOk = writeObject(context, request, REQUEST_FILENAME, getDirectoryForTarget(placeID));
        Set<String> requests = getSharedPreferences(context).getStringSet(REQUESTS_SET_KEY, new HashSet<String>());
        requests.add(placeID);
        getSharedPreferences(context).edit().putStringSet(REQUESTS_SET_KEY, requests).apply();

        return isOk;
    }

    /**
     * Gets the request associated with target specified by given Place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target, for which the request is requested.
     * @return The request if exists, null otherwise or on error.
     */
    public static Request getRequestForTarget(Context context, String placeID) {
        Object object = readObject(context, REQUEST_FILENAME, getDirectoryForTarget(placeID));
        if (object != null && object instanceof Request) {
            return (Request) object;
        }
        return null;
    }

    /**
     * Removes the request object for the target specified by its ID from local files.
     *
     * @param context Context of the caller.
     * @param placeID The Place ID of the target, that the request is associated to.
     */
    public static void removeRequestForTarget(Context context, String placeID) {
        /* Delete the request object */
        removeObject(context, REQUEST_FILENAME, getDirectoryForTarget(placeID));
        /* Remove the request from list of all pending requests */
        Set<String> requests = getSharedPreferences(context).getStringSet(REQUESTS_SET_KEY, new HashSet<String>());
        requests.remove(placeID);
        getSharedPreferences(context).edit().putStringSet(REQUESTS_SET_KEY, requests).apply();
    }

    /**
     * Returns place IDs of targets with pending requests for current hunt.
     *
     * @param context Context of the caller.
     * @return Place IDs of targets with pending requests for current hunt.
     */
    public static Set<String> getPendingRequestsIDs(Context context) {
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
     * Removes all photos associated to the target specified by given Place ID.
     *
     * @param context Context of the caller.
     * @param placeID Place ID of the target whose photos will be removed.
     */
    public static void clearPhotosOfTarget(Context context, String placeID) {
        /* Retrieve location of all photos for given place ID */
        SharedPreferences preferences = getPreferencesForTarget(context, placeID);
        Set<String> photoNames = new HashSet<>();
        photoNames = preferences.getStringSet(PHOTOS_SET_KEY, photoNames);

        /* Remove those photo files */
        for (String photoName : photoNames) {
            removeObject(context, photoName, getDirectoryForTarget(placeID));
        }
        /* Remove the list of photos from preferences of this place */
        preferences.edit().remove(PHOTOS_SET_KEY).apply();
    }

}
