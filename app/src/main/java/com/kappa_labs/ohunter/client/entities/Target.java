package com.kappa_labs.ohunter.client.entities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.utilities.PhotosManager;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

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
        ACCEPTED(60),
        LOCKED(50),
        OPENED(40),
        UNAVAILABLE(30),
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
         * Gets the color to visualize this state.
         *
         * @param context Context of the caller to access the resources.
         * @return The color representing this state.
         */
        public int getColor(Context context) {
            if (context == null) {
                return Color.WHITE;
            }
            switch (this) {
                case PHOTOGENIC:
                    return ContextCompat.getColor(context, R.color.state_photogenic);
                case ACCEPTED:
                    return ContextCompat.getColor(context, R.color.state_accepted);
                case LOCKED:
                    return ContextCompat.getColor(context, R.color.state_locked);
                case OPENED:
                    return ContextCompat.getColor(context, R.color.state_opened);
                case COMPLETED:
                    return ContextCompat.getColor(context, R.color.state_completed);
                case REJECTED:
                    return ContextCompat.getColor(context, R.color.state_rejected);
                default:
                    return Color.WHITE;
            }
        }

        /**
         * Decides by automaton rules if given state can be changed to photogenic.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canPhotogenify() {
            return this == TargetState.ACCEPTED;
        }

        /**
         * Decides by automaton rules if given state can be changed to accepted.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canAccept() {
            return this == TargetState.OPENED;
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
         * Decides by automaton rules if given state can be changed to opened.
         *
         * @return True when rules are satisfied, false if not.
         */
        public boolean canOpenUp() {
            return this == TargetState.UNAVAILABLE;
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
            return this == UNAVAILABLE || this == TargetState.OPENED;
        }

    }

    private boolean rotated;
    private boolean isRotationDrawn;
    private boolean highlighted;
    private TargetState mState = TargetState.UNAVAILABLE;
    private int photoIndex;
    private int photoCount;
    private boolean isPhotoDrawn;
    private int discoveryGain, similarityGain;
    private int huntNumber;
    private int rejectLoss;
    private boolean isStateInvalidated;
    private transient Bitmap icon;
    private transient Bitmap selectedPhoto;


    /**
     * Creates a new target by downcasting given place.
     *
     * @param place The place that is the base of this target.
     */
    public Target(Place place, Bitmap icon) {
        initTarget(place, icon);
    }

    /**
     * Initiates new Target object from given Place and icon.
     *
     * @param place Place that will be extended to Target.
     * @param icon Icon representing the target.
     */
    public void initTarget(Place place, Bitmap icon) {
        this.longitude = place.longitude;
        this.latitude = place.latitude;
        this.gfields = place.getGfields();
        this.icon = icon;

        PhotosManager.addPhotosOfTarget(place.getID(), place.getPhotos());
        photoCount = place.getNumberOfPhotos();
    }

    @Override
    public List<Photo> getPhotos() {
        return PhotosManager.getPhotosOfTarget(getPlaceID());
    }

    @Override
    public Photo getPhoto(int index) {
        return PhotosManager.getPhotoOfTarget(getPlaceID(), index);
    }

    @Override
    public int getNumberOfPhotos() {
        return photoCount;
    }

    @Override
    public boolean addPhoto(Photo photo) {
        PhotosManager.addPhotoOfTarget(getPlaceID(), photo, photoCount);
        photoCount++;
        return true;
    }

    @Override
    public boolean addPhotos(Collection<? extends Photo> addPhotos) {
        PhotosManager.addPhotosOfTarget(getPlaceID(), (Photo[]) addPhotos.toArray());
        photoCount += addPhotos.size();
        return true;
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
            case ACCEPTED:
                return accept();
            case LOCKED:
                return lock();
            case OPENED:
                return openUp();
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
     * Opens up a target if automaton rules are satisfied. Returns true on success,
     * false otherwise.
     *
     * @return True on success, false otherwise.
     */
    public boolean openUp() {
        if (mState.canOpenUp()) {
            mState = TargetState.OPENED;
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
     * Removes all the photos of this target. Sets the selected photo index to zero.
     */
    public void removePhotos() {
        photos.clear();
        photoCount = 0;
        photoIndex = 0;
        if (selectedPhoto != null && !selectedPhoto.isRecycled()) {
            selectedPhoto.recycle();
            selectedPhoto = null;
        }
        PhotosManager.removePhotosOfTarget(getPlaceID());
    }

    /**
     * Checks if the given photo index is in valid bounds.
     *
     * @param index Index of the photo to be checked.
     * @return True if the photo index is in valid bounds, false otherwise.
     */
    public boolean isPhotoIndexValid(int index) {
        return index >= 0 && index < photoCount;
    }

    /**
     * Checks if the photo index is in valid bounds.
     *
     * @return True if the photo index is in valid bounds, false otherwise.
     */
    public boolean isPhotoIndexValid() {
        return isPhotoIndexValid(photoIndex);
    }

    /**
     * Returns the selected Photo object or null if the index is not valid.
     *
     * @return The selected Photo object or null if the index is not valid.
     */
    public Photo getSelectedPhoto() {
        if (isPhotoIndexValid()) {
            return getPhoto(photoIndex);
        }
        return null;
    }

    /**
     * Gets small/preview version of the selected photo as Bitmap or null if not created from
     * the Photo object yet and should be created.
     *
     * @return The small preview version of the selected photo as Bitmap. Null if it should be created.
     */
    public Bitmap getSelectedPhotoPreview() {
        return selectedPhoto;
    }

    /**
     * Set the small preview version of the selected photo for this target. This will be stored
     * only in memory.
     *
     * @param photo The small preview version of the selected photo for this target.
     */
    public void setSelectedPhoto(Bitmap photo) {
        if (selectedPhoto != null && !selectedPhoto.isRecycled()) {
            selectedPhoto.recycle();
        }
        selectedPhoto = photo;
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
            if (selectedPhoto != null && !selectedPhoto.isRecycled()) {
                selectedPhoto.recycle();
                selectedPhoto = null;
            }
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

    /**
     * Gets the icon representing this target.
     *
     * @return The icon representing this target.
     */
    public Bitmap getIcon() {
        return icon;
    }

    /**
     * Gets the number of hunt in which this target was completed.
     *
     * @return The number of hunt in which this target was completed.
     */
    public int getHuntNumber() {
        return huntNumber;
    }

    /**
     * Sets the number of hunt in which this target was completed.
     *
     * @param huntNumber The number of hunt in which this target was completed.
     */
    public void setHuntNumber(int huntNumber) {
        this.huntNumber = huntNumber;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        if (icon != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
            out.writeObject(stream.toByteArray());
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        byte[] bytes = (byte[]) ois.readObject();
        if (bytes != null) {
            icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

}
