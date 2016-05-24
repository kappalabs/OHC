package com.kappa_labs.ohunter.client.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.kappa_labs.ohunter.client.entities.Target;
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
     * @param targets Targets to be shown as tiles.
     */
    public TileAdapter(Context context, List<Target> targets) {
        this.mContext = context;
        this.mTargets = targets;
    }

    /**
     * Connects this adapter to a context and list of targets.
     *
     * @param context Context for this adapter.
     * @param targets Targets to be shown as tiles.
     */
    public void connect(Context context, List<Target> targets) {
        this.mContext = context;
        this.mTargets = targets;
    }

    /**
     * Releases internal context and fields.
     */
    public void disconnect() {
        mContext = null;
        mTargets = null;
    }

    @Override
    public int getCount() {
        if (mTargets != null) {
            return mTargets.size();
        }
        return 0;
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

        Target myTarget = mTargets.get(position);
        if (targetTileView.getTarget() != myTarget) {
            targetTileView.setTarget(myTarget);
        }
        targetTileView.update();

        return targetTileView;
    }

}
