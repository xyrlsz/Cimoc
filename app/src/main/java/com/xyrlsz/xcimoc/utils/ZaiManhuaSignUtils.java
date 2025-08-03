package com.xyrlsz.xcimoc.utils;

import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_TOKEN;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class ZaiManhuaSignUtils {

    public static void CheckSigned(CheckSignCallback callback) {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.ZAI_SHARED, Context.MODE_PRIVATE);
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

    public static void sign() {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.ZAI_SHARED, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(Constants.ZAI_SHARED_TOKEN, "");
        Request request = new Request.Builder()
                .url("https://m.zaimanhua.com/lpi/v1/task/sign_in?_v=15")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Referer", "https://m.zaimanhua.com/pages/signIn/index?from=app")
                .post(new RequestBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return null;
                    }

                    @Override
                    public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {

                    }
                })
                .build();

        App.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    App.runOnMainThread(() -> {
                        HintUtils.showToast(App.getAppContext(), "再漫画签到成功");
                    });
                }
            }
        });


    }

    public interface CheckSignCallback {
        void onCheckSign(boolean isSigned);
    }

}
