package com.xyrlsz.xcimocob.model.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.objectbox.converter.PropertyConverter;

public class ImageUrlStringConverter implements PropertyConverter<List<String>, String> {
    private static final String SPLIT = "##XCimoc:ImageUrl##";

    @Override
    public List<String> convertToEntityProperty(String databaseValue) {
        if (databaseValue == null) return null;
        if (databaseValue.isEmpty()) return Collections.emptyList();  // 关键修复
        return Arrays.asList(databaseValue.split(SPLIT));
    }

    @Override
    public String convertToDatabaseValue(List<String> entityProperty) {
        if (entityProperty == null) return null;
        if (entityProperty.isEmpty()) return "";  // 显式返回空字符串，逻辑一致
        StringBuilder sb = new StringBuilder();
        for (String str : entityProperty) {
            sb.append(str).append(SPLIT);
        }
        return sb.toString();
    }
}
