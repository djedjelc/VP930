package com.zenty.tpe.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.zenty.tpe.R;
import com.zenty.tpe.palm.PalmDeviceManager;
import com.zenty.tpe.utils.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Activity principale - Menu
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String MODEL_DIR = Environment.getExternalStorageDirectory() + File.separator + "ZentyModels";
    
    private TextView tvDeviceStatus;
    private Button btnEnroll, btnPayment, btnViewLogs;
    private PalmDeviceManager palmManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        btnEnroll = findViewById(R.id.btn_enroll);
        btnPayment = findViewById(R.id.btn_payment);
        btnViewLogs = findViewById(R.id.btn_view_logs);
        
        // Bouton pour voir les logs
        btnViewLogs.setOnClickListener(v -> showLogs());
        
        checkPermissions();
    }
    
    private void checkPermissions() {
        String[] permissions = new String[]{
            Permission.CAMERA,
            Permission.MANAGE_EXTERNAL_STORAGE
        };
        
        XXPermissions.with(this)
            .permission(permissions)
            .request(new OnPermissionCallback() {
                @Override
                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                    if (allGranted) {
                        prepareModelFiles();
                        initializeDevice();
                    } else {
                        Toast.makeText(MainActivity.this, "Permissions manquantes", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                    Toast.makeText(MainActivity.this, "Permissions refusées", Toast.LENGTH_SHORT).show();
                    if (doNotAskAgain) {
                        XXPermissions.startPermissionActivity(MainActivity.this, permissions);
                    }
                }
            });
    }
    
    private void prepareModelFiles() {
        // Créer le répertoire pour les modèles d'algorithme
        File modelDir = new File(MODEL_DIR);
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        
        // Copier les fichiers modèles depuis assets
        FileUtils.copyModelsFromAssets(this, MODEL_DIR);
    }
    
    private void initializeDevice() {
        palmManager = PalmDeviceManager.getInstance();
        
        tvDeviceStatus.setText("Initialisation du terminal...");
        
        palmManager.initialize(this, new PalmDeviceManager.DeviceStateCallback() {
            @Override
            public void onDeviceCreated() {
                Log.d(TAG, "Device created, opening...");
                openDevice();
            }

            @Override
            public void onDeviceDestroyed() {
                runOnUiThread(() -> {
                    tvDeviceStatus.setText(R.string.device_disconnected);
                    btnEnroll.setEnabled(false);
                    btnPayment.setEnabled(false);
                });
            }

            @Override
            public void onDeviceAttached() {
                runOnUiThread(() -> tvDeviceStatus.setText("Terminal connecté"));
            }

            @Override
            public void onDeviceDetached() {
                runOnUiThread(() -> {
                    tvDeviceStatus.setText(R.string.device_disconnected);
                    btnEnroll.setEnabled(false);
                    btnPayment.setEnabled(false);
                });
            }

            @Override
            public void onDeviceError(String error) {
                runOnUiThread(() -> {
                    tvDeviceStatus.setText("Erreur: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void openDevice() {
        palmManager.openDevice(new PalmDeviceManager.OpenCallback() {
            @Override
            public void onOpenSuccess() {
                Log.d(TAG, "Device opened, enabling algorithm...");
                runOnUiThread(() -> tvDeviceStatus.setText("Initialisation de l'algorithme..."));
                enableAlgorithm();
            }

            @Override
            public void onOpenFailed(String error) {
                runOnUiThread(() -> {
                    tvDeviceStatus.setText("Erreur d'ouverture: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void enableAlgorithm() {
        palmManager.enableAlgorithm(MODEL_DIR + "/", new PalmDeviceManager.AlgorithmCallback() {
            @Override
            public void onAlgorithmEnabled() {
                Log.d(TAG, "Algorithm enabled, device ready");
                runOnUiThread(() -> {
                    tvDeviceStatus.setText(R.string.device_ready);
                    btnEnroll.setEnabled(true);
                    btnPayment.setEnabled(true);
                    setupButtons();
                });
            }

            @Override
            public void onAlgorithmEnableFailed(String error) {
                runOnUiThread(() -> {
                    tvDeviceStatus.setText("Erreur algorithme: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void setupButtons() {
        btnEnroll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PalmActivity.class);
            intent.putExtra("MODE", "ENROLLMENT");
            startActivity(intent);
        });
        
        btnPayment.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
            startActivity(intent);
        });
    }
    
    private void showLogs() {
        String logs = com.zenty.tpe.utils.FileLogger.getLogContent();
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Crash Logs");
        
        // Scroll view pour les logs longs
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        TextView tvLogs = new TextView(this);
        tvLogs.setText(logs);
        tvLogs.setPadding(20, 20, 20, 20);
        tvLogs.setTextIsSelectable(true);
        scrollView.addView(tvLogs);
        
        builder.setView(scrollView);
        builder.setPositiveButton("Fermer", null);
        builder.setNeutralButton("Effacer", (dialog, which) -> {
            com.zenty.tpe.utils.FileLogger.clearLogs();
            Toast.makeText(this, "Logs effacés", Toast.LENGTH_SHORT).show();
        });
        
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Note: On ne ferme pas le device ici car il peut être utilisé par d'autres activités
    }
}
