package com.zhilv.water1211;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "water";
    private static final String FROM_HOME_ACTIVITY = "fromHomeActivity";

    private Button loginButton;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private SharedPreferences sharedPreferences;
    private Qzxy qzxy;
    private VersionChecker versionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupFooter();
        loadSavedCredentials();
        setupLoginButton();
        setupVersionChecker();
    }

    private void initializeViews() {
        loginButton = findViewById(R.id.loginButton);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        sharedPreferences = getPreferences(MODE_PRIVATE);
    }

    private void setupFooter() {
        TextView footerText = findViewById(R.id.footerTextView);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            String footer = getString(R.string.footer_text) + "\n" + getString(R.string.app_version, versionName);
            footerText.setText(footer);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadSavedCredentials() {
        String savedUsername = sharedPreferences.getString("username", "");
        String savedPassword = sharedPreferences.getString("password", "");
        usernameEditText.setText(savedUsername);
        passwordEditText.setText(savedPassword);
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void setupVersionChecker() {
        versionChecker = new VersionChecker(this);
        checkVersion();
    }

    private boolean isFromHomeActivity() {
        return "true".equals(getIntent().getStringExtra("flag"));
    }

    private void checkVersion() {
        versionChecker.checkForUpdates(new VersionChecker.VersionCheckListener() {
            @Override
            public void onUpdateAvailable(String newVersion, String downloadUrl, String updateContent) {
                showUpdateDialog(newVersion, downloadUrl, updateContent);
            }

            @Override
            public void onNoUpdateAvailable() {
                if (!isFromHomeActivity()) {
                    AutoLogin();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "版本检查失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateDialog(final String newVersion, final String downloadUrl, final String updateContent) {
        new AlertDialog.Builder(this)
                .setTitle("新版本可用")
                .setMessage("发现新版本 " + newVersion + "\n\n更新内容：\n" + updateContent)
                .setPositiveButton("更新", (dialog, which) -> downloadAndInstallUpdate(downloadUrl))
                .setNegativeButton("稍后", null)
                .show();
    }

    private void downloadAndInstallUpdate(final String downloadUrl) {
        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("正在下载更新...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(downloadUrl).build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException("Failed to download file: " + response);
                    }
                    InputStream inputStream = response.body().byteStream();
                    long totalSize = response.body().contentLength();
                    long downloadedSize = 0;
                    byte[] buffer = new byte[4096];
                    int read;

                    String fileName = "update.apk";
                    File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                    FileOutputStream fos = new FileOutputStream(outputFile);

                    while ((read = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloadedSize += read;
                        publishProgress((int) ((downloadedSize * 100) / totalSize));
                    }
                    fos.flush();
                    fos.close();
                    inputStream.close();

                    return outputFile.getPath();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(values[0]);
            }

            @Override
            protected void onPreExecute() {
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(String result) {
                progressDialog.dismiss();
                if (result != null) {
                    installUpdate(result);
                } else {
                    Toast.makeText(LoginActivity.this, "下载更新失败", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void installUpdate(String filePath) {
        File file = new File(filePath);
        Uri apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            onLoginFailure("用户名和密码不能为空");
            return;
        }

        saveCredentials(username, password);
        performLogin(username, password);
    }

    private void saveCredentials(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.apply();
    }

    private void performLogin(final String username, final String password) {
        qzxy = new Qzxy(username, password);
        new Thread(() -> {
            try {
                if (qzxy.login()) {
                    onLoginSuccess();
                } else {
                    onLoginFailure("登录失败: " + qzxy.errorMsg);
                }
            } catch (IOException e) {
                onLoginFailure("网络错误");
            } catch (JSONException e) {
                onLoginFailure("数据解析错误");
            }
        }).start();
    }

    private void onLoginSuccess() {
        runOnUiThread(() -> {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.putExtra("rateData", qzxy.mapToString(qzxy.rateMap));
            intent.putExtra("cookie", qzxy.cookie);
            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_LONG).show();
            startActivity(intent);
            finish();
        });
    }

    private void onLoginFailure(final String errorMessage) {
        runOnUiThread(() -> Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show());
    }

    private void AutoLogin() {
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");
        if (!username.isEmpty() && !password.isEmpty()) {
            handleLogin();
        }
    }
}