package com.zenty.tpe.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client API pour communiquer avec le backend Zenty
 * Version VP930Pro - Palm only (sans fonctionnalités CB)
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://zenty-ndjq.onrender.com";
    private static final String TERMINAL_UID = "TPE-9F78FFEF";
    private static final String API_KEY_TPE = "3a1d10811b911df58c06b43bbcffae2e505c40ea1b437fbf3244bd92442f97b5";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;
    
    public ApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("X-TPE-ID", TERMINAL_UID)
                    .addHeader("X-TPE-API-KEY", API_KEY_TPE)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                Log.d(TAG, "Request: " + request.method() + " " + request.url());
                okhttp3.Response response = chain.proceed(request);
                Log.d(TAG, "Response: " + response.code() + " for " + request.url());
                return response;
            })
            .build();
            
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Crée une session d'enrôlement
     */
    public void createEnrollmentSession(ApiCallback<SessionCreateResponse> callback) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("tpe_id", TERMINAL_UID);
        String json = gson.toJson(jsonBody);
        
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
            .url(BASE_URL + "/api/v1/sessions/enrollment")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "createEnrollmentSession failed", e);
                mainHandler.post(() -> callback.onFailure(new ApiError("Erreur réseau", e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    SessionCreateResponse resp = gson.fromJson(responseBody, SessionCreateResponse.class);
                    mainHandler.post(() -> callback.onSuccess(resp));
                } else {
                    mainHandler.post(() -> callback.onFailure(new ApiError("Erreur API", "Code: " + response.code())));
                }
            }
        });
    }
    
    /**
     * Vérifie le statut d'une session d'enrôlement
     */
    public void checkEnrollmentSession(String sessionId, ApiCallback<SessionStatusResponse> callback) {
        Request request = new Request.Builder()
            .url(BASE_URL + "/api/v1/sessions/" + sessionId + "/status")
            .get()
            .build();
            
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkEnrollmentSession failed", e);
                mainHandler.post(() -> callback.onFailure(new ApiError("Erreur réseau", e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    SessionStatusResponse statusResponse = gson.fromJson(responseBody, SessionStatusResponse.class);
                    mainHandler.post(() -> callback.onSuccess(statusResponse));
                } else {
                    mainHandler.post(() -> callback.onFailure(new ApiError("Erreur API", "Code: " + response.code())));
                }
            }
        });
    }
    
    /**
     * Enregistre l'empreinte palmaire pour un utilisateur
     */
    public void registerPalmForUser(String userId, String palmFeature, ApiCallback<EnrollResponse> callback) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("user_id", userId);
        jsonBody.addProperty("palm_feature", palmFeature);
        String json = gson.toJson(jsonBody);
        
        Log.d(TAG, "registerPalmForUser: userId=" + userId + ", feature length=" + palmFeature.length());
        
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request req = new Request.Builder()
            .url(BASE_URL + "/api/v1/palm/register-tpe")
            .post(body)
            .build();
            
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "registerPalmForUser failed", e);
                mainHandler.post(() -> callback.onFailure(new ApiError("Erreur réseau", e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "registerPalmForUser response code: " + response.code());
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    EnrollResponse r = gson.fromJson(responseBody, EnrollResponse.class);
                    mainHandler.post(() -> callback.onSuccess(r));
                } else {
                    mainHandler.post(() -> callback.onFailure(new ApiError("Erreur API", "Code: " + response.code())));
                }
            }
        });
    }
    
    /**
     * Identifie un utilisateur par sa feature palmaire
     */
    public void identifyUser(String palmFeature, ApiCallback<UserIdentificationResponse> callback) {
        PalmIdentifyRequest identifyRequest = new PalmIdentifyRequest(palmFeature);
        String json = gson.toJson(identifyRequest);
        
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request httpRequest = new Request.Builder()
            .url(BASE_URL + "/api/v1/palm/identify")
            .post(body)
            .build();
            
        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "identifyUser failed", e);
                mainHandler.post(() -> callback.onFailure(new ApiError("Erreur réseau", e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    UserIdentificationResponse idResponse = gson.fromJson(responseBody, UserIdentificationResponse.class);
                    mainHandler.post(() -> callback.onSuccess(idResponse));
                } else {
                    Log.e(TAG, "identifyUser API error: " + response.code());
                    mainHandler.post(() -> callback.onFailure(new ApiError("Erreur API", "Code: " + response.code())));
                }
            }
        });
    }
    
    /**
     * Traite un paiement par paume
     */
    public void processPayment(PaymentRequest request, ApiCallback<PaymentResponse> callback) {
        JsonObject obj = new JsonObject();
        obj.addProperty("user_id", request.userId);
        obj.addProperty("amount", request.amount.toPlainString());
        obj.addProperty("terminal_id", request.terminalId);
        if (request.sessionId != null) obj.addProperty("session_id", request.sessionId);
        String json = gson.toJson(obj);

        Log.d(TAG, "processPayment: " + json);
        
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request httpRequest = new Request.Builder()
            .url(BASE_URL + "/api/v1/payments")
            .post(body)
            .build();
            
        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "processPayment failed", e);
                mainHandler.post(() -> callback.onFailure(new ApiError("Erreur réseau", e.getMessage())));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    PaymentResponse paymentResponse = gson.fromJson(responseBody, PaymentResponse.class);
                    mainHandler.post(() -> callback.onSuccess(paymentResponse));
                } else {
                    mainHandler.post(() -> callback.onFailure(new ApiError("Erreur API", "Code: " + response.code())));
                }
            }
        });
    }

    /** Classes pour les requêtes et réponses API **/
    
    public static class SessionCreateResponse {
        @SerializedName("session_id")
        private String sessionId;
        @SerializedName("expire_at")
        private String expireAt;
        
        public String getSessionId() { return sessionId; }
        public String getExpireAt() { return expireAt; }
    }
    
    public static class SessionStatusResponse {
        private String status;
        @SerializedName("user_id")
        private String userId;
        
        public String getStatus() { return status; }
        public String getUserId() { return userId; }
    }
    
    public static class EnrollResponse {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class UserIdentificationResponse {
        @SerializedName("user_id")
        private String userId;
        @SerializedName("user_name")
        private String userName;
        
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
    }
    
    public static class PaymentRequest {
        private String userId;
        private BigDecimal amount;
        private String terminalId;
        private String sessionId;
        
        public PaymentRequest(String userId, BigDecimal amount, String terminalId, String sessionId) {
            this.userId = userId;
            this.amount = amount;
            this.terminalId = terminalId;
            this.sessionId = sessionId;
        }
    }
    
    public static class PaymentResponse {
        private boolean success;
        private String errorReason;
        private String userId;
        private String userName;
        
        public boolean isSuccessful() { return success; }
        public String getErrorReason() { return errorReason; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
    }
    
    private static class PalmIdentifyRequest {
        @SerializedName("palm_feature")
        private String palmFeature;

        public PalmIdentifyRequest(String palmFeature) {
            this.palmFeature = palmFeature;
        }
    }
    
    /** Interface callback générique */
    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onFailure(ApiError error);
    }

    /** Classe erreur */
    public static class ApiError {
        private String title;
        private String message;
        
        public ApiError(String title, String message) {
            this.title = title;
            this.message = message;
        }
        
        public String getTitle() { return title; }
        public String getMessage() { return message; }
    }
}
