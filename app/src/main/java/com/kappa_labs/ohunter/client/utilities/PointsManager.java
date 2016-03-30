package com.kappa_labs.ohunter.client.utilities;

import android.content.Context;

import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.requests.UpdatePlayerRequest;

/**
 * Class to manage score points of the player.
 */
public class PointsManager {

    public static final long MAX_HUNT_TIME_HOURS = 24;
    public static final long MAX_HUNT_TIME_MILLIS = MAX_HUNT_TIME_HOURS * 60 * 60 * 1000;

    public static final int MAX_BEGIN_AREA_COST = 20;
    public static final int MAX_REJECT_COST = 5;
    public static final int MIN_LOCATION_FOUND_GAIN = 1;
    public static final int MIN_PHOTOGRAPHED_GAIN = 1;
    public static final int SPOIL_FUNCTION_MAX = 10;
    public static final float SPOIL_FUNCTION_STAGTIME = 5f / MAX_HUNT_TIME_HOURS;

    private static final double SPOIL_MULTIPLIER = (MAX_HUNT_TIME_HOURS - SPOIL_FUNCTION_STAGTIME) / Math.log(1./SPOIL_FUNCTION_MAX);

    private Context mContext;


    /**
     * Creates a new manager of the score/points.
     *
     * @param mContext Context of the caller to enable reading/writing from/to local files.
     */
    public PointsManager(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Adds given points to the current player.
     *
     * @param points Number of points to add (can be negative).
     * @return True on success, false on fail, when saving the change.
     */
    public boolean addPoints(int points) {
        Player player = SharedDataManager.getPlayer(mContext);
        player.setScore(player.getScore() + points);
        return SharedDataManager.setPlayer(mContext, player);
    }

    /**
     * Sends the current state of the player to the server database.
     *
     * @param listener Listener on the server task, notified when the task is completed.
     */
    public void updateInDatabase(Utils.OnResponseTaskCompleted listener) {
        Player player = SharedDataManager.getPlayer(mContext);
        UpdatePlayerRequest request = new UpdatePlayerRequest(player);
        Utils.RetrieveResponseTask task = Utils.getInstance().new RetrieveResponseTask(listener,
                Utils.getServerCommunicationDialog(mContext));
        task.execute(request);
    }

    /**
     * Returns the number of points, which the player needs to have the given number of points.
     *
     * @param requiredPoints Number of points required.
     * @return The number of points, which the player needs to have the given number of points.
     */
    public int countMissingPoints(int requiredPoints) {
        Player player = SharedDataManager.getPlayer(mContext);
        return requiredPoints - player.getScore();
    }

    /**
     * Returns true, if the player has enough points to start a new area (hunt).
     *
     * @return True, if the player has enough points to start a new area (hunt), false otherwise.
     */
    public boolean canBeginArea() {
        Player player = SharedDataManager.getPlayer(mContext);
        return player.getScore() >= getBeginAreaCost();
    }

    /**
     * Gets the cost of beginning a new area (hunt).
     *
     * @return The cost of beginning a new area (hunt).
     */
    public int getBeginAreaCost() {
        return MAX_BEGIN_AREA_COST;
    }

    /**
     * Returns true, if the player has enough points to reject a target.
     *
     * @return True, if the player has enough points to reject a target, false otherwise.
     */
    public boolean canReject() {
        Player player = SharedDataManager.getPlayer(mContext);
        return player.getScore() >= getRejectCost();
    }

    /**
     * Gets the cost of rejecting a target.
     *
     * @return The cost of rejecting a target.
     */
    public int getRejectCost() {
        return MAX_REJECT_COST;
    }

    /**
     * Gets the number of hours spend in the current hunt.
     *
     * @return The number of hours spend in the current hunt.
     */
    public float getTimeInHours() {
        return (System.currentTimeMillis() - SharedDataManager.getStartTime(mContext)) / 3600000f;
    }

    /**
     * Gets the number of points, that should be given to the player for discovery of the target at current time.
     *
     * @return The number of points, that should be given to the player for discovery of the target at current time.
     */
    public int getTargetDiscoveryGain() {
        return Math.max(MIN_LOCATION_FOUND_GAIN, spoil(getTimeInHours() / MAX_HUNT_TIME_HOURS));
    }

    /**
     * Gets the number of points, that should be given to the player for given similarity of the photos at current time.
     *
     * @return The number of points, that should be given to the player for given similarity of the photos at current time.
     */
    public int getTargetSimilarityGain(float similarity) {
        return (int) Math.max(MIN_PHOTOGRAPHED_GAIN,
                Math.ceil(similarity * spoil(getTimeInHours() / MAX_HUNT_TIME_HOURS) / SPOIL_FUNCTION_MAX));
    }

    /**
     * Count spoil function in given time from interval [0;1], where 0 is starting time, 1 is time of the end.
     * Spoil function represents the maximum number of points player can achieve for every part
     * of completed target at given time.
     *
     * @param time The current playing time from interval [0;1], where 0 is starting time, 1 is time of the end.
     * @return The value of spoil function at the given time.
     */
    private int spoil(float time) {
        if (time <= SPOIL_FUNCTION_STAGTIME) {
            return SPOIL_FUNCTION_MAX;
        }
        return (int) Math.floor(Math.exp((time - SPOIL_FUNCTION_STAGTIME) * SPOIL_MULTIPLIER) * SPOIL_FUNCTION_MAX);
    }

}
