package com.kappa_labs.ohunter.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kappa_labs.ohunter.lib.entities.Player;


/**
 * Holds main game menu and takes care of user login prompt.
 */
public class MainActivity extends AppCompatActivity {

    private TextView playerTextView;
    private TextView serverTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerTextView = (TextView) findViewById(R.id.textView_player);
        serverTextView = (TextView) findViewById(R.id.textView_server);

        Button mNewHuntButton = (Button) findViewById(R.id.button_new_hunt);
        mNewHuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(MainActivity.this, PrepareHuntActivity.class);
                startActivity(i);
            }
        });
        Button mStatisticsButton = (Button) findViewById(R.id.button_statistics);
        mStatisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: pouze test image...
                Bitmap b_ = BitmapFactory.decodeResource(getResources(), R.drawable.img);
                double min = Math.min(400. / b_.getWidth(), 240. / b_.getHeight());
                Bitmap b = Bitmap.createScaledBitmap(b_, (int)(b_.getWidth() * min), (int)(b_.getHeight() * min), true);
                b_.recycle();
                CameraActivity.setTemplateImage(b);
                Intent i = new Intent();
                i.setClass(MainActivity.this, CameraActivity.class);
                startActivity(i);
            }
        });
        Button mHelpButton = (Button) findViewById(R.id.button_help);
        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
        Button mAboutButton = (Button) findViewById(R.id.button_about);
        mAboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
        Button mLogOutButton = (Button) findViewById(R.id.button_logout);
        mLogOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
                startLoginActivity();
            }
        });

        /* Set the last used server address */
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_FILE, MODE_PRIVATE);
        Utils.getInstance();
        if (!Utils.initServer(preferences.getString(LoginActivity.PREFS_LAST_SERVER,
                getString(R.string.prompt_server)))) {
            /* Wrong server address */
//            logout();
            updateInfo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SharedDataManager.getPlayer(this) == null) {
            startLoginActivity();
        }
        updateInfo();
    }

    private void startLoginActivity() {
        Intent i = new Intent();
        i.setClass(MainActivity.this, LoginActivity.class);
        startActivity(i);
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

    private void logout() {
        updateInfo();
        SharedDataManager.setPlayer(this, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
