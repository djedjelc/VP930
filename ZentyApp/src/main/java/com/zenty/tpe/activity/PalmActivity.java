package com.zenty.tpe.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.api.stream.ICapturePalmCallback;
import com.api.stream.bean.CaptureFrame;
import com.api.stream.enumclass.Hint;
import com.api.stream.veinshine.IVeinshine;
import com.palm.common.opengl.GLDisplay;
import com.palm.common.opengl.GLFrameSurface;
import com.zenty.tpe.R;
import com.zenty.tpe.api.ApiClient;
import com.zenty.tpe.palm.PalmDeviceManager;
import com.zenty.tpe.utils.QRCodeGenerator;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Activity principale pour la capture de paume
 * Gère l'enrôlement et l'identification
 */
public class PalmActivity extends AppCompatActivity {
    private static final String TAG = "PalmActivity";
    private static final int CAPTURE_TIMEOUT = 15000; // 15 secondes
    
    private GLFrameSurface glRgbView, glIrView;
    private GLDisplay rgbDisplay, irDisplay;
    private ImageView ivQrCode;
    private TextView tvStatus;
    private Button btnScanAgain;
    
    private PalmDeviceManager palmManager;
    private ApiClient apiClient;
    
    private String mode; // "ENROLLMENT" ou "IDENTIFICATION"
    private String sessionId;
    private BigDecimal paymentAmount;
    private boolean isCapturing = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palm);
        
        glRgbView = findViewById(R.id.gl_rgb);
        glIrView = findViewById(R.id.gl_ir);
        ivQrCode = findViewById(R.id.iv_qr_code);
        tvStatus = findViewById(R.id.tv_status);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        
        rgbDisplay = new GLDisplay();
        irDisplay = new GLDisplay();
        
        palmManager = PalmDeviceManager.getInstance();
        apiClient = new ApiClient();
        
        // Récupérer les paramètres
        mode = getIntent().getStringExtra("MODE");
        String amountStr = getIntent().getStringExtra("AMOUNT");
        if (amountStr != null) {
            paymentAmount = new BigDecimal(amountStr);
        }
        
        if ("ENROLLMENT".equals(mode)) {
            startEnrollmentFlow();
        } else {
            // Mode identification (pour paiement)
            startIdentificationFlow();
        }
        
        btnScanAgain.setOnClickListener(v -> {
            btnScanAgain.setVisibility(View.GONE);
            if ("ENROLLMENT".equals(mode)) {
                startEnrollmentFlow();
            } else {
                startIdentificationFlow();
            }
        });
    }
    
    /**
     * Flux d'enrôlement : Affiche QR code puis capture
     */
    private void startEnrollmentFlow() {
        tvStatus.setText("Création de la session d'enrôlement...");
        
        apiClient.createEnrollmentSession(new ApiClient.ApiCallback<ApiClient.SessionCreateResponse>() {
            @Override
            public void onSuccess(ApiClient.SessionCreateResponse response) {
                sessionId = response.getSessionId();
                Log.d(TAG, "Session created: " + sessionId);
                displayQRCode();
                startPollingSessionStatus();
            }

            @Override
            public void onFailure(ApiClient.ApiError error) {
                Log.e(TAG, "Failed to create session: " + error.getMessage());
                runOnUiThread(() -> {
                    tvStatus.setText("Erreur: " + error.getMessage());
                    Toast.makeText(PalmActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Affiche le QR code pour l'enrôlement
     */
    private void displayQRCode() {
        String enrollmentUrl = "https://frontend-zenty.vercel.app/enregistrement?session_id=" + sessionId;
        Bitmap qrBitmap = QRCodeGenerator.generate(enrollmentUrl);
        
        runOnUiThread(() -> {
            ivQrCode.setImageBitmap(qrBitmap);
            ivQrCode.setVisibility(View.VISIBLE);
            glRgbView.setVisibility(View.GONE);
            glIrView.setVisibility(View.GONE);
            tvStatus.setText(R.string.scan_qr_code);
        });
    }
    
    /**
     * Polling du statut de la session d'enrôlement
     */
    private void startPollingSessionStatus() {
        final Handler handler = new Handler();
        final Runnable[] pollingRunnable = new Runnable[1];
        
        pollingRunnable[0] = () -> {
            apiClient.checkEnrollmentSession(sessionId, new ApiClient.ApiCallback<ApiClient.SessionStatusResponse>() {
                @Override
                public void onSuccess(ApiClient.SessionStatusResponse response) {
                    if ("completed".equalsIgnoreCase(response.getStatus())) {
                        // Session validée, on peut capturer la paume
                        String userId = response.getUserId();
                        Log.d(TAG, "Session validated for user: " + userId);
                        runOnUiThread(() -> {
                            ivQrCode.setVisibility(View.GONE);
                            glRgbView.setVisibility(View.VISIBLE);
                            glIrView.setVisibility(View.VISIBLE);
                            tvStatus.setText(R.string.place_palm);
                        });
                        startPalmCapture(userId);
                    } else if ("expired".equalsIgnoreCase(response.getStatus())) {
                        runOnUiThread(() -> {
                            tvStatus.setText(R.string.session_expired);
                            btnScanAgain.setVisibility(View.VISIBLE);
                        });
                    } else {
                        // Continuer le polling
                        handler.postDelayed(pollingRunnable[0], 2000);
                    }
                }

                @Override
                public void onFailure(ApiClient.ApiError error) {
                    Log.e(TAG, "Polling error: " + error.getMessage());
                    // Continuer le polling malgré l'erreur
                    handler.postDelayed(pollingRunnable[0], 2000);
                }
            });
        };
        
        handler.post(pollingRunnable[0]);
    }
    
    /**
     * Flux d'identification directe (pour paiement)
     */
    private void startIdentificationFlow() {
        runOnUiThread(() -> {
            ivQrCode.setVisibility(View.GONE);
            glRgbView.setVisibility(View.VISIBLE);
            glIrView.setVisibility(View.VISIBLE);
            if (paymentAmount != null) {
                tvStatus.setText(getString(R.string.amount_to_pay, paymentAmount.toString()));
            } else {
                tvStatus.setText(R.string.place_palm);
            }
        });
        
        startPalmCapture(null);
    }
    
    /**
     * Lance la capture de paume
     * @param userId Si non null, c'est un enrôlement pour cet utilisateur
     */
    private void startPalmCapture(final String userId) {
        if (!palmManager.isDeviceOpen() || !palmManager.isAlgorithmEnabled()) {
            Toast.makeText(this, "Terminal non prêt", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isCapturing) {
            Log.w(TAG, "Capture already in progress");
            return;
        }
        
        IVeinshine veinshine = (IVeinshine) palmManager.getDevice();
        isCapturing = true;
        
        int ret = veinshine.capturePalmOnce(new ICapturePalmCallback() {
            @Override
            public void onCaptureFrame(CaptureFrame frame) {
                Log.d(TAG, "Palm captured successfully");
                isCapturing = false;
                
                // Afficher les images RGB et IR
                if (frame.rgbData != null) {
                    runOnUiThread(() -> rgbDisplay.render(glRgbView, 0, false, 
                        frame.rgbData, frame.rgbCols, frame.rgbRows, 1));
                }
                if (frame.irData != null) {
                    runOnUiThread(() -> irDisplay.render(glIrView, 0, false, 
                        frame.irData, frame.irCols, frame.irRows, 2));
                }
                
                // Encoder les features en Base64
                String rgbFeatureB64 = Base64.encodeToString(frame.rgbFeature, Base64.NO_WRAP);
                String irFeatureB64 = Base64.encodeToString(frame.irFeature, Base64.NO_WRAP);
                String combinedFeature = rgbFeatureB64 + "|" + irFeatureB64; // Format simple
                
                if (userId != null) {
                    // Mode enrôlement
                    registerPalmForUser(userId, combinedFeature);
                } else {
                    // Mode identification
                    identifyUser(combinedFeature);
                }
            }

            @Override
            public void onCapturePalmHint(Hint hint, HashMap<Integer, Float> hashMap) {
                isCapturing = false;
                Log.d(TAG, "Capture hint: " + hint);
                
                runOnUiThread(() -> {
                    if (hint == Hint.TIMEOUT) {
                        tvStatus.setText("Capture expirée");
                        btnScanAgain.setVisibility(View.VISIBLE);
                    } else if (hint == Hint.NO_PALM_DETECTED) {
                        tvStatus.setText("Aucune paume détectée");
                    } else {
                        tvStatus.setText(hint.toString());
                    }
                });
            }
        }, CAPTURE_TIMEOUT, false);
        
        if (ret != 0) {
            Log.e(TAG, "Failed to start capture: " + Integer.toHexString(ret));
            isCapturing = false;
            Toast.makeText(this, "Erreur de capture: " + Integer.toHexString(ret), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Enregistre l'empreinte palmaire pour un utilisateur
     */
    private void registerPalmForUser(String userId, String palmFeature) {
        runOnUiThread(() -> tvStatus.setText("Enregistrement en cours..."));
        
        apiClient.registerPalmForUser(userId, palmFeature, new ApiClient.ApiCallback<ApiClient.EnrollResponse>() {
            @Override
            public void onSuccess(ApiClient.EnrollResponse response) {
                Log.d(TAG, "Palm registered successfully");
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.enrollment_success);
                    Toast.makeText(PalmActivity.this, R.string.enrollment_success, Toast.LENGTH_LONG).show();
                    
                    // Retour au menu après 2 secondes
                    new Handler().postDelayed(() -> finish(), 2000);
                });
            }

            @Override
            public void onFailure(ApiClient.ApiError error) {
                Log.e(TAG, "Failed to register palm: " + error.getMessage());
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.enrollment_failed);
                    Toast.makeText(PalmActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    btnScanAgain.setVisibility(View.VISIBLE);
                });
            }
        });
    }
    
    /**
     * Identifie l'utilisateur par sa paume
     */
    private void identifyUser(String palmFeature) {
        runOnUiThread(() -> tvStatus.setText("Identification en cours..."));
        
        apiClient.identifyUser(palmFeature, new ApiClient.ApiCallback<ApiClient.UserIdentificationResponse>() {
            @Override
            public void onSuccess(ApiClient.UserIdentificationResponse response) {
                Log.d(TAG, "User identified: " + response.getUserName());
                
                if (paymentAmount != null) {
                    // Lancer le paiement
                    processPayment(response.getUserId(), response.getUserName());
                } else {
                    // Simple identification
                    runOnUiThread(() -> {
                        tvStatus.setText("Bienvenue " + response.getUserName());
                        Toast.makeText(PalmActivity.this, 
                            getString(R.string.identification_success) + ": " + response.getUserName(), 
                            Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(() -> finish(), 2000);
                    });
                }
            }

            @Override
            public void onFailure(ApiClient.ApiError error) {
                Log.e(TAG, "Identification failed: " + error.getMessage());
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.identification_failed);
                    Toast.makeText(PalmActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    btnScanAgain.setVisibility(View.VISIBLE);
                });
            }
        });
    }
    
    /**
     * Traite le paiement après identification
     */
    private void processPayment(String userId, String userName) {
        runOnUiThread(() -> tvStatus.setText("Paiement en cours..."));
        
        String terminalId = android.provider.Settings.Secure.getString(
            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        
        ApiClient.PaymentRequest request = new ApiClient.PaymentRequest(
            userId, paymentAmount, terminalId, null);
        
        apiClient.processPayment(request, new ApiClient.ApiCallback<ApiClient.PaymentResponse>() {
            @Override
            public void onSuccess(ApiClient.PaymentResponse response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Payment successful");
                    runOnUiThread(() -> {
                        tvStatus.setText(R.string.payment_accepted);
                        Toast.makeText(PalmActivity.this, 
                            getString(R.string.payment_accepted) + " - Merci " + userName, 
                            Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(() -> finish(), 3000);
                    });
                } else {
                    runOnUiThread(() -> {
                        tvStatus.setText(R.string.payment_refused);
                        Toast.makeText(PalmActivity.this, 
                            response.getErrorReason(), 
                            Toast.LENGTH_LONG).show();
                        btnScanAgain.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onFailure(ApiClient.ApiError error) {
                Log.e(TAG, "Payment failed: " + error.getMessage());
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.payment_refused);
                    Toast.makeText(PalmActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    btnScanAgain.setVisibility(View.VISIBLE);
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rgbDisplay != null) {
            rgbDisplay.release();
        }
        if (irDisplay != null) {
            irDisplay.release();
        }
    }
}
