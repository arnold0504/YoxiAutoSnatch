package com.arnold.yoxiautosnatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText etFare;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etFare = findViewById(R.id.etFare);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        SharedPreferences prefs = getSharedPreferences("yoxi", MODE_PRIVATE);
        int savedFare = prefs.getInt("min_fare", 300);
        etFare.setText(String.valueOf(savedFare));
        btnSave.setOnClickListener(v -> {
            String fareStr = etFare.getText().toString().trim();
            if (fareStr.isEmpty()) { Toast.makeText(this, "請輸入車資金額", Toast.LENGTH_SHORT).show(); return; }
            int fare = Integer.parseInt(fareStr);
            prefs.edit().putInt("min_fare", fare).apply();
            Toast.makeText(this, "已設定：車資 > $" + fare + " 自動搶單", Toast.LENGTH_SHORT).show();
        });
        btnAccessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAccessibilityEnabled()) {
            tvStatus.setText("已開啟，監控中...");
            tvStatus.setTextColor(0xFF00AA00);
        } else {
            tvStatus.setText("請開啟無障礙服務");
            tvStatus.setTextColor(0xFFCC0000);
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return s != null && s.contains(getPackageName());
        } catch (Exception e) { return false; }
    }
}
