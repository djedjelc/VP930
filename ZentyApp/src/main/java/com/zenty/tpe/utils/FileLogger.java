package com.zenty.tpe.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitaire pour logger dans un fichier
 */
public class FileLogger {
    private static final String TAG = "FileLogger";
    private static final String FILE_NAME = "zenty_crash_log.txt";
    private static File logFile;

    public static void init(Context context) {
        try {
            // Utiliser le dossier de fichiers externes de l'app pour éviter les problèmes de permission
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                logFile = new File(dir, FILE_NAME);
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                log("FileLogger", "Logger initialized: " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to init logger", e);
        }
    }

    public static synchronized void log(String tag, String message) {
        Log.d(tag, message); // Toujours logger dans logcat aussi
        
        if (logFile == null) return;

        try (FileWriter fw = new FileWriter(logFile, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            String timestamp = sdf.format(new Date());
            fw.append(String.format("%s [%s] %s\n", timestamp, tag, message));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log", e);
        }
    }

    public static synchronized void logError(String tag, String message, Throwable tr) {
        Log.e(tag, message, tr);
        
        if (logFile == null) return;

        try (FileWriter fw = new FileWriter(logFile, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            String timestamp = sdf.format(new Date());
            
            fw.append(String.format("%s [ERROR] [%s] %s\n", timestamp, tag, message));
            
            if (tr != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                tr.printStackTrace(pw);
                fw.append(sw.toString());
                fw.append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write error log", e);
        }
    }
    
    public static String getLogContent() {
        if (logFile == null || !logFile.exists()) return "Log file not found";
        
        StringBuilder content = new StringBuilder();
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(logFile));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
        return content.toString();
    }
    
    public static void clearLogs() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
