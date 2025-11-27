package com.zenty.tpe.palm;

import android.content.Context;
import android.util.Log;

import com.api.stream.Device;
import com.api.stream.IDevice;
import com.api.stream.IOpenCallback;
import com.api.stream.manager.DtUsbDevice;
import com.api.stream.manager.DtUsbManager;
import com.api.stream.manager.UsbMapTable;
import com.api.stream.veinshine.IVeinshine;

import java.util.Map;

/**
 * Gestionnaire centralisé pour le terminal VP930Pro
 * Singleton pour gérer l'instance IDevice
 */
public class PalmDeviceManager {
    private static final String TAG = "PalmDeviceManager";
    private static PalmDeviceManager instance;
    
    private IDevice device;
    private boolean isDeviceOpen = false;
    private boolean isAlgorithmEnabled = false;
    private DeviceStateCallback deviceStateCallback;
    
    // Threading pour les opérations device
    private final java.util.concurrent.ExecutorService singleThreadExecutor = 
        java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler = 
        new android.os.Handler(android.os.Looper.getMainLooper());
    
    private PalmDeviceManager() {}
    
    public static synchronized PalmDeviceManager getInstance() {
        if (instance == null) {
            instance = new PalmDeviceManager();
        }
        return instance;
    }
    
    /**
     * Initialise le device VP930Pro
     */
    public void initialize(Context context, DeviceStateCallback callback) {
        this.deviceStateCallback = callback;
        
        Device.create(context, new Device.DeviceListener() {
            @Override
            public void onDeviceCreatedSuccess(IDevice device, int deviceIndex, 
                                              Map<Long, IDevice> runningDevice, 
                                              UsbMapTable.DeviceType deviceType) {
                String msg = "Device created: " + deviceType + ", index: " + deviceIndex;
                Log.i(TAG, msg);
                com.zenty.tpe.utils.FileLogger.log(TAG, msg);
                
                PalmDeviceManager.this.device = device;
                
                // IMPORTANT: Ouvrir le device dans un thread en arrière-plan
                // comme dans le projet qui fonctionne (VP930Pro-main)
                singleThreadExecutor.execute(() -> {
                    if (deviceStateCallback != null) {
                        mainHandler.post(() -> deviceStateCallback.onDeviceCreated());
                    }
                });
            }

            @Override
            public void onDeviceCreateFailed(IDevice device) {
                String msg = "Device creation failed";
                Log.e(TAG, msg);
                com.zenty.tpe.utils.FileLogger.log(TAG, msg);
                
                if (deviceStateCallback != null) {
                    deviceStateCallback.onDeviceError("Échec de création du device");
                }
            }

            @Override
            public void onDeviceDestroy(IDevice device) {
                String msg = "Device destroyed";
                Log.i(TAG, msg);
                com.zenty.tpe.utils.FileLogger.log(TAG, msg);
                
                PalmDeviceManager.this.device = null;
                isDeviceOpen = false;
                isAlgorithmEnabled = false;
                if (deviceStateCallback != null) {
                    deviceStateCallback.onDeviceDestroyed();
                }
            }
        }, new DtUsbManager.DeviceStateListener() {
            @Override
            public void onDevicePermissionGranted(DtUsbDevice dtUsbDevice) {
                Log.d(TAG, "USB permission granted");
                com.zenty.tpe.utils.FileLogger.log(TAG, "USB permission granted");
            }

            @Override
            public void onDevicePermissionDenied(DtUsbDevice dtUsbDevice) {
                Log.e(TAG, "USB permission denied");
                com.zenty.tpe.utils.FileLogger.log(TAG, "USB permission denied");
                
                if (deviceStateCallback != null) {
                    deviceStateCallback.onDeviceError("Permission USB refusée");
                }
            }

            @Override
            public void onAttached(DtUsbDevice dtUsbDevice) {
                Log.i(TAG, "Device attached");
                com.zenty.tpe.utils.FileLogger.log(TAG, "Device attached: " + (dtUsbDevice != null ? dtUsbDevice.toString() : "null"));
                
                if (deviceStateCallback != null) {
                    deviceStateCallback.onDeviceAttached();
                }
            }

            @Override
            public void onDetached(DtUsbDevice dtUsbDevice) {
                Log.i(TAG, "Device detached");
                com.zenty.tpe.utils.FileLogger.log(TAG, "Device detached");
                
                isDeviceOpen = false;
                isAlgorithmEnabled = false;
                if (deviceStateCallback != null) {
                    deviceStateCallback.onDeviceDetached();
                }
            }
        });
    }
    
