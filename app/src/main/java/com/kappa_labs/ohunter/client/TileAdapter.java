package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Adapter for gridView to host all the targets specified by given Place IDs in given Targets array
 * as PlaceTileViews.
 */
public class TileAdapter extends BaseAdapter {

    String TAG = "TileAdapter";

    private Context mContext;
    private ArrayList<Target> mTargets;


    /**
     * Creates a new tile adapter to host all the targets specified by given Place IDs.
     *
     * @param context Context of this adapter.
     * @param targets Place IDs to be represented
     */
    public TileAdapter(Context context, ArrayList<Target> targets) {
        this.mContext = context;
        this.mTargets = targets;
    }

    @Override
    public int getCount() {
        return mTargets.size();
    }

    @Override
    public Object getItem(int position) {
        return mTargets.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TargetTileView targetTileView;

        if (convertView == null) {
            Log.d(TAG, "vytvarim nove placeTileView...");
            targetTileView = new TargetTileView(mContext);
            targetTileView.setPlaceTile(mTargets.get(position));
        } else {
            targetTileView = (TargetTileView) convertView;
            targetTileView.setPlaceTile(mTargets.get(position));
        }

        loadPlace(targetTileView);
        targetTileView.update();

        return targetTileView;
    }

    public void loadPlace(TargetTileView targetTileView) {
        Place place = targetTileView.getPlace();
        if (place != null && Objects.equals(place.getID(), targetTileView.getPlaceID())) {
            /* This tile is ready */
//            Log.d(TAG, placeTileView.getPlaceID() + " is ok (in memory)...");
        } else if ((place = SharedDataManager.getPlace(mContext, targetTileView.getPlaceID())) != null) {
            /* This place can be loaded from local file */
//            Log.d(TAG, placeTileView.getPlaceID() + " can be loaded from local file...");
            targetTileView.setPlace(place);
        }
        targetTileView.invalidate();
    }

}
