package com.haleydu.cimoc.utils;

import static com.haleydu.cimoc.Constants.KOMIIC_SHARED_COOKIES;
import static com.haleydu.cimoc.Constants.KOMIIC_SHARED_EXPIRED;

import android.content.Context;
import android.content.SharedPreferences;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KomiicUtils {
    public static boolean checkExpired() {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(Constants.KOMIIC_SHARED_USERNAME, "");
        Long expired = sharedPreferences.getLong(Constants.KOMIIC_SHARED_EXPIRED, -1L);
        if (!username.isEmpty() && expired != -1L) {
            Long now = System.currentTimeMillis() / 1000;
            return now >= expired;
        }
        return false;
    }

    public static void refresh() {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE);
        String cookies = sharedPreferences.getString(KOMIIC_SHARED_COOKIES, "");
        Request request = new Request.Builder()
                .url("https://komiic.com/auth/refresh")
                .post(RequestBody.create(MediaType.get("application/json"), ""))// 空的请求体
                .addHeader("accept", "*/*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/json")
                .addHeader("pragma", "no-cache")
                .addHeader("priority", "u=1, i")
                .addHeader("sec-ch-ua", "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("cookie", cookies)
                .addHeader("Referer", "https://komiic.com/")
                .addHeader("Referrer-Policy", "strict-origin-when-cross-origin")
                .build();

        try {

            App.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<String> cookies = response.headers("Set-Cookie");
                    if (response.isSuccessful() && response.body() != null && !cookies.isEmpty()) {
                        Set<String> set = new HashSet<>();
                        for (String s : cookies) {
                            List<String> tmp = Arrays.asList(s.split("; "));
                            set.addAll(tmp);
                        }
                        Long expired = -1L;
                        try {
                            JSONObject data = new JSONObject(response.body().string());
                            String iso8601String = data.getString("expire");
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            try {
                                Date date = dateFormat.parse(iso8601String);
                                if (date != null) {
                                    expired = date.getTime() / 1000;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String cookieStr = String.join("; ", set);
                        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KOMIIC_SHARED_COOKIES, cookieStr);
                        editor.putLong(KOMIIC_SHARED_EXPIRED, expired);
                        editor.apply();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String FormatTime(String t) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date date = inputFormat.parse(t);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return t;
    }
}
