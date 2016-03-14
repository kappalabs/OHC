package com.kappa_labs.ohunter.client.entities;

import java.io.Serializable;

/**
 * Class for storing basic, not memory intensive, information for TargetTileView.
 * Each Target is associated with one Place and TargetTileView. This class also provides logic
 * for different states of a target throughout the game.
 */
public class Target implements Serializable, Comparable<Target> {

    /**
     * Represents a state of target.
     */
    public enum TargetState {
        PHOTOGENIC(70),
        ACCEPTED(60),
        ACTIVATED(60),
        REJECTED(50),
        LOCKED(40),
        COMPLETED(30),
        FETCHING(20),
        EMPTY(10),
        UNAVAILABLE(0);

        private int mWeight;

        TargetState(int weight) {
            this.mWeight = weight;
        }

        /**
         * Compare this state with given state, return value suitable for Comparator.
         * States compared by this function should be sorted from most important to least important.
         *
         * @param second The second state this should be compared to.
         * @return Value suitable for Comparator for sorting target states by importance.
         */
        public int compare(TargetState second) {
            if (mWeight < second.mWeight) {
                return 1;
            } else if (mWeight > second.mWeight) {
                return -1;
            }
            return 0;
        }
    }

    private boolean rotated;
    private boolean highlighted;
    private TargetState mState = TargetState.EMPTY;
    private String placeID;


    /**
     * Creates a new target associated with place identified by given place ID.
     *
     * @param placeID Unique identifier from Google Places API of associated place.
     */
    public Target(String placeID) {
        this.placeID = placeID;
    }

    @Override
    public int compareTo(Target another) {
        return mState.compare(another.mState);
    }

    /**
     * Activates a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean activate() {
        if (mState == TargetState.ACCEPTED) {
            mState = TargetState.ACTIVATED;
            return true;
        }
        return false;
    }

    /**
     * Deactivates a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean deactivate() {
        if (mState == TargetState.ACTIVATED) {
            mState = TargetState.ACCEPTED;
            return true;
        }
        return false;
    }

    /**
     * Returns true if this target is activated, false otherwise.
     * @return True if this target is activated, false otherwise.
     */
    public boolean isActivated() {
        return mState == TargetState.ACTIVATED;
    }

    /**
     * Accepts a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean accept() {
        if (mState == TargetState.REJECTED) {
            mState = TargetState.ACCEPTED;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the target is accepted, false otherwise.
     *
     * @return True if the target is accepted, false otherwise.
     */
    public boolean isAccepted() {
        return mState == TargetState.ACCEPTED;
    }

    /**
     * Rejects a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean reject() {
        if (mState == TargetState.ACCEPTED) {
            mState = TargetState.REJECTED;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the target is rejected, false otherwise.
     *
     * @return True if the target is rejected, false otherwise.
     */
    public boolean isRejected() {
        return mState == TargetState.REJECTED;
    }

    /**
     * Returns true if associated target tile is rotated.
     *
     * @return True if associated target tile is rotated.
     */
    public boolean isRotated() {
        return rotated;
    }

//    public void setRotated(boolean rotated) {
//        this.rotated = rotated;
//    }

    /**
     * Returns true if associated target tile is highlighted.
     *
     * @return True if associated target tile is highlighted.
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public void changeRotation() {
        rotated = !rotated;
    }

    /**
     * Gets the Place ID associated with this target.
     *
     * @return The Place ID associated with this target.
     */
    public String getPlaceID() {
        return placeID;
    }

    /**
     * Sets the Place ID associated with this target.
     *
     * @param placeID The Place ID to be associated with this target.
     */
    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }

    /**
     * Gets the current state of this target.
     *
     * @return The current state of this target.
     */
    public TargetState getState() {
        return mState;
    }

    /**
     * Sets the state of this target.
     *
     * @param state The state to be set.
     */
    public void setState(TargetState state) {
        this.mState = state;
    }

}
