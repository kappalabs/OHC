package client.ohunter.fojjta.cekuj.net.ohunter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kappa_labs.ohunter.lib.entities.Player;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Holds main game menu and takes care of user login prompt.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PLAYER_FILENAME = "player_file";
    private static final int PLAYER_REQUEST_CODE = 1;

    public static Player mPlayer = null;

    private SharedPreferences mPreferences;
    private TextView playerTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = getPreferences(Context.MODE_PRIVATE);

        playerTextView = (TextView) findViewById(R.id.textView_player);

        /* Check if user is logged in */
        checkLogin();
        updateInfo();

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
                Bitmap b = Bitmap.createScaledBitmap(b_, 400, 240, true);
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
                //TODO: odhlaseni - nejak vymazat mPlayer ze vsech instanci aktivit ktere ji pouzivali
                //TODO: -> zabraneni moznosti back z login do mainu
                Intent i = new Intent();
                i.setClass(MainActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
    }

    private void checkLogin() {
        FileInputStream inputStream = null;
        try {
            /* Try to read Player object from file */
            inputStream = openFileInput(PLAYER_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            Object object = ois.readObject();
            if (object != null && object instanceof Player) {
                mPlayer = (Player) object;
                return;
            }

            /* Player from file is invalid, request login */
            startLoginActivity();
        } catch (Exception e) {
            /* File is unavailable, request login */
            startLoginActivity();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startLoginActivity() {
        Intent i = new Intent();
        i.setClass(MainActivity.this, LoginActivity.class);
        startActivityForResult(i, PLAYER_REQUEST_CODE);
    }

    private void updateInfo() {
        if (mPlayer == null) {
            playerTextView.setText(getString(R.string.your_nickname));
        }
        playerTextView.setText(mPlayer.getNickname() + " [" + mPlayer.getScore() + "]");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Login activity returns a Player object */
        if (requestCode == PLAYER_REQUEST_CODE && resultCode == RESULT_OK) {
            /* Write the given Player object into a local file */
            FileOutputStream outputStream = null;
            try {
                outputStream = openFileOutput(PLAYER_FILENAME, Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(mPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            updateInfo();
        }

        super.onActivityResult(requestCode, resultCode, data);
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
