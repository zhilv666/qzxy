package com.zhilv.water1211;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private Button logout;
    private Button drinking;
    private Button stop;
    private EditText mac;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Qzxy qzxy = new Qzxy();

        Intent intent = getIntent();
        sharedPreferences = getPreferences(HomeActivity.MODE_PRIVATE);
        String m = sharedPreferences.getString("mac", "");

        qzxy.rateMap = qzxy.stringToMap(intent.getStringExtra("rateData"));
        qzxy.cookie = intent.getStringExtra("cookie");
        logout = findViewById(R.id.logout);
        drinking = findViewById(R.id.drinking);
        stop = findViewById(R.id.stop);
        mac = findViewById(R.id.mac);

        mac.setText(m);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        drinking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            String snCode = mac.getText().toString();
                            editor.putString("mac", snCode);
                            editor.apply();
                            qzxy.rateMap.put("snCode", snCode.replace(":", ""));
                            Log.d("zhilv-->", qzxy.mapToString(qzxy.rateMap));
                            qzxy.rate();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });

    }
}