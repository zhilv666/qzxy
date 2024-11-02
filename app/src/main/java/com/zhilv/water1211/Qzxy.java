package com.zhilv.water1211;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Qzxy {
    private String username;
    private String password;
    static String tag = "zhilv-->";
    public String errorMsg = "";
    private final OkHttpClient client;
    public String cookie;
    public Map<String, String> rateMap = new HashMap<>();
    private final String LOGIN = "https://v3-api.china-qzxy.cn/user/login";
    private final String RATE = "https://v3-api.china-qzxy.cn/order/tcpDevice/downRate/rateOrder";

    public Qzxy(String _user, String _pass) {
        this.username = _user;
        this.password = _pass;
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .build();
    }

    public Qzxy() {
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .build();
    }

    public String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            stringBuilder.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append('&');
        }
        // 移除末尾的 '&'
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public Map<String, String> stringToMap(String input) {
        Map<String, String> map = new HashMap<>();
        String[] keyValuePairs = input.split("&");
        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=", 2); // 限制为 2 次分割，以处理包含 '=' 的值
            if (entry.length == 2) {
                map.put(entry[0], entry[1]);
            }
        }
        return map;
    }

    public static String md5(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(plainText.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(hexString.toString().length()-10).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有找到MD5算法", e);
        }
    }

    public boolean login() throws IOException, JSONException {
        RequestBody postData = new FormBody.Builder()
                .add("identifier", "")
                .add("password", md5(this.password))
                .add("phoneSystem", "android")
                .add("telephone", this.username)
                .add("type", "0")
                .add("version", "6.5.06")
                .build();
        Request request = new Request.Builder()
                .url(LOGIN)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:81.0) Gecko/81.0 Firefox/81.0")
                .post(postData)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                Log.d(tag, body);
                JSONObject jsonObject = new JSONObject(body);
                if (!"成功".equals(jsonObject.getString("errorMessage"))) {
                    this.errorMsg = jsonObject.getString("errorMessage");
                    return false;
                }
                JSONObject data = jsonObject.getJSONObject("data");
                rateMap.put("accountId", String.valueOf(data.getJSONObject("userAccount").getInt("accountId")));
                rateMap.put("loginCode", data.getString("loginCode"));
                rateMap.put("phoneSystem", "Alipay");
                rateMap.put("projectId", String.valueOf(data.getJSONObject("userAccount").getInt("projectId")));
                rateMap.put("telephone", data.getString("telephone"));
                rateMap.put("userId", String.valueOf(data.getInt("userId")));
                rateMap.put("version", "6.5.06");

                List<String> cookies = response.headers("Set-Cookie");
                for (String cookie : cookies) {
                    if (cookie.startsWith("acw")) {
                        this.cookie = cookie.split(";")[0];
                        Log.d(tag, this.cookie);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            this.errorMsg = e.toString();
        }
        return false;
    }

    public boolean rate() throws IOException {
        FormBody.Builder postData = new FormBody.Builder();
        for (Map.Entry<String, String> entry : this.rateMap.entrySet()) {
            postData.add(entry.getKey(), entry.getValue());
        }
        Log.d(tag, postData.toString());
        Request request = new Request.Builder()
                .url(RATE)
                .addHeader("Cookie", this.cookie)
                .post(postData.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                Log.d(tag, body);
                return true; // 表示成功
            }
        }
        return false;
    }
}
