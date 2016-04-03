package com.kappa_labs.ohunter.client.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.utilities.PointsManager;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds main game menu and takes care of user login prompt.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final int TIMER_INTERVAL = 1000;

    private TextView playerTextView;
    private TextView serverTextView;
    private TextView timeTextView;
    private Button mContinueHuntButton;

    private Handler mHandler;
    private static PointsManager mPointsManager;

    private static boolean requestingPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPointsManager = new PointsManager(this);

        playerTextView = (TextView) findViewById(R.id.textView_player);
        serverTextView = (TextView) findViewById(R.id.textView_server);
        timeTextView = (TextView) findViewById(R.id.textView_time);

        Button mNewHuntButton = (Button) findViewById(R.id.button_new_hunt);
        assert mNewHuntButton != null;
        mNewHuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPointsManager.canBeginArea()) {
                    String text = String.format(getString(R.string.error_missing_points),
                            mPointsManager.countMissingPoints(mPointsManager.getBeginAreaCost()));
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent();
                i.setClass(MainActivity.this, PrepareHuntActivity.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        });
        mContinueHuntButton = (Button) findViewById(R.id.button_continue_hunt);
        mContinueHuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(MainActivity.this, HuntActivity.class);
                startActivity(i);
            }
        });
        Button mStatisticsButton = (Button) findViewById(R.id.button_statistics);
        assert mStatisticsButton != null;
        mStatisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: statistiky
                //DEBUG: pouze test image...
                Bitmap b_ = BitmapFactory.decodeResource(getResources(), R.drawable.img);
                double min = Math.min(400. / b_.getWidth(), 240. / b_.getHeight());
                Bitmap b = Bitmap.createScaledBitmap(b_, (int)(b_.getWidth() * min), (int)(b_.getHeight() * min), true);
                b_.recycle();
                CameraActivity.init(b, "testPlaceID", "testPlaceReference");
                Intent i = new Intent();
                i.setClass(MainActivity.this, CameraActivity.class);
                startActivity(i);
            }
        });
        Button mHelpButton = (Button) findViewById(R.id.button_help);
        assert mHelpButton != null;
        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                //DEBUG: pouze test funkcionality nabidky
                List<String> ids = new ArrayList<>();
                ids.add("ChIJbcDGzgOVC0cRHVtUxuhyPyc");
                ids.add("ChIJnzfcNpSUC0cR02jk07H0ROA");
                ids.add("ChIJAyNP5o6UC0cRH2h_j6e-d9c");
                ids.add("ChIJL-htB_-UC0cRw5332DTK3qk");
                ids.add("ChIJTxbyv-OUC0cRFEhcU2Cq8po");
                ids.add("ChIJF9p_IeqUC0cREiNdJH5eISQ");
                ids.add("ChIJF-GOjz6uEmsRgENqSlnL0qA");
                ids.add("ChIJNe6UFBWuEmsRm-raxeK9RdI");
                HuntActivity.radarPlaceIDs = ids;
                /* Reset the states for new hunt */
                SharedDataManager.initNewHunt(MainActivity.this, false, System.currentTimeMillis());
                /* Start the main game activity with these groups of places prepared */
                Intent i = new Intent();
                i.setClass(MainActivity.this, HuntActivity.class);
                startActivity(i);
            }
        });
        Button mAboutButton = (Button) findViewById(R.id.button_about);
        assert mAboutButton != null;
        mAboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                Long start = SharedDataManager.getStartTime(MainActivity.this);
                if (start != null) {
                    long uprTime = start - Math.abs(System.currentTimeMillis() - start - PointsManager.MAX_HUNT_TIME_MILLIS + 10000);
                    SharedDataManager.setStartTime(MainActivity.this, uprTime);
                }
            }
        });
        Button mSettingsButton = (Button) findViewById(R.id.button_settings);
        assert mSettingsButton != null;
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });
        Button mLogOutButton = (Button) findViewById(R.id.button_logout);
        assert mLogOutButton != null;
        mLogOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
                startLoginActivity();
            }
        });

        /* Set the last used server address */
        if (!Utils.initServer(SharedDataManager.getLastServer(this))) {
            /* Wrong server address */
//            logout();
            updateInfo();
        }
    }

    /**
     * Gets the PointsManager with context of the main activity.
     *
     * @return The PointsManager with context of the main activity.
     */
    public static PointsManager getPointsManager() {
        return mPointsManager;
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Request login if necessary */
        if (SharedDataManager.getPlayer(this) == null) {
            startLoginActivity();
        }

        /* Continue button is visible only when the game can really continue... */
        if (SharedDataManager.isHuntReady(this)) {
            mContinueHuntButton.setVisibility(View.VISIBLE);
            timeTextView.setVisibility(View.VISIBLE);
        } else {
            mContinueHuntButton.setVisibility(View.GONE);
            timeTextView.setVisibility(View.GONE);
        }
        if (mHandler == null) {
            mHandler = new Handler();
            startTimer();
        }

        /* Check if the player has enough points */
        checkPoints();

        updateInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTimer();
    }

    private void startLoginActivity() {
        Intent i = new Intent();
        i.setClass(MainActivity.this, LoginActivity.class);
        startActivity(i);
    }

    private void onTimeIsUp() {
        /* Reset the states for new hunt */
        SharedDataManager.initNewHunt(this, false, 0);
        mContinueHuntButton.setVisibility(View.GONE);
        timeTextView.setVisibility(View.GONE);
        if (HuntActivity.hunt != null) {
            HuntActivity.hunt.finish();
        }
        /* Check if the player has enough points */
        checkPoints();
        /* Remove cached previous targets */
        SharedDataManager.removeTargets(MainActivity.this);
        //TODO: zobrazit nejake vysledky
        Log.d(TAG, "time's up");
    }

    private void checkPoints() {
        /* If the score is not high enough, add points to the player, but only when the previous hunt ended */
        if (mPointsManager.getScore() < 20 && !SharedDataManager.isHuntReady(this)) {
            mPointsManager.setScore(30);
            updateInfo();
            try {
                if (!requestingPoints) {
                    requestingPoints = true;
                    /* Update the value in the server database if possible */
                    mPointsManager.updateInDatabase(new Utils.OnResponseTaskCompleted() {
                        @Override
                        public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data) {
                            /* It's ok if the request was not successful, the update will be done later */
                            requestingPoints = false;
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update the textView on top of the main menu activity.
     */
    private void updateInfo() {
        /* Show the current server address in use */
        String text = String.format(getResources().getString(R.string.main_activity_server_address),
                Utils.getAddress(), Utils.getPort());
        serverTextView.setText(text);

        /* Show information about current player */
        Player player = SharedDataManager.getPlayer(this);
        /* No player is logged in */
        if (player == null) {
            playerTextView.setText(getString(R.string.your_nickname));
            return;
        }
        /* Player is logged in, show some information about him */
        text = String.format(getResources().getString(R.string.main_activity_player_title),
                player.getNickname(), player.getScore());
        playerTextView.setText(text);
    }

    private void updateTimeInfo() {
        Long startTime = SharedDataManager.getStartTime(this);
        if (startTime == null) {
            return;
        }
        long diff = (PointsManager.MAX_HUNT_TIME_MILLIS - System.currentTimeMillis() + startTime) / 1000;
        if (diff < 0 && SharedDataManager.isHuntReady(this)) {
            onTimeIsUp();
            return;
        }
        long seconds = diff % 60;
        long minutes = diff / 60 % 60;
        long hours = diff / 3600;
        String text = String.format(getResources().getString(R.string.main_activity_timer),
                hours, minutes, seconds);
        timeTextView.setText(text);
    }

    private void logout() {
        updateInfo();
        SharedDataManager.setPlayer(this, null);
    }

    /**
     * Starts the timer showing remaining time on the main screen.
     */
    public void startTimer() {
        mStatusChecker.run();
    }

    /**
     * Stops the timer showing remaining time on the main screen.
     */
    public void stopTimer() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateTimeInfo();
            } finally {
                mHandler.postDelayed(mStatusChecker, TIMER_INTERVAL);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflate the menu; this adds items to the action bar if it is present */
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        /* Invoke the settings */
        if (id == R.id.action_settings) {
            Intent i = new Intent();
            i.setClass(MainActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
