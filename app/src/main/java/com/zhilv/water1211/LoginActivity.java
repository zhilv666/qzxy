package com.zhilv.water1211;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;

public class LoginActivity extends AppCompatActivity {

    private Button button;
    private TextView user;
    private TextView pass;
    private SharedPreferences sharedPreferences;
    private String Tag = "water";
    public Qzxy qzxy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 获取SharedPreferences实例
        sharedPreferences = getPreferences(LoginActivity.MODE_PRIVATE);

        // 获取标签
        button = findViewById(R.id.login);
        user = findViewById(R.id.username);
        pass = findViewById(R.id.password);
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        // 如果有已保存的账号密码，显示在EditText中
        String userName = sharedPreferences.getString("username", "");
        String passWord = sharedPreferences.getString("password", "");
        user.setText(userName);
        pass.setText(passWord);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 保存到SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("username", user.getText().toString());
                editor.putString("password", pass.getText().toString());
                editor.apply();
                qzxy = new Qzxy(user.getText().toString(), pass.getText().toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (qzxy.login()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_LONG).show();
                                        intent.putExtra("rateData", qzxy.mapToString(qzxy.rateMap));
                                        intent.putExtra("cookie", qzxy.cookie);
                                        startActivity(intent);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });
        if (!userName.equals("") && !passWord.equals("")) {
            button.callOnClick();
        }

    }
}