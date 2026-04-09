package com.example.facecheck.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_TEACHER_ID = "teacher_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    // TODO: Move this to a secure place like BuildConfig
    private static final String API_SECRET = "my-secret-api-key";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取当前登录教师的ID
     * @return a long representing the teacher's ID, or -1 if not found.
     */
    public long getTeacherId() {
        return preferences.getLong(KEY_TEACHER_ID, -1L);
    }

    /**
     * 获取用于API请求的访问令牌
     * @return a String representing the access token, or null if not found.
     */
    public String getApiKey() {
        return API_SECRET;
    }

    /**
     * 检查用户是否已登录（这里仅检查教师）
     * @return true if a teacher is logged in, false otherwise.
     */
    // 改为
    public boolean isLoggedIn() {
        String role = preferences.getString("user_role", "");
        if ("student".equals(role)) {
            return preferences.getLong("student_id", -1L) != -1L;
        }
        return getTeacherId() != -1;
    }

    /**
     * 清除会话数据（用于登出）
     */
    public void logout() {
        preferences.edit()
            .remove(KEY_TEACHER_ID)
            .remove(KEY_ACCESS_TOKEN)
            // Also remove other user-related data
            .remove("user_role")
            .remove("student_id")
            .remove("refresh_token")
            .apply();
    }
}
