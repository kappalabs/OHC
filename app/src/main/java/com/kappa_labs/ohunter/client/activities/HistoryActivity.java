package com.kappa_labs.ohunter.client.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.adapters.TileAdapter;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.PhotosManager;
import com.kappa_labs.ohunter.client.utilities.PointsManager;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.CompareRequest;
import com.kappa_labs.ohunter.lib.requests.CompleteTargetRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Activity providing all the completed and locked targets from previous hunts.
 */
public class HistoryActivity extends AppCompatActivity implements ResponseTask.OnResponseTaskCompleted {

    public static final String TAG = "HistoryActivity";

    private static TileAdapter mAdapter;

    private PointsManager mPointsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        GridView historyGrid = (GridView) findViewById(R.id.gridView_history);
        assert historyGrid != null;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            historyGrid.setNumColumns(SharedDataManager.getOfferColumnsPortrait(this));
        } else {
            historyGrid.setNumColumns(SharedDataManager.getOfferColumnsLandscape(this));
        }

        /* Create a manager to control the player's score */
        mPointsManager = MainActivity.getPointsManager();

        PhotosManager.init();
        /* Retrieve targets from history */
        final List<Target> targets = SharedDataManager.getTargetsFromHistory(this);
        /* Remove targets without photos */
        final List<Target> toRemove = new ArrayList<>();
        for (Target target : targets) {
            if (target.getPhoto(0) == null) {
                toRemove.add(target);
            }
        }
        targets.removeAll(toRemove);
        /* Sort the targets */
        Collections.sort(targets);
        for (Target target : targets) {
            target.setHighlighted(false);
        }
        /* Show the sorted targets from history in the grid */
        mAdapter = new TileAdapter(this, targets);
        historyGrid.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        /* Button to complete locked targets */
        FloatingActionButton evaluateFab = (FloatingActionButton) findViewById(R.id.fab_evaluate);
        assert evaluateFab != null;
        evaluateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Send all pending compare requests for evaluation */
                Map<String, Request> requests = SharedDataManager.getRequestsFromHistory(HistoryActivity.this);
                for (Target target : targets) {
                    Request request = requests.get(target.getPlaceID());
                    if (request != null) {
                        /* Asynchronously execute and wait for callback when result ready */
                        DialogFragment dialog = Wizard.getServerCommunicationDialog(HistoryActivity.this);
                        ResponseTask task = new ResponseTask(dialog, target, HistoryActivity.this);
                        task.execute(request);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* To be able to load the target's photos */
        PhotosManager.init();
    }

    @Override
    protected void onDestroy() {
        /* Release the reference */
        PhotosManager.disconnect();
        super.onDestroy();
    }

    @Override
    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
        /* Problem on the server side */
        if (ohex != null) {
            Wizard.informOHException(this, ohex);
            return;
        }
        /* Problem on the client side */
        if (response == null) {
            Wizard.informNullResponse(this);
            return;
        }
        /* Success */
        if (data instanceof Target && request instanceof CompareRequest) {
            /* Request to evaluate similarity successfully finished */
            Target target = (Target) data;
            String photoReference = ((CompareRequest) request).getReferencePhoto().reference;

            Toast.makeText(HistoryActivity.this, String.format(getString(R.string.similarity_is),
                    response.similarity * 100), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "response similarity: " + response.similarity);

            /* Retrieve the discovery gain and count the similarity gain */
            int discoveryGain = target.getDiscoveryGain();
            int similarityGain = mPointsManager.getTargetSimilarityGain(response.similarity);
            Log.d(TAG, "discoveryGain = " + discoveryGain + ", similarityGain = " + similarityGain);

            Request completeRequest = new CompleteTargetRequest(
                    SharedDataManager.getPlayer(this),
                    target.getPlaceID(),
                    photoReference,
                    discoveryGain,
                    similarityGain,
                    SharedDataManager.getHuntNumber(HistoryActivity.this)
            );
            DialogFragment dialog = Wizard.getServerCommunicationDialog(HistoryActivity.this);
            ResponseTask task = new ResponseTask(dialog, target, HistoryActivity.this);
            task.execute(completeRequest);

            /* If the complete result fails, compare is not going to be done again */
            SharedDataManager.setRequestForTarget(this, completeRequest, target.getPlaceID());

            /* Add target to the history of completed ones */
            target.setState(Target.TargetState.LOCKED);
            SharedDataManager.addTargetToHistory(this, target);
            SharedDataManager.addRequestToHistory(this, target.getPlaceID(), completeRequest);
        } else if (data instanceof Target && request instanceof CompleteTargetRequest) {
            /* Request to complete the evaluated target successfully finished (stored in database) */
            Target target = (Target) data;
            String placeID = ((CompleteTargetRequest) request).getPlaceID();
            int discoveryGain = ((CompleteTargetRequest) request).getDiscoveryGain();
            int similarityGain = ((CompleteTargetRequest) request).getSimilarityGain();
            target.setDiscoveryGain(discoveryGain);
            target.setSimilarityGain(similarityGain);
            target.setState(Target.TargetState.COMPLETED);
            SharedDataManager.setPlayer(this, response.player);
            SharedDataManager.removeRequestForTarget(this, placeID);
            SharedDataManager.addTargetToHistory(this, target);
            SharedDataManager.addRequestToHistory(this, target.getPlaceID(), null);
            Wizard.targetCompletedDialog(this);
            Log.d(TAG, "Completion of target " + placeID + " was written to database.");
        }
        mAdapter.notifyDataSetChanged();
    }

}
