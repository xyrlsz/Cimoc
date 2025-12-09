package com.xyrlsz.xcimoc.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_EXP;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_PASSWD_MD5;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_TOKEN;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_UID;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_USERNAME;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ZaiManhuaSignUtils {

    public static void CheckSigned(Context context, CheckSignCallback callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
        Request request = new Request.Builder()
                .url("https://m.zaimanhua.com/lpi/v1/task/list?_v=15")
                .addHeader("Authorization", "Bearer " + sharedPreferences.getString(ZAI_SHARED_TOKEN, ""))
                .addHeader("Referer", "https://m.zaimanhua.com/pages/signIn/index?from=app")
                .build();
        App.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject signInfo = jsonObject.getJSONObject("data").getJSONObject("task").getJSONObject("signInfo");
                    boolean isSigned = signInfo.getBoolean("currentSign");
                    callback.onCheckSign(isSigned);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 登录
    public static void Login(Context context, LoginCallback callback, String username, String password) {
        // 密码md5加密
        String passwdMd5 = HashUtils.MD5(password);
        LoginWithPasswdMd5(context, callback, username, passwdMd5);
    }

    public static void LoginWithPasswdMd5(Context context, LoginCallback callback, String username, String passwordMd5) {

        String url = StringUtils.format("https://i.zaimanhua.com/lpi/v1/login/passwd?username=%s&passwd=%s", username, passwordMd5);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://i.zaimanhua.com/login")
                .post(RequestBody.create("", null))// 空的请求体
                .build();
        Objects.requireNonNull(App.getHttpClient()).newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONObject data = jsonObject.getJSONObject("data");
                        JSONObject user = data.getJSONObject("user");
                        String uid = user.getLong("uid") + "";
                        String token = user.getString("token");
                        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(ZAI_SHARED_TOKEN, token);
                        editor.putString(ZAI_SHARED_UID, uid);
                        editor.putString(ZAI_SHARED_USERNAME, username);
                        editor.putString(ZAI_SHARED_PASSWD_MD5, passwordMd5);
                        JwtManualParser jwtManualParser = new JwtManualParser(token);
                        JSONObject payload = jwtManualParser.getPayload();
                        long exp = payload.getLong("exp");
                        editor.putLong(ZAI_SHARED_EXP, exp);
                        editor.apply();
                        callback.onSuccess();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onFail();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFail();
            }
        });
    }

    // 签到
    public static void SignIn(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
        String token = sharedPreferences.getString(Constants.ZAI_SHARED_TOKEN, "");
        Request request = new Request.Builder()
                .url("https://m.zaimanhua.com/lpi/v1/task/sign_in?_v=15")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Referer", "https://m.zaimanhua.com/pages/signIn/index?from=app")
                .post(RequestBody.create("", null))// 空的请求体
                .build();

        App.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                HintUtils.showToast(context, "再漫画签到失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    HintUtils.showToast(context, "再漫画签到成功");
                }
            }
        });


    }

    public interface CheckSignCallback {
        void onCheckSign(boolean isSigned);
    }

    public interface LoginCallback {
        void onSuccess();

        void onFail();
    }

}
