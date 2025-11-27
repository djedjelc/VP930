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
        
        // Initialiser le logger de fichiers
        com.zenty.tpe.utils.FileLogger.init(this);
        
        // Installer le gestionnaire de crash
        Thread.setDefaultUncaughtExceptionHandler(new com.zenty.tpe.utils.CrashHandler(this));
        
        Log.d(TAG, "ZentyApp initialized with CrashHandler");
        com.zenty.tpe.utils.FileLogger.log(TAG, "App started");
        
        // Initialisation globale si nécessaire
    }
}
