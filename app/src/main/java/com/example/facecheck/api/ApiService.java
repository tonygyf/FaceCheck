package com.example.facecheck.api;

import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.model.ClassroomDeltaSyncResponse;
import com.example.facecheck.data.model.ClassroomListResponse;
import com.example.facecheck.data.model.StudentListResponse;

import retrofit2.Call;
import retrofit2.http.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface ApiService {

    String BASE_URL = "https://omni.gyf123.dpdns.org/api/";

        @POST("auth/login")
    Call<TeacherLoginResponse> loginTeacher(@Header("X-API-Key") String apiKey, @Body TeacherLoginRequest body);

    @POST("auth/register")
    Call<ApiCreateResponse> registerTeacher(@Body TeacherRegisterRequest body);

    @POST("login/student")
    Call<StudentLoginResponse> loginStudent(@Header("X-API-Key") String apiKey, @Body StudentLoginRequest body);

    // ===== 已有接口（保持不变）=====

    @GET("classrooms")
    Call<ClassroomListResponse> getClassrooms(@Header("X-API-Key") String apiKey);

    @POST("classrooms")
    Call<ApiCreateResponse> createClassroom(@Header("X-API-Key") String apiKey, @Body Classroom classroom);

    @PUT("auth/change-password")
    Call<ApiResponse> changePassword(@Header("X-API-Key") String apiKey, @Body ChangePasswordRequest body);

    @GET("v1/classes/delta")
    Call<ClassroomDeltaSyncResponse> getClassesDelta(
            @Header("X-API-Key") String apiKey,
            @Query("lastSyncTimestamp") long lastSyncTimestamp);

    @PUT("profile/username")
    Call<ApiResponse> changeUsername(@Header("X-API-Key") String apiKey, @Body ChangeUsernameRequest body);

    @PUT("student/profile/username")
    Call<ApiResponse> changeStudentUsername(@Header("X-API-Key") String apiKey, @Body ChangeStudentUsernameRequest body);

    @PUT("student/profile/password")
    Call<ApiResponse> changeStudentPassword(@Header("X-API-Key") String apiKey, @Body ChangeStudentPasswordRequest body);

    @Multipart
    @POST("profile/avatar")
    Call<ApiResponse> uploadAvatar(
            @Header("X-API-Key") String apiKey,
            @Part("teacherId") RequestBody teacherId,
            @Part("key") RequestBody key,
            @Part MultipartBody.Part file);

    // ===== 新增：学生接口 =====

    @GET("students")
    Call<StudentListResponse> getStudents(
            @Header("X-API-Key") String apiKey,
            @Query("classId") long classId);

    @POST("students")
    Call<ApiCreateResponse> createStudent(
            @Header("X-API-Key") String apiKey,
            @Body StudentRequest body);

    @POST("students/batch")
    Call<ApiResponse> batchCreateStudents(
            @Header("X-API-Key") String apiKey,
            @Body BatchStudentRequest body);

    @PUT("students/{id}")
    Call<ApiResponse> updateStudent(
            @Header("X-API-Key") String apiKey,
            @Path("id") long studentId,
            @Body StudentRequest body);

    @DELETE("students/{id}")
    Call<ApiResponse> deleteStudent(
            @Header("X-API-Key") String apiKey,
            @Path("id") long studentId);

    // ===== 新增：签到任务接口（教师端）=====

    @GET("checkin/tasks")
    Call<CheckinTaskListResponse> getCheckinTasks(@Header("X-API-Key") String apiKey, @Query("teacherId") long teacherId);

    @POST("checkin/tasks")
    Call<ApiCreateResponse> createCheckinTask(
            @Header("X-API-Key") String apiKey,
            @Body CheckinTaskRequest body);

    @POST("checkin/tasks/{id}/close")
    Call<ApiResponse> closeCheckinTask(
            @Header("X-API-Key") String apiKey,
            @Path("id") long taskId);

    // ===== 新增：签到提交接口（学生端）=====

    @POST("checkin/tasks/{id}/submit")
    Call<ApiCreateResponse> submitCheckin(
            @Header("X-API-Key") String apiKey,
            @Path("id") long taskId,
            @Body CheckinSubmitRequest body);

    @GET("checkin/tasks/{id}/review-queue")
    Call<ReviewQueueResponse> getReviewQueue(
            @Header("X-API-Key") String apiKey,
            @Path("id") long taskId);

    @POST("checkin/submissions/{id}/review")
    Call<ApiResponse> reviewSubmission(
            @Header("X-API-Key") String apiKey,
            @Path("id") long submissionId,
            @Body ReviewRequest body);

    @GET("checkin/submissions/my")
    Call<MySubmissionsResponse> getMySubmissions(
            @Header("X-API-Key") String apiKey,
            @Query("studentId") long studentId);

    @POST("checkin/submissions/{id}/appeal")
    Call<ApiResponse> appealSubmission(
            @Header("X-API-Key") String apiKey,
            @Path("id") long submissionId,
            @Body AppealRequest body);

    // ===== 新增：考勤同步上传 =====

    @POST("sync/upload")
    Call<SyncUploadResponse> uploadAttendanceSync(
            @Header("X-API-Key") String apiKey,
            @Body SyncUploadRequest body);
}
