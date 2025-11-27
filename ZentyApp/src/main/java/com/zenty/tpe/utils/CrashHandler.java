package com.zenty.tpe.utils;

import android.content.Context;
import android.os.Process;
import android.util.Log;

/**
 * Capture les crashs globaux
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context context;

    public CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e(TAG, "Uncaught Exception detected!", e);
        
        // Logger l'erreur dans le fichier
        FileLogger.logError(TAG, "FATAL CRASH DETECTED", e);
        
        // Laisser le système gérer le crash après avoir loggé
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }
}
