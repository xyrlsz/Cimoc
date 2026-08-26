package com.xyrlsz.xcimocob.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.source.js.JsMangaParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 按「JS 配置」自动生成登录与设置界面的页面（替代硬编码的逐源固定页面）。
 * <p>
 * 登录区：脚本声明 {@code getLoginState()/login(params)/logout()} 时渲染账号/密码框与登录/登出按钮；
 * 设置区：脚本声明 {@code getSettings()} 返回字段描述数组时，按类型（text/select/bool）动态渲染并持久化。
 */
public class JsSourceSettingsActivity extends BackActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_TITLE = "title";

    private LinearLayout mContainer;
    private JsMangaParser mParser;

    public static Intent createIntent(Context context, int type, String title) {
        Intent intent = new Intent(context, JsSourceSettingsActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_TITLE, title);
        return intent;
    }

    @Override
    protected String getDefaultTitle() {
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        return title == null || title.isEmpty() ? getString(R.string.comic_source_js_settings) : title;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_js_source_settings;
    }

    @Override
    protected void initViewById() {
        super.initViewById();
        mContainer = findViewById(R.id.js_settings_container);
    }

    @Override
    protected void initView() {
        super.initView();
        MangaParser parser = SourceManager.getInstance(this).getParser(getIntent().getIntExtra(EXTRA_TYPE, -1));
        if (!(parser instanceof JsMangaParser)) {
            Toast.makeText(this, R.string.comic_source_js_settings, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mParser = (JsMangaParser) parser;
        buildAll();
    }

    private void buildAll() {
        mContainer.removeAllViews();
        boolean hasLogin = mParser.hasLogin();
        JSONArray settings = mParser.getSettings();
        int settingCount = (settings == null) ? 0 : settings.length();
        android.util.Log.i("JsSource", "[settings] type="
                + mParser.getType() + " hasLogin=" + hasLogin
                + " settings=" + settingCount
                + (settings != null ? " " + settings.toString() : ""));
        // 打印当前登录态，确认是否真的保存了
        try {
            String stored = com.xyrlsz.xcimocob.source.js.JsHost.INSTANCE
                    .getLogin(mParser.getType());
            android.util.Log.i("JsSource", "[settings] type=" + mParser.getType()
                    + " loginState(js)=" + mParser.getLoginState().toString()
                    + " stored(host)=" + (stored == null ? "null" : stored));
        } catch (Exception ignore) {
        }
        if (hasLogin) {
            mContainer.addView(buildLoginSection());
        }
        if (settingCount > 0) {
            mContainer.addView(buildSettingsSection(settings));
        }
        if (mContainer.getChildCount() == 0) {
            TextView tv = new TextView(this);
            tv.setText("该源未声明登录或设置项");
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            mContainer.addView(tv);
        }
    }

    /* ---------------- 登录区（显示为一个可点击 option，点击弹登录对话框） ---------------- */

    private View buildLoginSection() {
        JSONObject state = mParser.getLoginState();
        boolean loggedIn = state != null && state.optBoolean("loggedIn", false);
        String status = loggedIn ? getString(R.string.comic_source_js_logged_in)
                : getString(R.string.comic_source_js_not_logged_in);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        addSectionTitle(box, getString(R.string.comic_source_js_login));
        View row = addOptionRow(box, getString(R.string.comic_source_js_login), status);
        row.setOnClickListener(v -> showLoginDialog());
        addDivider(box);
        return box;
    }

    private void showLoginDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(12), dp(24), 0);

        EditText account = new EditText(this);
        account.setHint(R.string.comic_source_js_account);
        account.setSingleLine(true);
        EditText password = new EditText(this);
        password.setHint(R.string.comic_source_js_password);
        password.setSingleLine(true);
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(account);
        content.addView(password);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.comic_source_js_login))
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.comic_source_js_logout, (d, w) -> {
                    mParser.logout();
                    Toast.makeText(this, R.string.user_login_logout_sucess, Toast.LENGTH_SHORT).show();
                    buildAll();
                })
                .setPositiveButton(R.string.comic_source_js_login, (d, w) -> {
                    JSONObject params = new JSONObject();
                    try {
                        params.put("account", account.getText().toString());
                        params.put("password", password.getText().toString());
                    } catch (Exception ignore) {
                    }
                    doLogin(params);
                })
                .show();
    }

    private void doLogin(JSONObject params) {
        new Thread(() -> {
            JSONObject result = mParser.login(params);
            android.util.Log.i("JsSource", "[login] type=" + mParser.getType()
                    + " result=" + (result != null ? result.toString() : "null"));
            runOnUiThread(() -> {
                boolean ok = result != null && result.optBoolean("success", false);
                String msg = ok ? getString(R.string.user_login_sucess)
                        : (result != null && !result.optString("message").isEmpty()
                        ? result.optString("message") : getString(R.string.user_login_failed));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                buildAll();
            });
        }).start();
    }

    /* ---------------- 通用 option 行 ---------------- */

    /** 添加一行「左标签 + 右侧文字」的 option 行，返回该行（可设置点击）。 */
    private View addOptionRow(LinearLayout box, String label, String valueText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(15);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lbl);

        TextView value = new TextView(this);
        value.setText(valueText == null ? "" : valueText);
        value.setTextSize(14);
        value.setTextColor(0xFF888888);
        row.addView(value);

        box.addView(row);
        return row;
    }

    /** 添加一行「左标签 + 右侧控件」的 option 行，返回该行。 */
    private View addOptionRow(LinearLayout box, String label, View control) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(15);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lbl);

        if (control != null) row.addView(control);

        box.addView(row);
        return row;
    }

    private void addDivider(LinearLayout box) {
        View v = new View(this);
        v.setBackgroundColor(0x12000000);
        box.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    /* ---------------- 设置区 ---------------- */

    private static class SettingRow {
        String key;
        String type;
        EditText edit;
        Spinner spinner;
        CheckBox check;
    }

    private View buildSettingsSection(JSONArray settings) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        addSectionTitle(box, "设置");

        for (int i = 0; i < settings.length(); i++) {
            JSONObject o = settings.optJSONObject(i);
            if (o == null) continue;
            String key = o.optString("key");
            String label = o.optString("label", key);
            String type = o.optString("type", "text");
            String def = o.optString("default");
            SettingRow row = new SettingRow();
            row.key = key;
            row.type = type;

            String current = mParser.getSetting(key);
            if (current == null) current = def;

            switch (type) {
                case "bool": {
                    // 勾选型 option：右侧 CheckBox，勾选即保存
                    row.check = new CheckBox(this);
                    row.check.setChecked("true".equalsIgnoreCase(current) || "1".equals(current));
                    row.check.setOnCheckedChangeListener((b, checked) ->
                            mParser.setSetting(key, checked ? "true" : "false"));
                    addOptionRow(box, label, row.check);
                    break;
                }
                case "select": {
                    // 下拉型 option：右侧 Spinner，选择即保存（存 value）
                    row.spinner = new Spinner(this);
                    List<String> opts = new ArrayList<>();
                    List<String> vals = new ArrayList<>();
                    JSONArray arr = o.optJSONArray("options");
                    if (arr != null) {
                        for (int j = 0; j < arr.length(); j++) {
                            JSONObject op = arr.optJSONObject(j);
                            if (op != null) {
                                opts.add(op.optString("label"));
                                vals.add(op.optString("value"));
                            }
                        }
                    }
                    row.spinner.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, opts));
                    int idx = vals.indexOf(current);
                    if (idx >= 0) row.spinner.setSelection(idx);
                    row.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            String val = (pos >= 0 && pos < vals.size()) ? vals.get(pos) : "";
                            mParser.setSetting(key, val);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                    addOptionRow(box, label, row.spinner);
                    break;
                }
                case "callback":
                case "button": {
                    // 按钮型 option：点击调用脚本 onSettingsAction(key)（如签到）
                    String actionKey = key;
                    Button btn = new Button(this);
                    btn.setText(o.optString("buttonText", label));
                    btn.setOnClickListener(v -> {
                        btn.setEnabled(false);
                        new Thread(() -> {
                            JSONObject r = mParser.settingsCallback(actionKey);
                            runOnUiThread(() -> {
                                btn.setEnabled(true);
                                // 优先显示回调返回的真实 message（如"已登录剩余可看 N 页"），
                                // 不能成功时硬编码"登录成功"——那是给登录按钮用的
                                String msg = (r != null && !r.optString("message").isEmpty())
                                        ? r.optString("message")
                                        : ((r != null && r.optBoolean("success", false))
                                        ? "操作成功" : "操作失败");
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    });
                    addOptionRow(box, label, btn);
                    break;
                }
                default: {
                    // 文本型 option：右侧 EditText，失去焦点时保存
                    row.edit = new EditText(this);
                    row.edit.setText(current);
                    row.edit.setSingleLine(true);
                    row.edit.setGravity(android.view.Gravity.END);
                    row.edit.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            mParser.setSetting(key, row.edit.getText().toString());
                        }
                    });
                    addOptionRow(box, label, row.edit);
                    break;
                }
            }
            addDivider(box);
        }
        return box;
    }

    private void addSectionTitle(LinearLayout box, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                com.xyrlsz.xcimocob.utils.ThemeUtils.getResourceId(this, R.attr.colorAccent)));
        tv.setTextSize(15);
        tv.setPadding(0, dp(4), 0, dp(4));
        box.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
