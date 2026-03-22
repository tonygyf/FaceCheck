package com.example.facecheck.data.repository;

import com.example.facecheck.api.ApiResponse;

public interface ProfileRepository {
    void changePassword(long teacherId, String oldPassword, String newPassword, ApiCallback<ApiResponse> callback);
    void uploadAvatar(long teacherId, String filePath, ApiCallback<ApiResponse> callback);
    void changeUsername(long teacherId, String newName, ApiCallback<ApiResponse> callback);
}
