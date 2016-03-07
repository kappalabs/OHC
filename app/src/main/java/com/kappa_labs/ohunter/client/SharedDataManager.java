package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kappa_labs.ohunter.lib.entities.Player;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class managing access to shared values from SharedPreferences and private files
 * among all the Activities.
 */
public class SharedDataManager {

    private static final String TAG = "PreferencesManager";
    private static final String SHARED_DATA_FILENAME = "SHARED_DATA_FILENAME";
    private static final String PLAYER_FILENAME = "PLAYER_FILENAME";

    private static SharedPreferences mPreferences;
    private static Player mPlayer;


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

}
