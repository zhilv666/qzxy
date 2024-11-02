package com.zhilv.water1211;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VersionChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/zhilv666/apk/releases/latest";
    private static final String DOWNLOAD_PROXY_URL = "https://github.5700.cf/"; // 添加GitHub下载的代理URL
    private Context context;
    private OkHttpClient client;

    public VersionChecker(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public interface VersionCheckListener {
        void onUpdateAvailable(String newVersion, String downloadUrl, String updateContent);
        void onNoUpdateAvailable();
        void onError(String error);
    }

    public void checkForUpdates(final VersionCheckListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Request request = new Request.Builder()
                        .url(GITHUB_API_URL)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    return response.body().string();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        JSONObject json = new JSONObject(result);
                        String latestVersion = json.getString("name");
                        String currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

                        if (isNewerVersion(latestVersion, currentVersion)) {
                            String downloadUrl = DOWNLOAD_PROXY_URL + json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"); // 使用代理URL
                            String updateContent = json.getString("body");
                            listener.onUpdateAvailable(latestVersion, downloadUrl, updateContent);
                        } else {
                            listener.onNoUpdateAvailable();
                        }
                    } catch (Exception e) {
                        listener.onError("解析版本信息时出错: " + e.getMessage());
                    }
                } else {
                    listener.onError("获取版本信息失败");
                }
            }
        }.execute();
    }
    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latest = latestVersion.split("\\.");
        String[] current = currentVersion.split("\\.");

        int length = Math.max(latest.length, current.length);

        for (int i = 0; i < length; i++) {
            int l = i < latest.length ? Integer.parseInt(latest[i]) : 0;
            int c = i < current.length ? Integer.parseInt(current[i]) : 0;

            if (l > c) {
                return true;
            } else if (l < c) {
                return false;
            }
        }

        return false; // 版本相同
    }
}