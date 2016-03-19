package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.List;
import java.util.Objects;

/**
 * Adapter for gridView to host all the targets specified by given Place IDs in given Targets array
 * as PlaceTileViews.
 */
public class TileAdapter extends BaseAdapter {

    private Context mContext;
    private List<Target> mTargets;


    /**
     * Creates a new tile adapter to host all the targets specified by given Place IDs.
     *
     * @param context Context of this adapter.
     * @param targets Place IDs to be represented
     */
    public TileAdapter(Context context, List<Target> targets) {
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
            targetTileView = new TargetTileView(mContext);
        } else {
            targetTileView = (TargetTileView) convertView;
        }

        targetTileView.setTarget(mTargets.get(position));
        loadPlace(targetTileView);
        targetTileView.update();

        return targetTileView;
    }

    public void loadPlace(TargetTileView targetTileView) {
        Place place = targetTileView.getPlace();
        /* Tile does not have place or the place it has is not what it should be */
        if (place == null || !Objects.equals(place.getID(), targetTileView.getPlaceID())) {
            /* Can we load the file from local file? */
//            if ((place = SharedDataManager.getPlace(mContext, targetTileView.getPlaceID())) != null) {
            if ((place = PlacesManager.getPlace(mContext, targetTileView.getPlaceID())) != null) {
                /* Load the place from local file */
                targetTileView.setPlace(place);
            }
        }
    }

}
