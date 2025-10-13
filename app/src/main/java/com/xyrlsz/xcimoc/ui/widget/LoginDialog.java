package com.xyrlsz.xcimoc.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.utils.ThemeUtils;

public class LoginDialog extends Dialog {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private ImageButton togglePasswordButton;
    private boolean isPasswordInvisible = true;
    // 设置登录和注册的监听器
    private OnLoginListener loginListener;
    private OnRegisterListener registerListener;
    private Context mContext;

    public LoginDialog(Context context) {
        super(context);
        init(context);
    }

    public LoginDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.setContentView(R.layout.dialog_login);
        // Find views by ID
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        togglePasswordButton = findViewById(R.id.ib_is_show_passwd);
        changeTogglePassword();
        togglePasswordButton.setOnClickListener(
                view -> togglePasswordVisibility()
        );

        // 可以通过提供接口来处理登录和注册按钮的点击事件
        loginButton.setOnClickListener(view -> {
                    if (loginListener != null) {
                        loginListener.onLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString());
                    }
                    dismiss();
                }
        );

        registerButton.setOnClickListener(v -> {

            // 注册逻辑
            if (registerListener != null) {
                registerListener.onRegister();
            }
            dismiss();

        });
    }

    private void changeTogglePassword() {
        if (isPasswordInvisible) {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            if (ThemeUtils.isDarkMode(mContext)) {
                togglePasswordButton.setImageResource(R.drawable.eye_close_white);
            } else {
                togglePasswordButton.setImageResource(R.drawable.eye_close);
            }
        } else {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            if (ThemeUtils.isDarkMode(mContext)) {
                togglePasswordButton.setImageResource(R.drawable.eye_white);
            } else {
                togglePasswordButton.setImageResource(R.drawable.eye);
            }
        }
    }

    private void togglePasswordVisibility() {
        isPasswordInvisible = !isPasswordInvisible;
        changeTogglePassword();
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    public void setOnLoginListener(OnLoginListener listener) {
        this.loginListener = listener;
    }

    public void setOnRegisterListener(OnRegisterListener listener) {
        this.registerListener = listener;
    }

    // 定义监听器接口
    public interface OnLoginListener {
        void onLogin(String username, String password);
    }

    public interface OnRegisterListener {
        void onRegister();
    }
}
