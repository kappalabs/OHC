package client.ohunter.fojjta.cekuj.net.ohunter;

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
        /* Otherwise read it from file */
        FileInputStream inputStream = null;
        try {
            /* Try to read Player object from file */
            inputStream = context.openFileInput(PLAYER_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            Object object = ois.readObject();
            if (object != null && object instanceof Player) {
                mPlayer = (Player) object;
                return mPlayer;
            }
            return null;
        } catch (Exception e) {
            /* File is unavailable */
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close Player file: " + e);
                }
            }
        }
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
        FileOutputStream outputStream = null;
        try {
            outputStream = context.openFileOutput(PLAYER_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(player);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot write the Player file: " + e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close Player file: " + e);
                }
            }
        }
        return false;
    }

}
