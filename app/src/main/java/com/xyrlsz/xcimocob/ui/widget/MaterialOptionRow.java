package com.xyrlsz.xcimocob.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

/**
 * Material 卡片式「左标签 + 右侧内容」option 行，用于 JS 源设置/登录区动态生成控件。
 * <p>
 * 统一采用圆角卡片 + 轻微阴影（明暗模式自适应主题 surface 色），支持：
 * <ul>
 *   <li>{@link #setLabel(CharSequence)}：左侧标签；</li>
 *   <li>{@link #setValueText(CharSequence)}：右侧纯文字（如登录状态）；</li>
 *   <li>{@link #setContent(View)}：右侧任意控件（如开关/下拉/输入框/按钮）。</li>
 * </ul>
 */
public class MaterialOptionRow extends MaterialCardView {

    private final TextView mLabelView;
    private final LinearLayout mContentContainer;

    public MaterialOptionRow(@NonNull Context context) {
        this(context, null);
    }

    public MaterialOptionRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    public MaterialOptionRow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setCardElevation(dp(1));
        setRadius(dp(12));
        setStrokeWidth(0);
        setUseCompatPadding(false);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(12), dp(12));
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mLabelView = new TextView(context);
        mLabelView.setTextSize(15);
        mLabelView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mLabelView);

        mContentContainer = new LinearLayout(context);
        mContentContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContentContainer.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(mContentContainer);
    }

    /** 设置左侧标签文字。 */
    public void setLabel(CharSequence text) {
        mLabelView.setText(text);
    }

    public TextView getLabelView() {
        return mLabelView;
    }

    /** 设置右侧纯文字（替换原有内容），用于登录状态等只读展示。 */
    public void setValueText(CharSequence text) {
        mContentContainer.removeAllViews();
        TextView value = new TextView(getContext());
        value.setText(text);
        value.setTextSize(14);
        value.setTextColor(0xFF888888);
        mContentContainer.addView(value);
    }

    /** 设置右侧内容控件（替换原有内容）。 */
    public void setContent(View view) {
        mContentContainer.removeAllViews();
        if (view != null) {
            mContentContainer.addView(view);
        }
    }

    public LinearLayout getContentContainer() {
        return mContentContainer;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
