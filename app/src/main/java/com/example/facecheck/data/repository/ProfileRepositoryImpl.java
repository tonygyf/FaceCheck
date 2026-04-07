package com.example.facecheck.data.repository;

import android.content.Context;

import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.ApiResponse;
import com.example.facecheck.api.ChangePasswordRequest;
import com.example.facecheck.api.ChangeStudentPasswordRequest;
import com.example.facecheck.api.ChangeStudentUsernameRequest;
import com.example.facecheck.api.ChangeUsernameRequest;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepositoryImpl implements ProfileRepository {

    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final DatabaseHelper databaseHelper;

    public ProfileRepositoryImpl(Context context) {
        this.apiService = RetrofitClient.getApiService();
        this.sessionManager = new SessionManager(context);
        this.databaseHelper = new DatabaseHelper(context);
    }

    private String getApiKey() {
        return sessionManager.getApiKey();
    }

    @Override
    public void changePassword(long teacherId, String oldPassword, String newPassword, ApiCallback<ApiResponse> callback) {
        ChangePasswordRequest request = new ChangePasswordRequest(teacherId, oldPassword, newPassword);
        apiService.changePassword(getApiKey(), request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError(response.body().getError());
                    }
                } else {
                    callback.onError("Response was not successful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network request failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void uploadAvatar(long teacherId, String filePath, ApiCallback<ApiResponse> callback) {
        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse(detectImageMediaType(filePath)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody teacherIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(teacherId));
        String key = resolveAvatarObjectKey(teacherId);
        RequestBody keyBody = RequestBody.create(MediaType.parse("text/plain"), key);

        apiService.uploadAvatar(getApiKey(), teacherIdBody, keyBody, body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String finalKey = key;
                    if (response.body().getData() != null) {
                        String fromServer = normalizeRemoteKey(response.body().getData().getAvatarUri());
                        if (fromServer != null && !fromServer.isEmpty()) {
                            finalKey = fromServer;
                        }
                    }
                    Teacher teacher = databaseHelper.getTeacherById(teacherId);
                    if (teacher != null) {
                        teacher.setAvatarUri(finalKey);
                        databaseHelper.updateTeacher(teacher);
                    }
                    callback.onSuccess(response.body());
                } else {
                    String error = "Unknown error";
                    if (response.body() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    callback.onError("Upload failed: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error during avatar upload: " + t.getMessage());
            }
        });
    }

    @Override
    public void changeUsername(long teacherId, String newName, ApiCallback<ApiResponse> callback) {
        ChangeUsernameRequest request = new ChangeUsernameRequest(teacherId, newName);
        apiService.changeUsername(getApiKey(), request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body());
                } else {
                    String error = "Unknown error";
                    if (response.body() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception e) { /* Ignore */ }
                    }
                    callback.onError("Username change failed: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error during username change: " + t.getMessage());
            }
        });
    }

    @Override
    public void changeStudentPassword(long studentId, String oldPassword, String newPassword, ApiCallback<ApiResponse> callback) {
        ChangeStudentPasswordRequest request = new ChangeStudentPasswordRequest(studentId, oldPassword, newPassword);
        apiService.changeStudentPassword(getApiKey(), request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body());
                } else {
                    String error = "Unknown error";
                    if (response.body() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception ignored) { }
                    }
                    callback.onError("Student password change failed: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error during student password change: " + t.getMessage());
            }
        });
    }

    @Override
    public void uploadStudentAvatar(long studentId, String filePath, ApiCallback<ApiResponse> callback) {
        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse(detectImageMediaType(filePath)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody studentIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(studentId));
        String key = resolveStudentAvatarObjectKey(studentId);
        RequestBody keyBody = RequestBody.create(MediaType.parse("text/plain"), key);

        apiService.uploadStudentAvatar(getApiKey(), studentIdBody, keyBody, body).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String finalKey = key;
                    if (response.body().getData() != null) {
                        String fromServer = normalizeRemoteKey(response.body().getData().getAvatarUri());
                        if (fromServer != null && !fromServer.isEmpty()) {
                            finalKey = fromServer;
                        }
                    }
                    updateLocalStudentAvatar(studentId, finalKey);
                    callback.onSuccess(response.body());
                } else {
                    String error = "Unknown error";
                    if (response.body() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    callback.onError("Student avatar upload failed: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error during student avatar upload: " + t.getMessage());
            }
        });
    }

    private String resolveAvatarObjectKey(long teacherId) {
        Teacher teacher = databaseHelper.getTeacherById(teacherId);
        if (teacher != null) {
            String existing = normalizeRemoteKey(teacher.getAvatarUri());
            if (existing != null && !existing.isEmpty()) {
                return existing;
            }
            return buildAvatarKey("teachers", teacherId, teacher.getName());
        }
        return buildAvatarKey("teachers", teacherId, null);
    }

    private String resolveStudentAvatarObjectKey(long studentId) {
        Student student = getStudentById(studentId);
        if (student != null) {
            String existing = normalizeRemoteKey(student.getAvatarUri());
            if (existing != null && !existing.isEmpty()) {
                return existing;
            }
            return buildAvatarKey("students", studentId, student.getName());
        }
        return buildAvatarKey("students", studentId, null);
    }

    private String normalizeRemoteKey(String raw) {
        if (raw == null) return null;
        String key = raw.trim();
        if (key.isEmpty()) return null;

        // 过滤本地文件路径（Windows 盘符 / Unix 绝对路径 / file://）
        if (key.matches("^[A-Za-z]:\\\\.*")) return null;
        if (key.startsWith("file://")) return null;

        // 过滤完整 URL，只接受 R2 相对 key
        if (key.startsWith("http://") || key.startsWith("https://")) return null;

        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    private String buildAvatarKey(String roleDir, long id, String displayName) {
        String safeName = sanitizeName(displayName);
        long ts = System.currentTimeMillis();
        return "avatars/" + roleDir + "/" + id + "_" + safeName + "_" + ts + ".png";
    }

    private String sanitizeName(String name) {
        if (name == null) return "user";
        String normalized = name.trim().toLowerCase();
        if (normalized.isEmpty()) return "user";
        normalized = normalized.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        if (normalized.isEmpty()) return "user";
        if (normalized.length() > 32) {
            normalized = normalized.substring(0, 32);
        }
        return normalized;
    }

    private String detectImageMediaType(String filePath) {
        String lower = filePath == null ? "" : filePath.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private Student getStudentById(long studentId) {
        try (android.database.Cursor c = databaseHelper.getStudentById(studentId)) {
            if (c != null && c.moveToFirst()) {
                Student student = new Student();
                student.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                student.setClassId(c.getLong(c.getColumnIndexOrThrow("classId")));
                student.setName(c.getString(c.getColumnIndexOrThrow("name")));
                student.setSid(c.getString(c.getColumnIndexOrThrow("sid")));
                student.setGender(c.getString(c.getColumnIndexOrThrow("gender")));
                student.setAvatarUri(c.getString(c.getColumnIndexOrThrow("avatarUri")));
                return student;
            }
        }
        return null;
    }

    private void updateLocalStudentAvatar(long studentId, String avatarKey) {
        Student student = getStudentById(studentId);
        if (student == null) return;
        student.setAvatarUri(avatarKey);
        databaseHelper.updateStudent(student);
    }

    @Override
    public void changeStudentUsername(long studentId, String newName, ApiCallback<ApiResponse> callback) {
        ChangeStudentUsernameRequest request = new ChangeStudentUsernameRequest(studentId, newName);
        apiService.changeStudentUsername(getApiKey(), request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body());
                } else {
                    String error = "Unknown error";
                    if (response.body() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception ignored) { }
                    }
                    callback.onError("Student username change failed: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error during student username change: " + t.getMessage());
            }
        });
    }
}
