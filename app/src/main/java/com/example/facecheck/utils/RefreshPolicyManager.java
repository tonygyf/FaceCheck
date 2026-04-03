package com.example.facecheck.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class RefreshPolicyManager {
    private static final String PREFS_NAME = "refresh_policy_prefs";

    public static final long TTL_CLASSROOM_MS = 90_000L;
    public static final long TTL_HOME_MS = 60_000L;

    public static boolean shouldRefresh(Context context, String key, long ttlMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long last = prefs.getLong(key, 0L);
        if (last <= 0L) return true;
        return System.currentTimeMillis() - last >= ttlMs;
    }

    public static void markRefreshed(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(key, System.currentTimeMillis()).apply();
    }
}
