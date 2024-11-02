package com.zhilv.water1211;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeActivity extends AppCompatActivity {

    private MaterialButton logoutButton;
    private MaterialButton drinkingButton;
    private MaterialButton stopButton;
    private MaterialButton scanButton;
    private TextInputEditText macEditText;
    private SharedPreferences sharedPreferences;
    private Qzxy qzxy;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/zhilv666/apk/contents/announcement.json";
    private static final long ANNOUNCEMENT_CHECK_INTERVAL = 3600000; // 1 hour in milliseconds
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeComponents();
        setupClickListeners();

        // Start checking for announcements
        startAnnouncementChecker();
    }

    private void startAnnouncementChecker() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForAnnouncements();
                handler.postDelayed(this, ANNOUNCEMENT_CHECK_INTERVAL);
            }
        }, 0);
    }

    private void checkForAnnouncements() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(GITHUB_API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String content = jsonResponse.getString("content");
                    String decodedContent = new String(android.util.Base64.decode(content, android.util.Base64.DEFAULT));

                    JSONArray announcements = new JSONArray(decodedContent);
                    for (int i = 0; i < announcements.length(); i++) {
                        JSONObject announcement = announcements.getJSONObject(i);
                        if (announcement.getBoolean("show")) {
                            final String title = announcement.getString("title");
                            final String message = announcement.getString("message");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showAnnouncement(title, message);
                                }
                            });
                            break; // 只显示第一个设置为显示的公告
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showAnnouncement(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void initializeComponents() {
        qzxy = new Qzxy();
        Intent intent = getIntent();
        sharedPreferences = getPreferences(MODE_PRIVATE);

        qzxy.rateMap = qzxy.stringToMap(intent.getStringExtra("rateData"));
        qzxy.cookie = intent.getStringExtra("cookie");

        logoutButton = findViewById(R.id.logoutButton);
        drinkingButton = findViewById(R.id.drinkingButton);
        stopButton = findViewById(R.id.stopButton);
        scanButton = findViewById(R.id.scanButton);
        macEditText = findViewById(R.id.macEditText);

        String savedMac = sharedPreferences.getString("mac", "");
        macEditText.setText(savedMac);
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v ->  {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.putExtra("flag", "true");
            startActivity(intent);
            finish();
        });
        drinkingButton.setOnClickListener(v -> handleDrinkingClick());
        stopButton.setOnClickListener(v -> handleStopClick());
        scanButton.setOnClickListener(v -> initiateQRScan());
    }

    private void handleDrinkingClick() {
        new Thread(() -> {
            try {
                String snCode = macEditText.getText().toString();
                saveMAC(snCode);
                updateQzxyRateMap(snCode);
                boolean success = qzxy.rate();
                logOperationResult("Drinking", success);
            } catch (IOException e) {
                Log.e("zhilv-->", "Error during drinking operation", e);
            }
        }).start();
    }

    private void handleStopClick() {
        // 实现停水功能
        Toast.makeText(this, "停水功能尚未实现", Toast.LENGTH_SHORT).show();
    }

    private void initiateQRScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startQRScanner();
        }
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CustomCaptureActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("扫描MAC地址二维码");
        integrator.setCameraId(0);  // 设置使用后置摄像头
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "扫描已取消", Toast.LENGTH_LONG).show();
            } else {
                String[] split = result.getContents().split(",");
                macEditText.setText(split[split.length-1]);
                Toast.makeText(this, "扫描成功", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveMAC(String mac) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("mac", mac);
        editor.apply();
    }

    private void updateQzxyRateMap(String snCode) {
        qzxy.rateMap.put("snCode", snCode.replace(":", ""));
        Log.d("zhilv-->", qzxy.mapToString(qzxy.rateMap));
    }

    private void logOperationResult(String operation, boolean success) {
        if (success) {
            Log.d("zhilv-->", operation + " operation successful");
        } else {
            Log.e("zhilv-->", operation + " operation failed");
        }
    }
}