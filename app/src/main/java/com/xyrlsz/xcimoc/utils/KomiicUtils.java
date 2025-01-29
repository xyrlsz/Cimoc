package com.xyrlsz.xcimoc.utils;

import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED_COOKIES;
import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED_EXPIRED;

import android.content.Context;
import android.content.SharedPreferences;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;

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
                            String date = data.getString("expire");
                            expired = KomiicUtils.toTimestamp(date);
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

    public static void getImageLimit(UpdateImageLimitCallback callback) {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE);
        String cookies = sharedPreferences.getString(KOMIIC_SHARED_COOKIES, "");
        getImageLimit(cookies, callback);
    }

    public static void getImageLimit(String cookies, UpdateImageLimitCallback callback) {
        String json = "{\"operationName\":\"getImageLimit\",\"variables\":{},\"query\":\"query getImageLimit {\\n  getImageLimit {\\n    limit\\n    usage\\n    resetInSeconds\\n    __typename\\n  }\\n}\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .url("https://komiic.com/api/query")
                .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("origin", "https://komiic.com")
                .addHeader("pragma", "no-cache")
                .addHeader("priority", "u=1, i")
                .addHeader("referer", "https://komiic.com/login")
                .addHeader("sec-ch-ua", "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("cookie", cookies)
                .post(body)
                .build();
        try {
            App.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject data;
                        try {
                            String json = response.body().string();
                            data = new JSONObject(json).getJSONObject("data");
                            int limit = data.getJSONObject("getImageLimit").getInt("limit");
                            int usage = data.getJSONObject("getImageLimit").getInt("usage");
                            usage = Math.max(usage - 1, 0);
                            int res = limit - usage;
                            callback.onSuccess(Math.max(res, 0));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkImgLimit(String cookies) {
        String json = "{\"operationName\":\"getImageLimit\",\"variables\":{},\"query\":\"query getImageLimit {\\n  getImageLimit {\\n    limit\\n    usage\\n    resetInSeconds\\n    __typename\\n  }\\n}\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .url("https://komiic.com/api/query")
                .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("origin", "https://komiic.com")
                .addHeader("pragma", "no-cache")
                .addHeader("priority", "u=1, i")
                .addHeader("referer", "https://komiic.com/login")
                .addHeader("sec-ch-ua", "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("cookie", cookies)
                .post(body)
                .build();
        try {
            Response response = App.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONObject data;
                try {
                    String respJson = response.body().string();
                    data = new JSONObject(respJson).getJSONObject("data");
                    int limit = data.getJSONObject("getImageLimit").getInt("limit");
                    int usage = data.getJSONObject("getImageLimit").getInt("usage");
                    return limit - usage <= 0;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkIsOverImgLimit() {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE);
        String cookies = sharedPreferences.getString(KOMIIC_SHARED_COOKIES, "");
        return checkImgLimit(cookies);
    }

    public static boolean checkEmptyAccountIsOverImgLimit() {
        return checkImgLimit("");
    }

    public static Long toTimestamp(String t) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long timestamp = 0;
        try {
            Date date = dateFormat.parse(t);
            timestamp = date.getTime() / 1000;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    public interface UpdateImageLimitCallback {
        void onSuccess(int result);
    }
}
