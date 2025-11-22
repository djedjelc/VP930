package com.zenty.tpe.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for file operations
 */
public class FileUtils {
    private static final String TAG = "FileUtils";
    
    /**
     * Copy model files from assets to external storage
     */
    public static void copyModelsFromAssets(Context context, String targetDir) {
        File targetDirectory = new File(targetDir);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        
        try {
            String[] modelFiles = context.getAssets().list("models");
            if (modelFiles != null) {
                for (String filename : modelFiles) {
                    copyAssetFile(context, "models/" + filename, targetDir + "/" + filename);
                }
                Log.d(TAG, "Models copied successfully to " + targetDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying models", e);
        }
    }
    
    /**
     * Copy a single file from assets
     */
    private static void copyAssetFile(Context context, String assetPath, String targetPath) {
        File targetFile = new File(targetPath);
        
        // Skip if file already exists
        if (targetFile.exists()) {
            Log.d(TAG, "File already exists: " + targetPath);
            return;
        }
        
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            Log.d(TAG, "Copied: " + assetPath + " -> " + targetPath);
        } catch (Exception e) {
            Log.e(TAG, "Error copying file: " + assetPath, e);
        }
    }
    
    /**
     * Check if a file exists
     */
    public static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
