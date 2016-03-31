package com.kappa_labs.ohunter.client.entities;

import android.support.annotation.NonNull;

import com.kappa_labs.ohunter.lib.entities.Place;

import java.io.Serializable;

/**
 * Class for storing basic, not memory intensive, information for TargetTileView.
 * Each Target is associated with one Place and TargetTileView. This class also provides logic
 * for different states of a target throughout the game.
 */
public class Target extends Place implements Serializable, Comparable<Target> {

    /**
     * Represents a state of target.
     */
    public enum TargetState {
        PHOTOGENIC(70),
        ACTIVATED(60),
        ACCEPTED(50),
        LOCKED(40),
        DEFERRED(30),
        COMPLETED(20),
        REJECTED(10);

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

        /**
         * Decides by automaton rules if given state can be changed to photogenic.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canPhotogenify() {
            return this == TargetState.ACTIVATED;
        }

        /**
         * Decides by automaton rules if given state can be changed to activated.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canActivate() {
            return this == TargetState.ACCEPTED;
        }

        /**
         * Decides by automaton rules if given state can be changed back from activated.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canDeactivate() {
            return this == TargetState.ACTIVATED;
        }

        /**
         * Decides by automaton rules if given state can be changed to accepted.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canAccept() {
            return this == TargetState.DEFERRED;
        }

        /**
         * Decides by automaton rules if given state can be changed to locked.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canLock() {
            return this == TargetState.PHOTOGENIC;
        }

        /**
         * Decides by automaton rules if given state can be changed to deferred.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canDefer() {
            return this == TargetState.ACCEPTED;
        }

        /**
         * Decides by automaton rules if given state can be changed to completed.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canComplete() {
            return this == TargetState.LOCKED;
        }

        /**
         * Decides by automaton rules if given state can be changed to rejected.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canReject() {
            return this == TargetState.DEFERRED;
        }

    }

    private boolean rotated;
    private boolean isRotationDrawn;
    private boolean highlighted;
    private TargetState mState = TargetState.DEFERRED;
    private int photoIndex;
    private boolean isPhotoDrawn;
    private int discoveryGain, similarityGain;
    private int rejectLoss;
    private boolean isStateInvalidated;


    /**
     * Creates a new target by downcasting given place.
     *
     * @param place The place that is the base of this target.
     */
    public Target(Place place) {
        initTarget(place);
    }

    public void initTarget(Place place) {
        this.longitude = place.longitude;
        this.latitude = place.latitude;
        this.gfields = place.getGfields();
        // TODO: 31.3.16 fotky spis loadovat externe, neukladat je tady
        this.photos = place.getPhotos();
    }

    /**
     * Gets the name of this target. Returns empty string if name does not exist.
     *
     * @return The name of this target. Empty string if does not exist.
     */
    public String getName() {
        String name = getGField("name");
        if (name == null) {
            return "";
        }
        return name;
    }

    @Override
    public int compareTo(@NonNull Target another) {
        return mState.compare(another.mState);
    }

    /**
     * Tries to change the state of this target. Returns true on success, false on fail.
     * Does nothing on fail.
     *
     * @param state The new state to set.
     * @return True on success, false on fail.
     */
    public boolean changeState(TargetState state) {
        isStateInvalidated = true;
        switch (state) {
            case PHOTOGENIC:
                return photogenify();
            case ACTIVATED:
                return activate();
            case ACCEPTED:
                return deactivate() || accept();
            case LOCKED:
                return lock();
            case DEFERRED:
                return defer();
            case COMPLETED:
                return complete();
            case REJECTED:
                return reject();
        }
        return false;
    }

    /**
     * Makes a target photogenic if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean photogenify() {
        if (mState.canPhotogenify()) {
            mState = TargetState.PHOTOGENIC;
            return true;
        }
        return false;
    }

    /**
     * Activates a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean activate() {
        if (mState.canActivate()) {
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
        if (mState.canDeactivate()) {
            mState = TargetState.ACCEPTED;
            return true;
        }
        return false;
    }

    /**
     * Accepts a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean accept() {
        if (mState.canAccept()) {
            mState = TargetState.ACCEPTED;
            return true;
        }
        return false;
    }

    /**
     * Locks a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean lock() {
        if (mState.canLock()) {
            mState = TargetState.LOCKED;
            return true;
        }
        return false;
    }

    /**
     * Defers a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean defer() {
        if (mState.canDefer()) {
            mState = TargetState.DEFERRED;
            return true;
        }
        return false;
    }

    /**
     * Completes a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean complete() {
        if (mState.canComplete()) {
            mState = TargetState.COMPLETED;
            return true;
        }
        return false;
    }

    /**
     * Rejects a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean reject() {
        if (mState.canReject()) {
            mState = TargetState.REJECTED;
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
     * Returns true if associated target tile is rotated.
     *
     * @return True if associated target tile is rotated.
     */
    public boolean isRotated() {
        return rotated;
    }

