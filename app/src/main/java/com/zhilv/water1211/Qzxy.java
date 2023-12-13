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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kotlin.sequences.Sequence;
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
    private final OkHttpClient client;
    public String cookie;
    public Map<String, String> rateMap = new HashMap<>();
    private final String LOGIN = "https://wfwapi.china-qzxy.cn/user/login";
    private final String RATE = "https://v3-api.china-qzxy.cn/order/tcpDevice/downRate/rateOrder";

    public String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            stringBuilder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }

        // 删除末尾的"&"
        if (stringBuilder.length() > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }

    public Map<String, String> stringToMap(String input) {
        Map<String, String> map = new HashMap<>();

        String[] keyValuePairs = input.split("&");

        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            if (entry.length == 2) {
                map.put(entry[0], entry[1]);
            }
        }

        return map;
    }
    //    private final String ALIPAYID = "https://v3-api.china-qzxy.cn/aliCard/signInfo";
//    private final String MACINFO = "https://v3-api.china-qzxy.cn/device/info/mac";
//    private final String DOWNRATERESULT = "https://v3-api.china-qzxy.cn/order/tcpDevice/query/downRateResult";
//    private final String CLOSE = "https://v3-api.china-qzxy.cn/order/tcpDevice/closeOrder";
//    private final String RESULT = "https://v3-api.china-qzxy.cn/order/consumeOrder/result/query";
//    private final String WALLET = "https://v3-api.china-qzxy.cn/account/wallet/my";
    public Qzxy(String _user, String _pass) {
        this.username = _user;
        this.password = _pass;
        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .build();
    }

    public Qzxy() {
        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .build();
    }

    public static String md5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code.substring(md5code.length() - 10).toUpperCase();
    }

    public boolean login() throws IOException, JSONException {
        RequestBody postData = new FormBody.Builder()
                .add("password", md5(this.password))
                .add("telPhone", this.username)
                .add("phoneSystem", "android")
                .add("openId", "")
                .add("typeId", "0")
                .add("version", "6.4.02")
                .build();
        Request request = new Request.Builder()
                .url(LOGIN)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:81.0) Gecko/81.0 Firefox/81.0")
                .post(postData)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String body = response.body().string();
            Log.d(tag, body);
            JSONObject jsonObject = new JSONObject(body);
            if (!jsonObject.getString("message").equals("成功")) {
                return false;
            }
            JSONObject data = jsonObject.getJSONObject("data");

            rateMap.put("accountId", String.valueOf(data.getInt("accountId")));
            rateMap.put("loginCode", data.getString("loginCode"));
            rateMap.put("phoneSystem", "Alipay");
            rateMap.put("projectId", String.valueOf(data.getInt("projectId")));
//            rateMap.put("snCode", "");
            rateMap.put("telPhone", data.getString("telPhone"));
            rateMap.put("userId", String.valueOf(data.getInt("v3UserId")));
            rateMap.put("version", "6.4.02");
//            Log.d(tag, String.valueOf(rateMap));
            List<String> cookies = response.headers().values("Set-Cookie");
            for (String cookie : cookies) {
                if (cookie.startsWith("acw")) {
                    this.cookie = cookie.split(";")[0];
                    Log.d(tag, this.cookie);
                    return true;
                }
            }
        }


        return false;
    }

    public boolean rate() throws IOException {
        FormBody.Builder postData = new FormBody.Builder();
        for (Map.Entry<String, String> entry: this.rateMap.entrySet()) {
            postData.add(entry.getKey(), entry.getValue());
        }
        Log.d(tag, postData.toString());
        Request request = new Request.Builder()
                .url(RATE)
                .addHeader("Cookie", this.cookie)
                .post(postData.build())
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()){
            String body = response.body().string();
            Log.d(tag, body);
        }
        return false;
    }
}
