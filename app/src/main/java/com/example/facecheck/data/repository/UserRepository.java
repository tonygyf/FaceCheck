package com.example.facecheck.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.facecheck.api.ApiCreateResponse;
import com.example.facecheck.api.ApiResponse;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.api.StudentLoginRequest;
import com.example.facecheck.api.StudentLoginResponse;
import com.example.facecheck.api.TeacherLoginRequest;
import com.example.facecheck.api.TeacherLoginResponse;
import com.example.facecheck.api.TeacherRegisterRequest;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final DatabaseHelper databaseHelper;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public UserRepository(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
        this.apiService = RetrofitClient.getApiService();
        this.sessionManager = new SessionManager(context);
    }

    public static class UserLoginResult {
        public final long userId;
        public final String role;
        public final String username;
        public final String name;
        public final String avatarUri;
        public final String accessToken;
        public final String refreshToken;

        public UserLoginResult(long userId, String role, String username, String name, String avatarUri, String accessToken, String refreshToken) {
            this.userId = userId;
            this.role = role;
            this.username = username;
            this.name = name;
            this.avatarUri = avatarUri;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public interface LoginCallback {
        void onSuccess(UserLoginResult result);
        void onError(String message);
    }

    public void loginAny(String username, String password, final LoginCallback callback) {
        TeacherLoginRequest request = new TeacherLoginRequest(username, password);
        apiService.loginTeacher(sessionManager.getApiKey(), request).enqueue(new Callback<TeacherLoginResponse>() {
            @Override
            public void onResponse(Call<TeacherLoginResponse> call, Response<TeacherLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeacherLoginResponse.TeacherData data = response.body().getData();
                    UserLoginResult result = new UserLoginResult(
                        data.getId(),
                        data.getRole(),
                        data.getUsername(),
                        data.getName(),
                        data.getAvatarUri(),
                        data.getAccessToken(),
                        data.getRefreshToken()
                    );
                    callback.onSuccess(result);
                } else {
                    callback.onError("用户名或密码错误");
                }
            }

            @Override
            public void onFailure(Call<TeacherLoginResponse> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    public void loginStudent(String sid, String password, final LoginCallback callback) {
        StudentLoginRequest request = new StudentLoginRequest(sid, password);
        apiService.loginStudent(sessionManager.getApiKey(), request).enqueue(new Callback<StudentLoginResponse>() {
            @Override
            public void onResponse(Call<StudentLoginResponse> call, Response<StudentLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    StudentLoginResponse.StudentData data = response.body().getData();

                    // 登录成功后把学生基本信息写入本地数据库
                    Student student = new Student(
                            data.getId(),
                            data.getClassId(),
                            data.getName(),
                            data.getSid(),
                            null,   // gender
                            data.getAvatarUri(),   // avatarUri
                            null    // createdAt
                    );
                    databaseHelper.insertOrUpdateStudent(student);

                    UserLoginResult result = new UserLoginResult(
                        data.getId(),
                        data.getRole(),
                        data.getSid(), // Using sid as username for consistency
                        data.getName(),
                       null, // Student avatar  is not in this response
                        data.getAccessToken(),
                        data.getRefreshToken()
                    );
                    callback.onSuccess(result);
                } else {
                    callback.onError("学号或密码错误");
                }
            }

            @Override
            public void onFailure(Call<StudentLoginResponse> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    public interface RegisterCallback {
        void onSuccess();
        void onError(String message);
    }

    public void registerTeacher(String name, String username, String password, final RegisterCallback callback) {
        TeacherRegisterRequest request = new TeacherRegisterRequest(name, username, password);
        apiService.registerTeacher(request).enqueue(new Callback<ApiCreateResponse>() {
            @Override
            public void onResponse(Call<ApiCreateResponse> call, Response<ApiCreateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    callback.onSuccess();
                } else {
                    callback.onError("注册失败，请检查用户名是否已存在");
                }
            }

            @Override
            public void onFailure(Call<ApiCreateResponse> call, Throwable t) {
                callback.onError("网络请求失败: " + t.getMessage());
            }
        });
    }

    public Teacher getTeacherById(long teacherId) {
        return databaseHelper.getTeacherById(teacherId);
    }

    public boolean updateTeacher(Teacher teacher) {
        return databaseHelper.updateTeacher(teacher);
    }
}
