package com.xyrlsz.xcimocob.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.utils.ThemeUtils;

public class LoginDialog extends Dialog {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private Button logoutButton;
    private ImageButton togglePasswordButton;
    private boolean isPasswordInvisible = true;
    // 设置登录和注册的监听器
    private OnLoginListener loginListener;
    private OnRegisterListener registerListener;
    private OnLogoutListener logoutListener;
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
        setupWindowSize();
        // Find views by ID
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        logoutButton = findViewById(R.id.logout_button);
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

        logoutButton.setOnClickListener(v -> {
            if (logoutListener != null) {
                logoutListener.onLogout();
            }
            dismiss();
        });
    }

    /**
     * 控制注册按钮显隐：源未声明注册链接时传入 false 隐藏。
     */
    public void setRegisterButtonVisible(boolean visible) {
        registerButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * 控制登出按钮显隐：默认隐藏。
     */
    public void setLogoutButtonVisible(boolean visible) {
        logoutButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * 设置对话框窗口宽度为屏幕宽度的约 85%，避免过宽/贴边显示异常。
     */
    private void setupWindowSize() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        Point size = new Point();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Rect bounds = wm.getCurrentWindowMetrics().getBounds();
                size.set(bounds.width(), bounds.height());
            } else {
                wm.getDefaultDisplay().getSize(size);
            }
            window.setLayout((int) (size.x * 0.85f), WindowManager.LayoutParams.WRAP_CONTENT);
        }
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

    public void setOnLogoutListener(OnLogoutListener listener) {
        this.logoutListener = listener;
    }

    // 定义监听器接口
    public interface OnLoginListener {
        void onLogin(String username, String password);
    }

    public interface OnRegisterListener {
        void onRegister();
    }

    public interface OnLogoutListener {
        void onLogout();
    }
}
