package com.example.facecheck.sync;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.ByteArrayOutputStream;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String BASE_URL = "https://omni.gyf123.dpdns.org"; // Replaced placeholder

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final SessionManager sessionManager;

    public SyncManager(Context context, DatabaseHelper dbHelper) {
        Log.e("SYNC_DEBUG", "SyncManager 被构造了");
        this.context = context;
        this.dbHelper = dbHelper;
        this.sessionManager = new SessionManager(context);
    }

    /**
     * 主同步入口：上传所有 PENDING 的 SyncLog 对应实体到 Worker
     */
    public boolean performSync() {
        Cursor logs = dbHelper.getPendingSyncLogs();
        if (logs == null) return true;

        boolean allSuccess = true;
        if (logs.moveToFirst()) {
            do {
                long logId    = logs.getLong(logs.getColumnIndexOrThrow("id"));
                String entity = logs.getString(logs.getColumnIndexOrThrow("entity"));
                long entityId = logs.getLong(logs.getColumnIndexOrThrow("entityId"));
                String op     = logs.getString(logs.getColumnIndexOrThrow("op"));

                boolean ok = syncEntity(entity, entityId, op);
                dbHelper.updateSyncLogStatus(logId, ok ? "SYNCED" : "FAILED");
                if (!ok) allSuccess = false;

            } while (logs.moveToNext());
        }
        logs.close();
        return allSuccess;
    }

    private boolean syncEntity(String entity, long entityId, String op) {
        switch (entity) {
            case "Classroom": return syncClassroom(entityId, op);
            case "Student":   return syncStudent(entityId, op);
            default:
                Log.w(TAG, "未处理的实体类型: " + entity);
                return false;
        }
    }

    // ===================== Classroom =====================

    private boolean syncClassroom(long classroomId, String op) {
        if ("DELETE".equals(op)) return true; // 暂不处理远程删除

        Cursor c = dbHelper.getReadableDatabase().query(
            "Classroom", null, "id = ?",
            new String[]{String.valueOf(classroomId)}, null, null, null);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            return false;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("id",        c.getLong(c.getColumnIndexOrThrow("id")));
            body.put("teacherId", c.getLong(c.getColumnIndexOrThrow("teacherId")));
            body.put("name",      c.getString(c.getColumnIndexOrThrow("name")));
            body.put("year",      c.getInt(c.getColumnIndexOrThrow("year")));
            body.put("meta",      c.getString(c.getColumnIndexOrThrow("meta")));
            c.close();

            return postJson("/api/classrooms", body);
        } catch (Exception e) {
            Log.e(TAG, "syncClassroom 失败", e);
            if (!c.isClosed()) c.close();
            return false;
        }
    }

    // ===================== Student =====================

    private boolean syncStudent(long studentId, String op) {
        if ("DELETE".equals(op)) return true;

        Cursor c = dbHelper.getReadableDatabase().query(
            "Student", null, "id = ?",
            new String[]{String.valueOf(studentId)}, null, null, null);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            return false;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("id",        c.getLong(c.getColumnIndexOrThrow("id")));
            body.put("classId",   c.getLong(c.getColumnIndexOrThrow("classId")));
            body.put("name",      c.getString(c.getColumnIndexOrThrow("name")));
            body.put("sid",       c.getString(c.getColumnIndexOrThrow("sid")));
            body.put("gender",    c.getString(c.getColumnIndexOrThrow("gender")));
            body.put("avatarUri", c.getString(c.getColumnIndexOrThrow("avatarUri")));
            c.close();

            return postJson("/api/students", body);
        } catch (Exception e) {
            Log.e(TAG, "syncStudent 失败", e);
            if (!c.isClosed()) c.close();
            return false;
        }
    }

    // ===================== 从远端拉取班级 =====================

    /**
     * 从 Worker 拉取该教师的班级列表，替换本地数据
     */
    public boolean fetchRemoteClassrooms(long teacherId) {
        try {
            // 用 delta 接口，lastSyncTimestamp=0 拉全量
            String path = "/api/v1/classes/delta?lastSyncTimestamp=0";
            String resp = getJson(path);
            if (resp == null) return false;

            JSONObject json = new JSONObject(resp);
            JSONArray added   = json.optJSONArray("addedClasses");
            JSONArray updated = json.optJSONArray("updatedClasses");

            List<com.example.facecheck.data.model.Classroom> classrooms = new java.util.ArrayList<>();
            parseClassrooms(added,   teacherId, classrooms);
            parseClassrooms(updated, teacherId, classrooms);

            if (!classrooms.isEmpty()) {
                dbHelper.replaceTeacherClassrooms(teacherId, classrooms);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "fetchRemoteClassrooms 失败", e);
            return false;
        }
    }

    private void parseClassrooms(JSONArray arr, long teacherId,
            List<com.example.facecheck.data.model.Classroom> out) throws Exception {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            com.example.facecheck.data.model.Classroom c =
                new com.example.facecheck.data.model.Classroom(
                    o.getLong("id"),
                    teacherId,
                    o.getString("name"),
                    o.getInt("year"),
                    o.optString("meta", null)
                );
            out.add(c);
        }
    }

    // ===================== HTTP 工具 =====================

    private boolean postJson(String path, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-API-Key", sessionManager.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            Log.d(TAG, "POST " + url + " body=" + body);  // ← 加这行
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            int code = conn.getResponseCode();
            // ← 加这段，读取错误响应体
            if (code != 200 && code != 201) {
                java.io.InputStream err = conn.getErrorStream();
                String errBody = "null";
                if (err != null) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = err.read(buf)) != -1) {
                        baos.write(buf, 0, len);
                    }
                    errBody = baos.toString(StandardCharsets.UTF_8.name());
                }
                Log.e(TAG, "POST 失败 " + path + " code=" + code + " body=" + errBody);
                return false;
            }
            Log.d(TAG, "POST " + path + " → " + code);
            return code == 200 || code == 201;
        } catch (Exception e) {
            Log.e(TAG, "postJson 失败: " + path, e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String getJson(String path) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key", sessionManager.getApiKey());
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            java.io.InputStream is = conn.getInputStream();
            // Note: is.readAllBytes() requires API level 28 (Android 9.0 Pie)
            // For broader compatibility, a manual read loop would be better.
            byte[] buf = new byte[1024];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            Log.e(TAG, "getJson 失败: " + path, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
