package com.example.facecheck.api;

import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.model.ClassroomDeltaSyncResponse;
import com.example.facecheck.data.model.ClassroomListResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Multipart;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface ApiService {

    // 基础URL在RetrofitClient中定义
    String BASE_URL = "https://omni.gyf123.dpdns.org/api/";

    @GET("classrooms")
    Call<ClassroomListResponse> getClassrooms(@Header("X-API-Key") String apiKey);

    @POST("classrooms")
    Call<ApiCreateResponse> createClassroom(@Header("X-API-Key") String apiKey, @Body Classroom classroom);

    @PUT("auth/change-password")
    Call<ApiResponse> changePassword(@Header("X-API-Key") String apiKey, @Body ChangePasswordRequest body);

    @GET("v1/classes/delta")
    Call<ClassroomDeltaSyncResponse> getClassesDelta(@Header("X-API-Key") String apiKey, @Query("lastSyncTimestamp") long lastSyncTimestamp);

    @PUT("profile/username")
    Call<ApiResponse> changeUsername(@Header("X-API-Key") String apiKey, @Body ChangeUsernameRequest body);

    @Multipart
    @POST("profile/avatar")
    Call<ApiResponse> uploadAvatar(
        @Header("X-API-Key") String apiKey,
        @Part("teacherId") RequestBody teacherId,
        @Part("key") RequestBody key,
        @Part MultipartBody.Part file
    );
}
