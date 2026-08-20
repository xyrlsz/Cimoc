package com.xyrlsz.xcimocob.model.converter;

import io.objectbox.converter.PropertyConverter;
import okhttp3.Headers;

public class HeadersStringConverter implements PropertyConverter<Headers, String> {
    private static final String SPLIT = "##XCimoc:Headers##";

    @Override
    public Headers convertToEntityProperty(String databaseValue) {
        if (databaseValue == null) return null;
        if (databaseValue.isEmpty()) return Headers.of();  // 关键修复
        return Headers.of(databaseValue.split(SPLIT));
    }

    @Override
    public String convertToDatabaseValue(Headers entityProperty) {
        if (entityProperty == null) return null;
        if (entityProperty.size() == 0) return "";  // 显式返回空字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entityProperty.size(); i++) {
            sb.append(entityProperty.name(i)).append(SPLIT);
            sb.append(entityProperty.value(i)).append(SPLIT);
        }
        return sb.toString();
    }
}