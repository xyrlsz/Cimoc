package com.xyrlsz.xcimoc.ui.activity;

import static com.xyrlsz.xcimoc.Constants.DMZJ_SHARED;
import static com.xyrlsz.xcimoc.Constants.DMZJ_SHARED_COOKIES;
import static com.xyrlsz.xcimoc.Constants.DMZJ_SHARED_UID;
import static com.xyrlsz.xcimoc.Constants.DMZJ_SHARED_USERNAME;
import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED;
import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED_COOKIES;
import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED_EXPIRED;
import static com.xyrlsz.xcimoc.Constants.KOMIIC_SHARED_USERNAME;
import static com.xyrlsz.xcimoc.Constants.VOMIC_SHARED;
import static com.xyrlsz.xcimoc.Constants.VOMIC_SHARED_COOKIES;
import static com.xyrlsz.xcimoc.Constants.VOMIC_SHARED_USERNAME;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_AUTO_SIGN;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_TOKEN;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_UID;
import static com.xyrlsz.xcimoc.Constants.ZAI_SHARED_USERNAME;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.ui.view.ComicSourceLoginView;
import com.xyrlsz.xcimoc.ui.widget.LoginDialog;
import com.xyrlsz.xcimoc.ui.widget.Option;
import com.xyrlsz.xcimoc.ui.widget.preference.CheckBoxPreference;
import com.xyrlsz.xcimoc.utils.HashUtils;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.KomiicUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.ThemeUtils;
import com.xyrlsz.xcimoc.utils.ZaiManhuaSignUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class ComicSourceLoginActivity extends BackActivity implements ComicSourceLoginView {
    @BindView(R.id.comic_login_layout)
    View mComicSourceLoginLayout;

    @BindView(R.id.comic_login_dmzj_login)
    Option mDmzjLogin;
    @BindView(R.id.comic_login_dmzj_logout)
    ImageButton mDmzjLogout;

    @BindView(R.id.comic_login_komiic_login)
    Option mkomiicLogin;
    @BindView(R.id.comic_login_komiic_logout)
    ImageButton mKomiicLogout;

    @BindView(R.id.comic_login_vomicmh_login)
    Option mVoMiCMHLogin;
    @BindView(R.id.comic_login_vomicmh_logout)
    ImageButton mVoMiCMHLogout;

    @BindView(R.id.comic_login_zai_login)
    Option mZaiLogin;
    @BindView(R.id.comic_login_zai_logout)
    ImageButton mZaiLogout;

    @BindView(R.id.comic_login_zai_auto_sign)
    CheckBoxPreference mZaiAutoSign;

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.settings_comic_login_title);
    }

    @Override
    protected void initView() {
        super.initView();
        boolean isDarkMod = ThemeUtils.isDarkMode(getAppInstance());
        if (isDarkMod) {
            mDmzjLogout.setImageResource(R.drawable.ic_logout_white);
            mKomiicLogout.setImageResource(R.drawable.ic_logout_white);
            mVoMiCMHLogout.setImageResource(R.drawable.ic_logout_white);
            mZaiLogout.setImageResource(R.drawable.ic_logout_white);
        } else {
            mDmzjLogout.setImageResource(R.drawable.ic_logout);
            mKomiicLogout.setImageResource(R.drawable.ic_logout);
            mVoMiCMHLogout.setImageResource(R.drawable.ic_logout);
            mZaiLogout.setImageResource(R.drawable.ic_logout);
        }

        String dmzjUsername = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE).getString(DMZJ_SHARED_USERNAME, "");
        if (!dmzjUsername.isEmpty()) {
            mDmzjLogin.setSummary(dmzjUsername);
            mDmzjLogin.setTitle(getString(R.string.logined));
            mDmzjLogout.setVisibility(View.VISIBLE);
        }

        String komiicUsername = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE).getString(KOMIIC_SHARED_USERNAME, "");
        if (!komiicUsername.isEmpty()) {
            mkomiicLogin.setSummary(komiicUsername);
            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                mkomiicLogin.setSummary(komiicUsername + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
                CharSequence tmp = mkomiicLogin.getSummary();
                KomiicUtils.getImageLimit("", res -> mKomiicLogout.post(() -> {
                    mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.empty_account_limit) + res);
                }));
            }));
            mkomiicLogin.setTitle(getString(R.string.logined));
            mKomiicLogout.setVisibility(View.VISIBLE);
        } else {
            CharSequence tmp = mkomiicLogin.getSummary();
            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
            }));
        }

        String vomicmhUsername = getSharedPreferences(VOMIC_SHARED, MODE_PRIVATE).getString(VOMIC_SHARED_USERNAME, "");
        if (!vomicmhUsername.isEmpty()) {
            mVoMiCMHLogin.setSummary(vomicmhUsername);
            mVoMiCMHLogin.setTitle(getString(R.string.logined));
            mVoMiCMHLogout.setVisibility(View.VISIBLE);
        }

        String zaiUsername = getSharedPreferences(ZAI_SHARED, MODE_PRIVATE).getString(ZAI_SHARED_USERNAME, "");
        if (!zaiUsername.isEmpty()) {
            mZaiLogin.setSummary(zaiUsername);
            mZaiLogin.setTitle(getString(R.string.logined));
            mZaiLogout.setVisibility(View.VISIBLE);

            boolean autoSign = getSharedPreferences(ZAI_SHARED, MODE_PRIVATE).getBoolean(ZAI_SHARED_AUTO_SIGN, false);
            mZaiAutoSign.setChecked(autoSign);
            mZaiAutoSign.setVisibility(View.VISIBLE);

            ZaiManhuaSignUtils.CheckSigned(isSigned -> {
                if (isSigned) {
                    App.runOnMainThread(() -> {
                        mZaiAutoSign.setSummary(getString(R.string.is_sign));
                    });
                }
            });

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_comic_source_login;
    }

    @Override
    public void onLoginSuccess() {
        hideProgressDialog();
        showSnackbar(getString(R.string.user_login_sucess));
    }

    @Override
    public void onLoginFail() {
        hideProgressDialog();
        showSnackbar(getString(R.string.user_login_failed));
    }

    @Override
    public void onStartLogin() {
        showProgressDialog();
    }

    @Override
    protected View getLayoutView() {
        return mComicSourceLoginLayout;
    }

    // 动漫之家
    @OnClick(R.id.comic_login_dmzj_login)
    void onDmzjLoginClick() {

        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }
            onStartLogin();
            RequestBody formBody = new FormBody.Builder()
                    .add("nickname", username)
                    .add("password", password)
                    .add("type", "1")
                    .add("to", "https://i.dmzj.com")
                    .build();

            Request request = new Request.Builder()
                    .url("https://i.dmzj.com/doLogin")
                    .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .addHeader("origin", "https://i.dmzj.com")
                    .addHeader("pragma", "no-cache")
                    .addHeader("priority", "u=1, i")
                    .addHeader("referer", "https://i.dmzj.com/login")
                    .addHeader("sec-ch-ua", "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .addHeader("sec-ch-ua-mobile", "?0")
                    .addHeader("sec-ch-ua-platform", "\"Windows\"")
                    .addHeader("sec-fetch-dest", "empty")
                    .addHeader("sec-fetch-mode", "cors")
                    .addHeader("sec-fetch-site", "same-origin")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                    .addHeader("x-requested-with", "XMLHttpRequest")
                    .post(formBody)
                    .build();

            Objects.requireNonNull(App.getHttpClient()).newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onLoginFail();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    List<String> cookies = response.headers("Set-Cookie");
                    if (response.isSuccessful() && response.body() != null && !cookies.isEmpty()) {
                        String json = response.body().string();
                        String uid = StringUtils.match("m=(\\d+)\\|", json, 1);
                        Set<String> set = new HashSet<>();
                        for (String s : cookies) {
                            List<String> tmp = Arrays.asList(s.split("; "));
                            set.addAll(tmp);
                        }
                        String cookieStr = String.join("; ", set);
                        SharedPreferences sharedPreferences = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(DMZJ_SHARED_COOKIES, cookieStr);
                        editor.putString(DMZJ_SHARED_USERNAME, username);
                        editor.putString(DMZJ_SHARED_UID, uid);
                        editor.apply();
                        runOnUiThread(() -> {
                            mDmzjLogin.setSummary(username);
                            mDmzjLogin.setTitle(getString(R.string.logined));
                            mDmzjLogout.setVisibility(View.VISIBLE);
                        });
                        onLoginSuccess();
                    } else {
                        onLoginFail();
                    }
                    loginDialog.dismiss();
                }
            });


        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://m.idmzj.com/register.html";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();
    }


    @OnClick(R.id.comic_login_dmzj_logout)
    void onDmzjLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DMZJ_SHARED_COOKIES);
        editor.remove(DMZJ_SHARED_USERNAME);
        editor.remove(DMZJ_SHARED_UID);
        mDmzjLogin.setSummary(getString(R.string.no_login));
        mDmzjLogin.setTitle(getString(R.string.login));
        mDmzjLogout.setVisibility(View.GONE);
        editor.apply();
        showSnackbar(getString(R.string.user_login_logout_sucess));
    }

    // komiic
    @OnClick(R.id.comic_login_komiic_login)
    void onKomiicLoginClick() {

        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }
            onStartLogin();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String json = "{\"email\":\"" + username + "\", \"password\":\"" + password + "\"}";
            RequestBody body = RequestBody.create(mediaType, json);
            Request request = new Request.Builder()
                    .url("https://komiic.com/api/login")
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
                    .post(body)
                    .build();

            Objects.requireNonNull(App.getHttpClient()).newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onLoginFail();
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
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            expired = KomiicUtils.toTimestamp(date);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String cookieStr = String.join("; ", set);
                        SharedPreferences sharedPreferences = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KOMIIC_SHARED_COOKIES, cookieStr);
                        editor.putString(KOMIIC_SHARED_USERNAME, username);
                        editor.putLong(KOMIIC_SHARED_EXPIRED, expired);
                        editor.apply();
                        runOnUiThread(() -> {
                            mkomiicLogin.setSummary(username);
                            mkomiicLogin.setTitle(getString(R.string.logined));
                            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                                mkomiicLogin.setSummary(username + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
                                CharSequence tmp = mkomiicLogin.getSummary();
                                KomiicUtils.getImageLimit("", res -> mKomiicLogout.post(() -> {
                                    mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.empty_account_limit) + res);
                                }));
                            }));
                            mKomiicLogout.setVisibility(View.VISIBLE);
                        });
                        onLoginSuccess();
                    } else {
                        onLoginFail();
                    }
                    loginDialog.dismiss();
                }
            });


        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://komiic.com/register";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();


    }

    @OnClick(R.id.comic_login_komiic_logout)
    void onKomiicLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KOMIIC_SHARED_COOKIES);
        editor.remove(KOMIIC_SHARED_USERNAME);
        editor.remove(KOMIIC_SHARED_EXPIRED);
        editor.apply();
        mkomiicLogin.setSummary(getString(R.string.no_login));
        mkomiicLogin.setTitle(getString(R.string.login));
        mKomiicLogout.setVisibility(View.GONE);
    }

    // vomicmh漫
    @OnClick(R.id.comic_login_vomicmh_login)
    void onVoMiCMHLoginClick() {
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }
            onStartLogin();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String json = "{\"email\":\""
                    + username
                    + "\",\"password\":\""
                    + password
                    + "\"}";
            RequestBody body = RequestBody.create(mediaType, json);
            Request request = new Request.Builder().url("https://api.vomicmh.com/pics/login")
                    .post(body)
                    .addHeader("referer", "https://www.vomicmh.com/")
                    .build();
            Objects.requireNonNull(App.getHttpClient()).newCall(request).enqueue(new Callback() {

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            String token = jsonObject.getString("token");
                            SharedPreferences sharedPreferences = getSharedPreferences(VOMIC_SHARED, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(VOMIC_SHARED_USERNAME, username);
                            editor.putString(VOMIC_SHARED_COOKIES, "_token=" + token);
                            editor.apply();
                            runOnUiThread(() -> {
                                mVoMiCMHLogin.setSummary(username);
                                mVoMiCMHLogin.setTitle(getString(R.string.logined));
                                mVoMiCMHLogout.setVisibility(View.VISIBLE);
                            });
                            onLoginSuccess();
                            loginDialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            onLoginFail();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    onLoginFail();
                }
            });
        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://www.vomicmh.com/";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();
    }

    @OnClick(R.id.comic_login_vomicmh_logout)
    void onVoMiCMHLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(VOMIC_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(VOMIC_SHARED_COOKIES);
        editor.remove(VOMIC_SHARED_USERNAME);
        mVoMiCMHLogin.setSummary(getString(R.string.no_login));
        mVoMiCMHLogin.setTitle(getString(R.string.login));
        mVoMiCMHLogout.setVisibility(View.GONE);
        editor.apply();
        showSnackbar(getString(R.string.user_login_logout_sucess));
    }

    // 再漫画
    @OnClick(R.id.comic_login_zai_login)
    void onZaiLoginClick() {
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }
            onStartLogin();

            String url = StringUtils.format("https://i.zaimanhua.com/lpi/v1/login/passwd?username=%s&passwd=%s", username, HashUtils.MD5(password));
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://i.zaimanhua.com/login")
                    .post(new RequestBody() {
                        @Nullable
                        @Override
                        public MediaType contentType() {
                            return null;
                        }

                        @Override
                        public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
                        }
                    }).build();
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
                            SharedPreferences sharedPreferences = getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(ZAI_SHARED_TOKEN, token);
                            editor.putString(ZAI_SHARED_UID, uid);
                            editor.putString(ZAI_SHARED_USERNAME, username);
                            editor.apply();
                            runOnUiThread(() -> {
                                mZaiLogin.setSummary(username);
                                mZaiLogin.setTitle(getString(R.string.logined));
                                mZaiLogout.setVisibility(View.VISIBLE);
                                boolean autoSign = getSharedPreferences(ZAI_SHARED, MODE_PRIVATE).getBoolean(ZAI_SHARED_AUTO_SIGN, false);
                                mZaiAutoSign.setChecked(autoSign);
                                mZaiAutoSign.setVisibility(View.VISIBLE);
                            });

                            onLoginSuccess();
                            loginDialog.dismiss();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            onLoginFail();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    onLoginFail();
                }
            });
        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://i.zaimanhua.com/login";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();
    }

    @OnClick(R.id.comic_login_zai_logout)
    void onZaiLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(ZAI_SHARED_UID);
        editor.remove(ZAI_SHARED_TOKEN);
        editor.remove(ZAI_SHARED_USERNAME);
        mZaiLogin.setSummary(getString(R.string.no_login));
        mZaiLogin.setTitle(getString(R.string.login));
        mZaiLogout.setVisibility(View.GONE);
        editor.apply();
        mZaiAutoSign.setVisibility(View.GONE);
    }

    @OnClick(R.id.comic_login_zai_auto_sign)
    void onZaiAutoSignClick() {
        boolean isChecked = mZaiAutoSign.isChecked();
        mZaiAutoSign.setChecked(!isChecked);
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(ZAI_SHARED_AUTO_SIGN, mZaiAutoSign.isChecked());
        editor.apply();
    }

    @OnLongClick(R.id.comic_login_zai_auto_sign)
    void onZaiAutoSignLongClick() {
        ZaiManhuaSignUtils.CheckSigned(isSigned -> {
            if (isSigned) {
                App.runOnMainThread(() -> {
                    HintUtils.showToast(getApplicationContext(), "再漫画已签到");
                });
            } else {
                ZaiManhuaSignUtils.sign();
            }
        });
    }
}