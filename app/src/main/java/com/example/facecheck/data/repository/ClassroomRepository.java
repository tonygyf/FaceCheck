package com.example.facecheck.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.database.DatabaseHelper;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassroomRepository {

    private static final String TAG = "ClassroomRepository";
    private final ApiService apiService;
    private final DatabaseHelper dbHelper;
    private final String apiKey = "my-secret-api-key"; // FIXME: Should be stored securely

    public ClassroomRepository(Context context) {
        this.apiService = RetrofitClient.getApiService();
        this.dbHelper = new DatabaseHelper(context);
    }

    public LiveData<List<Classroom>> getClassrooms(long teacherId) {
        final MutableLiveData<List<Classroom>> data = new MutableLiveData<>();

        // Step 1: Immediately load data from local database
        List<Classroom> localClassrooms = dbHelper.getAllClassroomsWithStudentCountAsList(teacherId);
        data.setValue(localClassrooms);

        // Step 2: Fetch data from network
        apiService.getClassrooms(apiKey).enqueue(new Callback<com.example.facecheck.api.ApiResponse<List<Classroom>>>() {
            @Override
            public void onResponse(Call<com.example.facecheck.api.ApiResponse<List<Classroom>>> call, Response<com.example.facecheck.api.ApiResponse<List<Classroom>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    // Step 3: Network data successfully fetched, save to DB and update LiveData
                    List<Classroom> networkClassrooms = response.body().data;
                    saveClassroomsToDb(teacherId, networkClassrooms);
                    // Reload from DB to ensure data consistency (including student count)
                    List<Classroom> updatedClassrooms = dbHelper.getAllClassroomsWithStudentCountAsList(teacherId);
                    data.postValue(updatedClassrooms);
                } else {
                    Log.e(TAG, "Failed to fetch classrooms from network");
                }
            }

            @Override
            public void onFailure(Call<com.example.facecheck.api.ApiResponse<List<Classroom>>> call, Throwable t) {
                Log.e(TAG, "Network request failed", t);
                // Network failed, UI is already showing local data, so no need to do anything
            }
        });

        return data;
    }

    private void saveClassroomsToDb(long teacherId, List<Classroom> classrooms) {
        if (classrooms == null || classrooms.isEmpty()) {
            Log.d(TAG, "No classrooms from network to save.");
            return;
        }
        dbHelper.replaceTeacherClassrooms(teacherId, classrooms);
        Log.d(TAG, "Saved " + classrooms.size() + " classrooms to database.");
    }
}
