package com.xyrlsz.xcimoc.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestampUtils {
    public static String formatTimestamp(long timestamp) {
        Date date            = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }
}
