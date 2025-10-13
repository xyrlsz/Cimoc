package com.xyrlsz.xcimoc.ui.widget;


import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.core.WebDavConf;
import com.xyrlsz.xcimoc.utils.ThemeUtils;

public class WebDavConfDialog extends Dialog {
    private EditText urlEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private ImageButton togglePasswordButton;
    private boolean isPasswordInvisible = true;
    private Button btCommit;

    private SharedPreferences sharedPreferences;
    private Context mContext;

    public WebDavConfDialog(Context context, int themeResId, SubmitCallBack callBack) {
        super(context, themeResId);
        init(context, callBack);
    }

    private void init(Context context, SubmitCallBack callBack) {
        this.mContext = context;
        sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, MODE_PRIVATE);
        this.setContentView(R.layout.dialog_webdav_config);
        // Find views by ID
        urlEditText = findViewById(R.id.webdav_url);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        btCommit = findViewById(R.id.commit_button);
        togglePasswordButton = findViewById(R.id.ib_is_show_passwd);
        changeTogglePassword();
        togglePasswordButton.setOnClickListener(
                view -> togglePasswordVisibility()
        );

        btCommit.setOnClickListener(view -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String webDavUrl = urlEditText.getText().toString();
                    if (webDavUrl.endsWith("/")) {
                        webDavUrl = webDavUrl.substring(0, webDavUrl.length() - 1);
                    }
                    String username = usernameEditText.getText().toString();
                    String password = passwordEditText.getText().toString();

                    editor.putString(Constants.WEBDAV_SHARED_URL, webDavUrl);
                    editor.putString(Constants.WEBDAV_SHARED_USERNAME, username);
                    editor.putString(Constants.WEBDAV_SHARED_PASSWORD, password);
                    editor.apply();
                    if (webDavUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                        callBack.onFailed();
                    } else {
                        callBack.onSuccess();
                    }
                    WebDavConf.update(context);
                    dismiss();
                }
        );

        urlEditText.setText(sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, ""));
        usernameEditText.setText(sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, ""));
        passwordEditText.setText(sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, ""));

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


    public interface SubmitCallBack {
        void onSuccess();

        void onFailed();
    }

}
