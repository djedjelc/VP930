package com.zenty.tpe.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zenty.tpe.R;

import java.math.BigDecimal;

/**
 * Activity pour saisir le montant du paiement
 * Puis lance PalmActivity en mode identification
 */
public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    
    private EditText etAmount;
    private Button btnValidate;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        
        etAmount = findViewById(R.id.et_amount);
        btnValidate = findViewById(R.id.btn_validate);
        tvStatus = findViewById(R.id.tv_status);
        
        btnValidate.setOnClickListener(v -> validateAndStartCapture());
    }
    
    private void validateAndStartCapture() {
        String amountStr = etAmount.getText().toString().trim();
        
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Veuillez saisir un montant", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                Toast.makeText(this, "Le montant doit être positif", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (amount.compareTo(new BigDecimal("10000")) > 0) {
                Toast.makeText(this, "Montant trop élevé", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Lancer PalmActivity en mode identification avec le montant
            Intent intent = new Intent(PaymentActivity.this, PalmActivity.class);
            intent.putExtra("MODE", "IDENTIFICATION");
            intent.putExtra("AMOUNT", amount.toPlainString());
            startActivity(intent);
            
            // Fermer cette activity
            finish();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format de montant invalide", Toast.LENGTH_SHORT).show();
        }
    }
}
