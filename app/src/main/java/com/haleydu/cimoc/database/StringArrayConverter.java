package com.haleydu.cimoc.database;


import androidx.room.TypeConverter;

public class StringArrayConverter {

    private static final String SPLIT = "##Cimoc##";

    @TypeConverter
    public static String[] fromString(String value) {
        if (value == null) {
            return null;
        }
        return value.split(SPLIT);
    }

    @TypeConverter
    public static String toString(String[] array) {
        if (array == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String str : array) {
            sb.append(str).append(SPLIT);
        }
        return sb.toString();
    }
}