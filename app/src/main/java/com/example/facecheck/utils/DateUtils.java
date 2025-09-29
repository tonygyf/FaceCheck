package com.example.facecheck.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static String formatDateTime(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp * 1000L));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp * 1000L));
    }

    public static String formatDate(long timestamp) {
        return DATE_ONLY_FORMAT.format(new Date(timestamp * 1000L));
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String getRelativeTimeSpan(long timestamp) {
        long now = getCurrentTimestamp();
        long diff = now - timestamp;

        if (diff < 60) {
            return "刚刚";
        } else if (diff < 3600) {
            return diff / 60 + "分钟前";
        } else if (diff < 86400) {
            return diff / 3600 + "小时前";
        } else if (diff < 2592000) {
            return diff / 86400 + "天前";
        } else if (diff < 31536000) {
            return diff / 2592000 + "个月前";
        } else {
            return diff / 31536000 + "年前";
        }
    }

    public static String getAcademicYear() {
        Date now = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        int currentYear = Integer.parseInt(yearFormat.format(now));
        
        // 如果当前月份小于9月，则认为是上一学年
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM", Locale.getDefault());
        int currentMonth = Integer.parseInt(monthFormat.format(now));
        
        if (currentMonth < 9) {
            currentYear--;
        }
        
        return String.format(Locale.getDefault(), "%d-%d", currentYear, currentYear + 1);
    }
}