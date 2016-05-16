package com.kappa_labs.ohunter.client.activities;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.kappa_labs.ohunter.client.R;

/**
 * Activity providing simple information about this application.
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            TextView versionNumber = (TextView) findViewById(R.id.textView_version_number);
            assert versionNumber != null;
            versionNumber.setText(String.valueOf(pInfo.versionCode));

            TextView versionName = (TextView) findViewById(R.id.textView_version_name);
            assert versionName != null;
            versionName.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
