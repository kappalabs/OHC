package com.kappa_labs.ohunter.client.utilities;

import android.content.Context;
import android.support.v4.app.DialogFragment;

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
    public static final int MAX_DEFER_COST = 10;
    public static final int MIN_PHOTOGRAPHED_GAIN = 1;
    public static final int SPOIL_FUNCTION_MAX = 10;
    public static final double SPOIL_FUNCTION_STAGTIME = 5f;

    private static final double SPOIL_MULTIPLIER = Math.log(1. / SPOIL_FUNCTION_MAX) / (MAX_HUNT_TIME_HOURS - SPOIL_FUNCTION_STAGTIME);

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
     * Gets the cost of rejecting a target.
     *
     * @return The cost of rejecting a target.
     */
    public static int getRejectCost() {
        return MAX_REJECT_COST;
    }

    /**
     * Gets the cost of deferring a target.
     *
     * @return The cost of deferring a target.
     */
    public static int getDeferCost() {
        return MAX_DEFER_COST;
    }

    /**
     * Gets the score of the current player.
     *
     * @return The score of the current player.
     */
    public int getScore() {
        return SharedDataManager.getPlayer(mContext).getScore();
    }

    /**
     * Sets given points to the current player.
     *
     * @param score Number of points to set (can be negative).
     * @return True on success, false on fail, when saving the change.
     */
    public boolean setScore(int score) {
        Player player = SharedDataManager.getPlayer(mContext);
        player.setScore(score);
        return SharedDataManager.setPlayer(mContext, player);
    }

    /**
     * Adds given points to the current player.
     *
     * @param points Number of points to add (can be negative to subtract/remove).
     * @return True on success, false on fail, when saving the change.
     */
    @SuppressWarnings("unused")
    public boolean addPoints(int points) {
        Player player = SharedDataManager.getPlayer(mContext);
        player.setScore(player.getScore() + points);
        return SharedDataManager.setPlayer(mContext, player);
    }

    /**
     * Removes given number of points from the current player.
     *
     * @param points Number of points to remove (can be negative to add).
     * @return True on success, false on fail, when saving the change.
     */
    public boolean removePoints(int points) {
        Player player = SharedDataManager.getPlayer(mContext);
        player.setScore(player.getScore() - points);
        return SharedDataManager.setPlayer(mContext, player);
    }

    /**
     * Sends the current state of the player to the server database.
     *
     * @param context Active (currently shown) context.
     * @param listener Listener on the server task, notified when the task is completed.
     */
    public void updateInDatabase(Context context, ResponseTask.OnResponseTaskCompleted listener) {
        Player player = SharedDataManager.getPlayer(mContext);
        UpdatePlayerRequest request = new UpdatePlayerRequest(player);
        DialogFragment dialog = Wizard.getServerCommunicationDialog(context);
        ResponseTask task = new ResponseTask(dialog, listener);
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
     * Gets the cost of beginning a new area (hunt).
     *
     * @return The cost of beginning a new area (hunt).
     */
    public int getBeginAreaCost() {
        return MAX_BEGIN_AREA_COST;
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
     * Returns true, if the player has enough points to reject a target.
     *
     * @return True, if the player has enough points to reject a target, false otherwise.
     */
    public boolean canReject() {
        Player player = SharedDataManager.getPlayer(mContext);
        return player.getScore() >= getRejectCost();
    }

    /**
     * Returns true, if the player has enough points to defer a target.
     *
     * @return True, if the player has enough points to defer a target, false otherwise.
     */
    public boolean canDefer() {
        Player player = SharedDataManager.getPlayer(mContext);
        return player.getScore() >= getDeferCost();
    }

    /**
     * Gets the number of hours spend in the current hunt.
     *
     * @return The number of hours spend in the current hunt.
     */
    public double getTimeInHours() {
        return (System.currentTimeMillis() - SharedDataManager.getStartTime(mContext)) / 3600000.;
    }

    /**
     * Gets the number of points, that should be given to the player for discovery of the target at given time.
     *
     * @param time The time at which the discovery gain should be counted.
     * @return The number of points, that should be given to the player for discovery of the target at given time.
     */
    public int getTargetDiscoveryGain(double time) {
        return (int) Math.floor(spoil(time));
    }

    /**
     * Gets the number of points, that should be given to the player for discovery of the target at current time.
     *
     * @return The number of points, that should be given to the player for discovery of the target at current time.
     */
    public int getTargetDiscoveryGain() {
        return getTargetDiscoveryGain(getTimeInHours());
    }

    /**
     * Gets the number of points, that should be given to the player for given similarity of the photos at given time.
     *
     * @param time The time at which the similarity gain should be counted.
     * @return The number of points, that should be given to the player for given similarity of the photos at given time.
     */
    public int getTargetSimilarityGain(double time, float similarity) {
        return (int) Math.max(MIN_PHOTOGRAPHED_GAIN, Math.floor(similarity * spoil(time)));
    }

    /**
     * Gets the number of points, that should be given to the player for given similarity of the photos at current time.
     *
     * @return The number of points, that should be given to the player for given similarity of the photos at current time.
     */
    public int getTargetSimilarityGain(float similarity) {
        return getTargetSimilarityGain(getTimeInHours(), similarity);
    }

    /**
     * Count spoil function in given time from interval [0;M], where 0 is starting time, M is time of the end (in hours).
     * Spoil function represents the maximum number of points player can achieve for every part
     * of completed target at given time.
     *
     * @param time The current playing time from interval [0;M], where 0 is starting time, M is time of the end (in hours).
     * @return The value of spoil function at the given time.
     */
    private double spoil(double time) {
        if (time <= SPOIL_FUNCTION_STAGTIME) {
            return SPOIL_FUNCTION_MAX;
        }
        return SPOIL_FUNCTION_MAX * Math.exp((time - SPOIL_FUNCTION_STAGTIME) * SPOIL_MULTIPLIER);
    }

}
