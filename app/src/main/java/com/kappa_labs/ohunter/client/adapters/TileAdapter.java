package com.kappa_labs.ohunter.client.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.TargetsManager;
import com.kappa_labs.ohunter.client.views.TargetTileView;

import java.util.List;

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
//        loadPlace(targetTileView);
        targetTileView.setPlace(mTargets.get(position));
        targetTileView.update();

        return targetTileView;
    }

    private void loadPlace(TargetTileView targetTileView) {
        Target target;
        if ((target = TargetsManager.getTarget(mContext, targetTileView.getPlaceID())) != null) {
            targetTileView.setPlace(target);
        }
    }

}
