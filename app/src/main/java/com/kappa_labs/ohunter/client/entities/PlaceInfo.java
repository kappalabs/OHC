package com.kappa_labs.ohunter.client.entities;

import android.content.Context;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.Comparator;

/**
 * Class to store one piece of information about place, that can be then simply shown in textView.
 */
public class PlaceInfo implements Comparable<PlaceInfo> {

    /**
     * Specifies type of value that is stored in PlaceInfo object.
     */
    public enum InfoType {
        PLAIN, URL
    }

    private String title;
    private String content;
    private InfoType type;
    private int weight;


    /**
     * Create a new piece of information about a place with given title and content.
     *
     * @param title Title for the content.
     * @param content Content of the information piece.
     */
    public PlaceInfo(String title, String content) {
        this.title = title;
        this.content = content;

        this.type = InfoType.PLAIN;
    }

    /**
     * Translates the key from Place Details to value from android strings, creates a new PlaceInfo
     * with value as content and properly sets type and weight to support sorting.
     *
     * @param context Context of the caller.
     * @param key Key from the Place Details.
     * @param value Value for the given key.
     * @return The new PlaceInfo with all properties set and ready to sort.
     */
    public static PlaceInfo buildPlaceInfo(Context context, String key, String value) {
        String title;
        String content = value;
        boolean isUrl = false;
        int weight = 0;
        switch (key) {
            case "name":
                title = context.getString(R.string.gfields_name);
                weight = 60;
                break;
            case "formatted_address":
                title = context.getString(R.string.gfields_formatted_address);
                weight = 50;
                break;
            case "website":
                title = context.getString(R.string.gfields_website);
                weight = 40;
                isUrl = true;
                break;
            case "url":
                title = context.getString(R.string.gfields_url);
                weight = 30;
                isUrl = true;
                break;
            case "icon":
                title = context.getString(R.string.gfields_icon);
                weight = 20;
                isUrl = true;
                break;
            case "place_id":
                title = context.getString(R.string.gfields_place_id);
                weight = 10;
                break;
            default:
                title = "";
                break;
        }
        PlaceInfo info = new PlaceInfo(title, content);
        info.setWeight(weight);
        if (isUrl) {
            info.setType(InfoType.URL);
        }

        return info;
    }

    @Override
    public int compareTo(PlaceInfo another) {
        if (weight > another.weight) {
            return -1;
        }
        if (weight > another.weight) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns true if title or content is missing. False otherwise.
     *
     * @return True if title or content is missing. False otherwise.
     */
    public boolean isEmpty() {
        return title == null || content == null || title.isEmpty() || content.isEmpty();
    }

    /**
     * Gets the title of this information.
     *
     * @return The title of this information.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the type of this information.
     *
     * @param type The type of this information.
     */
    public void setType(InfoType type) {
        this.type = type;
    }

    /**
     * Gets the content of this information.
     *
     * @return The content of this information.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the type of this information.
     *
     * @return The type of this information.
     */
    public InfoType getType() {
        return type;
    }

    /**
     * Gets the weight of this information. Bigger means more important.
     *
     * @return The weight of this information.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets the weight of this information. Bigger means more important.
     *
     * @param weight The weight of this information.
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

}
