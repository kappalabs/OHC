package com.kappa_labs.ohunter.client.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.chart.HorizontalListView;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.BestPlayersRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Activity with table of best players and graph with score gain in previous games.
 */
public class StatisticsActivity extends AppCompatActivity implements ResponseTask.OnResponseTaskCompleted {

    public static final String TAG = "StatisticsActivity";

    private static final int DEFAULT_NUMBER_OF_BEST_PLAYERS = 10;

    private HorizontalListView horizontalListView;
    private TableLayout listTableLayout;
    private LinearLayout lay;

    private double highest;
    private int[] discoveryHeights, similarityHeights;
    private Integer[] binsDiscovery, binsSimilarity;
    private String[] labels;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        listTableLayout = (TableLayout) findViewById(R.id.tableLayout_list);

        /* Find the current player */
        Player player = SharedDataManager.getPlayer(StatisticsActivity.this);
        if (player == null) {
            /* Show login prompt message */
            Toast.makeText(StatisticsActivity.this, getString(R.string.login_prompt),
                    Toast.LENGTH_LONG).show();
            return;
        }

        /* Start request for best players */
        Request request = new BestPlayersRequest(player, DEFAULT_NUMBER_OF_BEST_PLAYERS);
        DialogFragment dialog = Wizard.getServerCommunicationDialog(StatisticsActivity.this);
        ResponseTask task = new ResponseTask(dialog, StatisticsActivity.this);
        task.execute(request);

        /* Variables for chart */
        final List<Target> targets = SharedDataManager.getTargetsFromHistory(this);
        int numHunts = SharedDataManager.getHuntNumber(this);
        binsDiscovery = new Integer[numHunts];
        binsSimilarity = new Integer[numHunts];
        labels = new String[numHunts];
        for (int i = 0; i < numHunts; i++) {
            binsDiscovery[i] = 0;
            binsSimilarity[i] = 0;
            labels[i] = (i + 1) + ".";
        }
        for (Target target : targets) {
            if (target.getState() != Target.TargetState.COMPLETED) {
                continue;
            }
            binsDiscovery[target.getHuntNumber() - 1] += target.getDiscoveryGain();
            binsSimilarity[target.getHuntNumber() - 1] += target.getSimilarityGain();
        }

        lay = (LinearLayout) findViewById(R.id.linearlay);
        horizontalListView = (HorizontalListView) findViewById(R.id.horizontalListView_chart);

        List<Integer> a = Arrays.asList(binsDiscovery);
        List<Integer> b = Arrays.asList(binsSimilarity);
        highest = Math.max(Collections.max(a), Collections.max(b));

        discoveryHeights = new int[numHunts];
        similarityHeights = new int[numHunts];
    }

    @Override
    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
        /* Problem on server side */
        if (ohex != null) {
            Toast.makeText(StatisticsActivity.this, getString(R.string.ohex_general) + " " + ohex,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.ohex_general) + ohex);
            return;
        }
        /* Problem on client side */
        if (response == null) {
            Log.e(TAG, "Problem on client side -> cannot start the o-hunt yet...");
            Toast.makeText(StatisticsActivity.this, getString(R.string.server_unreachable_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* Success */
        Player[] bests = response.players;
        if (bests == null) {
            Toast.makeText(StatisticsActivity.this, getString(R.string.error_cannot_read_players),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* Add the players to the list */
        for (Player best : bests) {
            if (best == null) {
                continue;
            }
            TextView nameView = new TextView(this);
            nameView.setText(best.getNickname());
            nameView.setLayoutParams(new TableRow.LayoutParams(1));
            nameView.setTextSize(25);
            TextView scoreView = new TextView(this);
            scoreView.setText(String.valueOf(best.getScore()));
            scoreView.setGravity(Gravity.RIGHT);
            scoreView.setTextSize(25);
            TableRow tableRow = new TableRow(this);
            tableRow.addView(nameView);
            tableRow.addView(scoreView);
            listTableLayout.addView(tableRow);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateSizeInfo();
    }

    private void updateSizeInfo() {
        /** This is onWindowFocusChanged method is used to allow the chart to
         * update the chart according to the orientation.
         * Here h is the integer value which can be updated with the orientation
         */
        int h;
        if (getResources().getConfiguration().orientation == 1) {
            h = (int) (lay.getHeight() * 0.5);
            if (h == 0) {
                h = 200;
            }
        } else {
            h = (int) (lay.getHeight() * 0.3);
            if (h == 0) {
                h = 130;
            }
        }
        for (int i = 0; i < discoveryHeights.length; i++) {
            discoveryHeights[i] = (int) ((h * binsDiscovery[i]) / highest);
            similarityHeights[i] = (int) ((h * binsSimilarity[i]) / highest);
        }
        horizontalListView.setAdapter(new bsAdapter(this, labels));
    }

    public class bsAdapter extends BaseAdapter {

        private Activity context;
        private String[] array;


        public bsAdapter(Activity context, String[] arr) {
            this.context = context;
            this.array = arr;
        }

        public int getCount() {
            return array.length;
        }

        public Object getItem(int position) {
            return array[position];
        }

        public long getItemId(int position) {
            return array.length;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.chart_column, parent, false);
            }
            View row = convertView;

            final TextView title = (TextView) row.findViewById(R.id.textView_bar_title);
            TextView discoveryTextView = (TextView) row.findViewById(R.id.textView_bar_left);
            TextView similarityTextView = (TextView) row.findViewById(R.id.textView_bar_right);

            TextView discoveryLabelTextView = (TextView) row.findViewById(R.id.textView_label_bar_left);
            TextView similarityLabelTextView = (TextView) row.findViewById(R.id.textView_label_bar_right);

            discoveryTextView.setHeight(discoveryHeights[position]);
            similarityTextView.setHeight(similarityHeights[position]);
            title.setText(labels[position]);

            discoveryLabelTextView.setText(String.valueOf(binsDiscovery[position]));
            similarityLabelTextView.setText(String.valueOf(binsSimilarity[position]));

            return row;
        }

    }

}
