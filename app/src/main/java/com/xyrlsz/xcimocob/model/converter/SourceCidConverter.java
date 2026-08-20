package com.xyrlsz.xcimocob.model.converter;

import android.util.Pair;

import io.objectbox.converter.PropertyConverter;

public class SourceCidConverter implements PropertyConverter<Pair<Integer, String>, String> {
    private static final String SPLIT = "##XCimoc:SourceCid##";

    @Override
    public Pair<Integer, String> convertToEntityProperty(String databaseValue) {
        if (databaseValue == null || databaseValue.isEmpty()) {
            return null;  // 或返回一个默认 Pair，视业务需求而定
        }
        String[] split = databaseValue.split(SPLIT);
        if (split.length != 2) {
            return null;
        }
        try {
            int sourceId = Integer.parseInt(split[0]);
            // 注意：split[1] 如果是空字符串，则 second 为空字符串，而非 null
            return new Pair<>(sourceId, split[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String convertToDatabaseValue(Pair<Integer, String> entityProperty) {
        if (entityProperty == null) {
            return null;
        }
        // 明确约定：如果 second 为 null，存储为空字符串（或选择存储 "null" 字符串，需统一）
        String second = entityProperty.second == null ? "" : entityProperty.second;
        return entityProperty.first + SPLIT + second;
    }
}