    /**
     * Ouvre le device
     */
    public void openDevice(final OpenCallback callback) {
        if (device == null) {
            Log.e(TAG, "Device not created yet");
            com.zenty.tpe.utils.FileLogger.log(TAG, "openDevice: Device not created yet");
            if (callback != null) {
                mainHandler.post(() -> callback.onOpenFailed("Device non initialisé"));
            }
            return;
        }
        
        com.zenty.tpe.utils.FileLogger.log(TAG, "Opening device in background thread...");
        
        // Ouvrir le device dans un thread en arrière-plan
        singleThreadExecutor.execute(() -> {
            device.open(new IOpenCallback() {
                @Override
                public void onDownloadPrepare() {
                    Log.d(TAG, "Download prepare");
                }

                @Override
                public void onDownloadProgress(int progress) {
                    Log.d(TAG, "Download progress: " + progress);
                }

                @Override
                public void onDownloadSuccess() {
                    Log.d(TAG, "Download success");
                }

                @Override
                public void onOpenSuccess() {
                    Log.i(TAG, "Device opened successfully");
                    com.zenty.tpe.utils.FileLogger.log(TAG, "Device opened successfully");
                    isDeviceOpen = true;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onOpenSuccess());
                    }
                }

                @Override
                public void onOpenFail(int errorCode) {
                    String errorMsg = "Device open failed: " + Integer.toHexString(errorCode);
                    Log.e(TAG, errorMsg);
                    com.zenty.tpe.utils.FileLogger.log(TAG, errorMsg);
                    isDeviceOpen = false;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onOpenFailed("Erreur: " + Integer.toHexString(errorCode)));
                    }
                }
            });
        });
    }
    
    /**
     * Active le module d'algorithme palmaire
     */
    public void enableAlgorithm(String modelPath, final AlgorithmCallback callback) {
        if (!isDeviceOpen) {
            Log.e(TAG, "Device not opened");
            if (callback != null) {
                callback.onAlgorithmEnableFailed("Device non ouvert");
            }
            return;
        }
        
        if (device instanceof IVeinshine) {
            IVeinshine veinshine = (IVeinshine) device;
            
            // Exécuter dans un thread séparé car c'est une opération bloquante
            new Thread(() -> {
                int ret = veinshine.enableDimPalm(modelPath);
                if (ret == 0) {
                    Log.i(TAG, "Algorithm enabled successfully");
                    Log.d(TAG, "Algorithm version: " + veinshine.getAlgorithmVersion());
                    isAlgorithmEnabled = true;
                    if (callback != null) {
                        callback.onAlgorithmEnabled();
                    }
                } else {
                    Log.e(TAG, "Algorithm enable failed: " + ret);
                    isAlgorithmEnabled = false;
                    if (callback != null) {
                        callback.onAlgorithmEnableFailed("Erreur: " + Integer.toHexString(ret));
                    }
                }
            }).start();
        } else {
            if (callback != null) {
                callback.onAlgorithmEnableFailed("Device incompatible");
            }
        }
    }
    
    /**
     * Ferme le device
     */
    public void closeDevice() {
        if (device != null) {
            try {
                device.close();
                Log.i(TAG, "Device closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing device", e);
            }
        }
        isDeviceOpen = false;
        isAlgorithmEnabled = false;
    }
    
    // Getters
    public IDevice getDevice() {
        return device;
    }
    
    public boolean isDeviceOpen() {
        return isDeviceOpen;
    }
    
    public boolean isAlgorithmEnabled() {
        return isAlgorithmEnabled;
    }
    
    /** Callbacks */
    
    public interface DeviceStateCallback {
        void onDeviceCreated();
        void onDeviceDestroyed();
        void onDeviceAttached();
        void onDeviceDetached();
        void onDeviceError(String error);
    }
    
    public interface OpenCallback {
        void onOpenSuccess();
        void onOpenFailed(String error);
    }
    
    public interface AlgorithmCallback {
        void onAlgorithmEnabled();
        void onAlgorithmEnableFailed(String error);
    }
}
