package client.ohunter.fojjta.cekuj.net.ohunter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Intent i = new Intent();
                i.setClass(MainActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
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
