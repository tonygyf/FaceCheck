package com.example.facecheck.data.repository;

import android.content.Context;

import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.ApiResponse;
import com.example.facecheck.api.ChangePasswordRequest;
import com.example.facecheck.api.ChangeUsernameRequest;
import com.example.facecheck.api.RetrofitClient;
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

    public ProfileRepositoryImpl(Context context) {
        this.apiService = RetrofitClient.getApiService();
        this.sessionManager = new SessionManager(context);
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
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody teacherIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(teacherId));
        String key = "avatars/" + teacherId + "/" + System.currentTimeMillis() + ".jpg";
        RequestBody keyBody = RequestBody.create(MediaType.parse("text/plain"), key);

        apiService.uploadAvatar(getApiKey(), teacherIdBody, keyBody, body).enqueue(new Callback<ApiResponse>() {
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
}
