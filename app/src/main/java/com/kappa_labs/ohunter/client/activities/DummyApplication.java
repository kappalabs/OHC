package com.kappa_labs.ohunter.client.activities;

import android.app.Application;
import android.content.Context;

/**
 * Dummy Application providing context even when the application is in background.
 */
public class DummyApplication extends Application {

    private static DummyApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    /**
     * Gets context which survives running in background.
     *
     * @return The context which survives running in background.
     */
    public static Context getContext(){
        return instance;
    }

}
