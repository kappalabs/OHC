package com.kappa_labs.ohunter.client.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.kappa_labs.ohunter.client.R;

/**
 * Activity for showing HTML format pages.
 */
public class HelpActivity extends AppCompatActivity {

    public final static String BUNDLE_NAME = "html_bundle_name";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        /* If there are any translations of the html files */
        String language = "cs";
//        if (Locale.getDefault().getLanguage().equals("cs")) {
//            language = "cs";
//        } else {
//            language = "en";
//        }

        WebView webView = (WebView) findViewById(R.id.webView);
        assert webView != null;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            String s = extras.getString(BUNDLE_NAME);
            webView.loadUrl("file:///android_asset/" + s + "-" + language + ".html");
        }
    }

}
