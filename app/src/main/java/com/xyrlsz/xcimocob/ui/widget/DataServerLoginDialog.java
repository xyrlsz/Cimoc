package com.xyrlsz.xcimocob.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.xyrlsz.xcimocob.R;

/**
 * 数据同步服务器登录对话框（仅登录，注册改由管理后台处理）
 */
public class DataServerLoginDialog extends Dialog {

    private EditText mUsernameInput;
    private EditText mPasswordInput;
    private final Callback mCallback;

    public DataServerLoginDialog(Context context, int themeResId, Callback callback) {
        super(context, themeResId);
        this.mCallback = callback;
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_data_server_login);
        Window window = getWindow();
        if (window != null) {
            // 清除宿主 Activity 继承来的沉浸式 flag（windowTranslucentStatus），
            // 否则 SOFT_INPUT_ADJUST_RESIZE 不生效，键盘会遮挡底部按钮
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 窗口随软键盘弹出而缩放
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        mUsernameInput = findViewById(R.id.input_username);
        mPasswordInput = findViewById(R.id.input_password);
        Button loginButton = findViewById(R.id.btn_action);
        loginButton.setText(R.string.data_server_btn_login);

        loginButton.setOnClickListener(v -> {
            String username = mUsernameInput.getText().toString().trim();
            String password = mPasswordInput.getText().toString().trim();
            if (!username.isEmpty() && !password.isEmpty()) {
                mCallback.onLogin(username, password);
                dismiss();
            }
        });
    }

    public interface Callback {
        void onLogin(String username, String password);
    }
}
