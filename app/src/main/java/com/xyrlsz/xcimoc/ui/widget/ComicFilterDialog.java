package com.xyrlsz.xcimoc.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.xyrlsz.xcimoc.R;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ComicFilterDialog extends Dialog {
    @BindView(R.id.et_keyword)
    EditText keywordEditText;
    @BindView(R.id.cb_is_completed)
    CheckBox isCompletedCheckBox;
    @BindView(R.id.cb_is_not_completed)
    CheckBox isNotCompletedCheckBox;
    @BindView(R.id.btn_commit)
    Button commitButton;
    @BindView(R.id.btn_cancel)
    Button cancelButton;

    public ComicFilterDialog(Context context, int themeResId, SubmitCallBack callBack) {
        super(context, themeResId);
        init(context, callBack);
    }

    private void init(Context context, SubmitCallBack callBack) {
        this.setContentView(R.layout.dialog_comic_filter);
        // Find views by ID
        ButterKnife.bind(this);
        // Set up click listeners
        commitButton.setOnClickListener(v -> {
            String keyword = keywordEditText.getText().toString().trim();
            boolean isCompleted = isCompletedCheckBox.isChecked();
            boolean isNotCompleted = isNotCompletedCheckBox.isChecked();
            if (StringUtils.isEmpty(keyword) && !isCompleted && !isNotCompleted) {
                callBack.OnClickCancel();
                dismiss();
            }
            callBack.OnClickCommit(keyword, isCompleted, isNotCompleted);
            dismiss();
        });
        cancelButton.setOnClickListener(v -> {
            callBack.OnClickCancel();
            dismiss();
        });
    }


    public interface SubmitCallBack {
        void OnClickCommit(String keyword, boolean isCompleted, boolean isNotCompleted);

        void OnClickCancel();
    }

}