package com.example.facecheck.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.ApiCreateResponse;
import com.example.facecheck.api.ApiResponse;
import com.example.facecheck.api.BatchStudentRequest;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.api.StudentRequest;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.data.model.StudentListResponse;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentRepository {

    private static final String TAG = "StudentRepository";

    private final DatabaseHelper dbHelper;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public StudentRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.apiService = RetrofitClient.getApiService();
        this.sessionManager = new SessionManager(context);
    }

    public interface StudentsCallback {
        void onSuccess(List<Student> students);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    /**
     * 获取班级学生列表。
     * 策略：先返回本地缓存，同时从服务端拉取最新数据更新本地并回调。
     */
    public void getStudentsByClass(long classId, StudentsCallback callback) {
        // 1. 立即返回本地数据
        List<Student> local = getLocalStudents(classId);
        callback.onSuccess(local);

        // 2. 后台从服务端同步
        String apiKey = sessionManager.getApiKey();
        apiService.getStudents(apiKey, classId).enqueue(new Callback<StudentListResponse>() {
            @Override
            public void onResponse(Call<StudentListResponse> call, Response<StudentListResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null) {
                    // 将服务端数据同步到本地DB
                    syncToLocal(classId, response.body().data);
                    // 再次回调，返回最新数据
                    callback.onSuccess(getLocalStudents(classId));
                }
                // 服务端失败不影响已返回的本地数据
            }

            @Override
            public void onFailure(Call<StudentListResponse> call, Throwable t) {
                Log.w(TAG, "同步学生列表失败（使用本地缓存）: " + t.getMessage());
            }
        });
    }

    /**
     * 新增单个学生。
     * 策略：先写本地DB，再同步服务端。
     */
    public void createStudent(long classId, String name, String sid,
                              String gender, String email, ActionCallback callback) {
        StudentRequest req = new StudentRequest(classId, name, sid, gender);
        req.email = email;
        apiService.createStudent(sessionManager.getApiKey(), req).enqueue(new Callback<ApiCreateResponse>() {
            @Override
            public void onResponse(Call<ApiCreateResponse> call, Response<ApiCreateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    callback.onSuccess();
                } else {
                    String error = "Failed to create student";
                    if (response.body() != null && response.body().getError() != null) {
                        error = response.body().getError();
                    } else if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (java.io.IOException e) {
                            // ignore
                        }
                    }
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<ApiCreateResponse> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    public void updateStudent(long studentId, long classId, String name, String sid, String gender, String email, ActionCallback callback) {
        StudentRequest req = new StudentRequest(classId, name, sid, gender);
        req.email = email;
        apiService.updateStudent(sessionManager.getApiKey(), studentId, req).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    // 服务端更新成功后，同步更新本地数据库
                    Student student = new Student(studentId, classId, name, sid, gender, null, null); // Avatar and createdAt are not updated here
                    dbHelper.updateStudent(student); 
                    callback.onSuccess();
                } else {
                    callback.onError("服务端更新失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    public void deleteStudent(long studentId, ActionCallback callback) {
        apiService.deleteStudent(sessionManager.getApiKey(), studentId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    // 服务端删除成功后，同步删除本地记录
                    dbHelper.deleteStudent(studentId);
                    callback.onSuccess();
                } else {
                    callback.onError("服务端删除失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("网络错误: " + t.getMessage());
            }
        });
    }

    /**
     * 批量导入学生（如从 Excel/CSV 导入）。
     */
    public void batchCreateStudents(long classId, List<StudentRequest> students,
                                    ActionCallback callback) {
        // 1. 批量写本地
        for (StudentRequest s : students) {
            dbHelper.insertStudent(classId, s.name, s.sid, s.gender, null);
        }
        callback.onSuccess();

        // 2. 批量同步服务端
        BatchStudentRequest req = new BatchStudentRequest(classId, students);
        apiService.batchCreateStudents(sessionManager.getApiKey(), req)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call call, Response response) {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "批量同步失败: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(Call call, Throwable t) {
                        Log.w(TAG, "批量同步网络失败: " + t.getMessage());
                    }
                });
    }

    // ===== 私有辅助方法 =====

    private List<Student> getLocalStudents(long classId) {
        List<Student> list = new ArrayList<>();
        android.database.Cursor cursor = dbHelper.getStudentsByClass(classId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new Student(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("classId")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sid")),
                        cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                        cursor.getString(cursor.getColumnIndexOrThrow("avatarUri")),
                        cursor.getString(cursor.getColumnIndexOrThrow("createdAt"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * 将服务端返回的学生列表同步覆盖到本地DB。
     * 使用 INSERT OR REPLACE 策略，以服务端ID为准。
     */
    private void syncToLocal(long classId,
                             List<StudentListResponse.StudentApiModel> serverStudents) {
        // 首先，删除该班级在本地的所有学生记录
        dbHelper.deleteAllStudentsByClass(classId);

        // 然后，插入从服务器获取的最新学生记录
        for (StudentListResponse.StudentApiModel s : serverStudents) {
            // 用服务端数据 upsert 本地记录
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("id", s.id);
            values.put("classId", s.classId > 0 ? s.classId : classId);
            values.put("name", s.name);
            values.put("sid", s.sid);
            values.put("gender", s.gender);
            values.put("avatarUri", s.avatarUri);
            values.put("createdAt", s.createdAt);

            dbHelper.getWritableDatabase().insertWithOnConflict(
                    "Student", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        }
        Log.d(TAG, "强制覆盖同步了 " + serverStudents.size() + " 条学生数据到本地，班级ID: " + classId);
    }
}