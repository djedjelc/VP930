package com.zenty.tpe;

import android.app.Application;
import android.util.Log;

/**
 * Application Zenty pour terminal VP930Pro
 */
public class ZentyApp extends Application {
    private static final String TAG = "ZentyApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ZentyApp initialized");
        
        // Initialisation globale si nécessaire
    }
}
