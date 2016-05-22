package com.kappa_labs.ohunter.client.activities;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

/**
 * Dummy Application providing context even when the application is in background.
 */
public class DummyApplication extends MultiDexApplication {

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

    /**
     * Gets instance of this application.
     *
     * @return The instance of this application.
     */
    public static Application getApplication() {
        return instance;
    }

}
