package com.example.facecheck.api;

import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.model.ClassroomDeltaSyncResponse;
import com.example.facecheck.data.model.SyncDownloadResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // 基础URL在RetrofitClient中定义
    String BASE_URL = "https://omni.gyf123.dpdns.org/api/";

    @GET("sync/download")
    Call<SyncDownloadResponse> downloadSyncData(@Header("X-API-Key") String apiKey, @Query("teacherId") long teacherId);

    @GET("v1/classes/delta")
    Call<ClassroomDeltaSyncResponse> getClassesDelta(@Header("X-API-Key") String apiKey, @Query("lastSyncTimestamp") long lastSyncTimestamp);

    @POST("classrooms")
    Call<ApiCreateResponse> createClassroom(@Header("X-API-Key") String apiKey, @Body Classroom classroom);
}