    /**
     * Returns true if associated target tile is highlighted.
     *
     * @return True if associated target tile is highlighted.
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Sets if target tile for this target should be highlighted.
     *
     * @param highlighted Specifies if target tile for this target should be highlighted.
     */
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    /**
     * Changes the rotation of the target tile for this target.
     */
    public void changeRotation() {
        rotated = !rotated;
    }

    /**
     * Gets the Place ID associated with this target.
     *
     * @return The Place ID associated with this target.
     */
    public String getPlaceID() {
        return getID();
    }

    /**
     * Gets the index of background image for place tile specified by current placeID.
     *
     * @return The index of background image for place tile specified by current placeID.
     */
    public int getPhotoIndex() {
        return photoIndex;
    }

    /**
     * Sets the index of image to be set onto background. Index is from range for place specified
     * by current placeID.
     *
     * @param photoIndex The index of image to be set onto background.
     */
    public void setPhotoIndex(int photoIndex) {
        if (this.photoIndex != photoIndex) {
            this.photoIndex = photoIndex;
            isPhotoDrawn = false;
        }
    }

    /**
     * Returns true if the background photo was already drawn on tile.
     *
     * @return True if the background photo was already drawn on tile. False otherwise.
     */
    public boolean isPhotoDrawn() {
        return isPhotoDrawn;
    }

    /**
     * Sets if the photo on the background was already drawn on tile.
     *
     * @param isPhotoDrawn Specifies if the photo on the background was already drawn on tile.
     */
    public void setIsPhotoDrawn(boolean isPhotoDrawn) {
        this.isPhotoDrawn = isPhotoDrawn;
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
     * Sets the state of this target without checking the automaton.
     *
     * @param state The state to be set.
     */
    public void setState(TargetState state) {
        this.mState = state;
    }

    /**
     * Gets the current state of rotation for connected target tile.
     *
     * @return The current state of rotation for connected target tile.
     */
    public boolean isRotationDrawn() {
        return isRotationDrawn;
    }

    /**
     * Sets the state of rotation for connected target tile.
     *
     * @param isRotationDrawn The state of rotation for connected target tile to be set.
     */
    public void setIsRotationDrawn(boolean isRotationDrawn) {
        this.isRotationDrawn = isRotationDrawn;
    }

    /**
     * Gets the score/points gain given for discovery of this target.
     *
     * @return The score/points gain given for discovery of this target.
     */
    public int getDiscoveryGain() {
        return discoveryGain;
    }

    /**
     * Sets the score/points gain given for discovery of this target.
     *
     * @param discoveryGain The score/points gain given for discovery of this target.
     */
    public void setDiscoveryGain(int discoveryGain) {
        this.discoveryGain = discoveryGain;
    }

    /**
     * Gets the score/points gain given for similarity of a photo of this target.
     *
     * @return The score/points gain given for similarity of a photo of this target.
     */
    public int getSimilarityGain() {
        return similarityGain;
    }

    /**
     * Sets the score/points gain given for similarity of a photo of this target.
     *
     * @param similarityGain The score/points gain given for similarity of a photo of this target.
     */
    public void setSimilarityGain(int similarityGain) {
        this.similarityGain = similarityGain;
    }

    /**
     * Gets the score/points decrease caused by rejecting this target.
     *
     * @return The score/points decrease caused by rejecting this target.
     */
    public int getRejectLoss() {
        return rejectLoss;
    }

    /**
     * Sets the score/points decrease caused by rejecting this target.
     *
     * @param rejectLoss The score/points decrease caused by rejecting this target.
     */
    public void setRejectLoss(int rejectLoss) {
        this.rejectLoss = rejectLoss;
    }

    /**
     * Gets if the state has been changed and is still invalidated.
     *
     * @return True if the state has been changed and is still invalidated, false otherwise.
     */
    public boolean isStateInvalidated() {
        return isStateInvalidated;
    }

    /**
     * Sets if the state has been changed and is invalidated.
     *
     * @param isStateInvalidated True if the state has been changed and is invalidated, false otherwise.
     */
    public void setIsStateInvalidated(boolean isStateInvalidated) {
        this.isStateInvalidated = isStateInvalidated;
    }

}
