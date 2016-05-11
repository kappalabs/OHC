package com.kappa_labs.ohunter.client.activities;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.utilities.ResponseTask;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.BestPlayersRequest;

/**
 * Activity with table of best players and graph with score gain in previous games.
 */
public class StatisticsActivity extends AppCompatActivity implements ResponseTask.OnResponseTaskCompleted {

    public static final String TAG = "StatisticsActivity";
    private static final int DEFAULT_NUMBER_OF_BEST_PLAYERS = 10;

    private TableLayout listTableLayout;


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
        System.out.println("mam " + bests.length + " nejlepsich hracu");
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

}
