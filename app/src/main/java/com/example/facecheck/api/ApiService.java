package com.example.facecheck.api;

import com.example.facecheck.data.model.Classroom;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

// 定义一个通用的API响应包装类
class ApiResponse<T> {
    public boolean success;
    public T data;
    public String error;
}

public interface ApiService {

    // 基础URL在RetrofitClient中定义
    String BASE_URL = "https://omni.gyf123.dpdns.org/api/";

    @GET("classrooms")
    Call<ApiResponse<List<Classroom>>> getClassrooms(@Header("X-API-Key") String apiKey);

    // 未来可以添加更多接口，例如：
    // @GET("students")
    // Call<ApiResponse<List<Student>>> getStudentsByClass(@Query("classId") long classId, @Header("X-API-Key") String apiKey);
}
