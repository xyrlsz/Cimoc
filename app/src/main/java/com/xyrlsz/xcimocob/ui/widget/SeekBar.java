package com.xyrlsz.xcimocob.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

/**
 * 自定义 SeekBar，继承 Material Slider。
 * 自动处理 valueFrom >= valueTo 的边界情况，避免
 * "valueFrom(x) must be smaller than valueTo(x)" 崩溃。
 * <p>
 * 当 valueFrom >= valueTo 时，自动将 valueTo 扩展为 valueFrom + 1，
 * 保持 value 不变，确保 Slider 布局时不会因范围校验失败而崩溃。
 */
public class SeekBar extends Slider {

    public SeekBar(@NonNull Context context) {
        super(context);
    }

    public SeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setValueFrom(float valueFrom) {
        if (valueFrom >= getValueTo()) {
            // 确保 valueFrom < valueTo，否则将 valueTo 扩展为 valueFrom + 1
            super.setValueFrom(valueFrom);
            super.setValueTo(valueFrom + 1f);
        } else {
            super.setValueFrom(valueFrom);
        }
    }

    @Override
    public void setValueTo(float valueTo) {
        if (valueTo <= getValueFrom()) {
            // 确保 valueFrom < valueTo，否则将 valueFrom 回退为 valueTo - 1
            super.setValueTo(valueTo);
            super.setValueFrom(valueTo - 1f);
        } else {
            super.setValueTo(valueTo);
        }
    }

    /**
     * 安全设置范围与当前值。当范围无效时自动修正，不会崩溃。
     *
     * @param valueFrom 范围起始
     * @param valueTo   范围结束
     * @param value     当前值
     */
    public void setRangeSafe(float valueFrom, float valueTo, float value) {
        if (valueFrom >= valueTo) {
            // 范围无效时扩展为 [valueFrom, valueFrom + 1]
            super.setValueFrom(valueFrom);
            super.setValueTo(valueFrom + 1f);
        } else {
            super.setValueFrom(valueFrom);
            super.setValueTo(valueTo);
        }
        setValue(value);
    }

}
