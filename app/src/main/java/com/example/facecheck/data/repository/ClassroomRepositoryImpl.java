package com.example.facecheck.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.facecheck.api.ApiCreateResponse;
import com.example.facecheck.api.ApiService;
import com.example.facecheck.api.RetrofitClient;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.api.CheckinTaskListResponse;
import com.example.facecheck.data.model.ClassroomDeltaSyncResponse;
import com.example.facecheck.data.model.ClassroomListResponse;
import com.example.facecheck.data.model.SyncDownloadResponse;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassroomRepositoryImpl implements ClassroomRepository {

    private static final String TAG = "ClassroomRepository";
    private static final String PREFS_NAME = "FaceCheckPrefs";
    private static final String KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp";

    private final DatabaseHelper dbHelper;
    private final ApiService apiService;
    private final SharedPreferences preferences;
    private final SessionManager sessionManager;

    public ClassroomRepositoryImpl(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.apiService = RetrofitClient.getApiService();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.sessionManager = new SessionManager(context);
    }

    private String getApiKey() {
        return sessionManager.getApiKey();
    }

    @Override
    public LiveData<List<Classroom>> getClassrooms(long teacherId) {
        final MutableLiveData<List<Classroom>> data = new MutableLiveData<>();

        // 首先尝试从远程同步数据
        syncAllClassrooms(teacherId, new ApiCallback<List<Classroom>>() {
            @Override
            public void onSuccess(List<Classroom> remoteClassrooms) {
                // 远程同步成功，直接使用这份包含正确学生数量的远程数据更新UI
                data.postValue(remoteClassrooms);
                Log.d(TAG, "getClassrooms: 远程同步成功，直接使用远程数据更新UI。");
            }

            @Override
            public void onError(String message) {
                // 远程同步失败，可能是网络问题或其他API错误，回退到从本地数据库加载
                Log.e(TAG, "getClassrooms: 远程同步失败，尝试加载本地数据: " + message);
                List<Classroom> localClassrooms = dbHelper.getAllClassroomsWithStudentCountAsList(teacherId);
                data.postValue(localClassrooms);
            }
        });
        return data;
    }

    @Override
    public void createClassroom(long teacherId, String name, int year, ApiCallback<Classroom> callback) {
        Classroom newClassroom = new Classroom(0, teacherId, name, year, null, null, null, System.currentTimeMillis());

        apiService.createClassroom(getApiKey(), newClassroom).enqueue(new Callback<ApiCreateResponse>() {
            @Override
            public void onResponse(Call<ApiCreateResponse> call, Response<ApiCreateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    // Per user suggestion, we no longer need to write to the local DB here,
                    // because the calling ViewModel will trigger a full refresh (loadClassrooms)
                    // which will fetch the new classroom from the server anyway.
                    // dbHelper.insertOrUpdateClassroom(newClassroom);

                    callback.onSuccess(newClassroom);
                } else {
                    String error = "Unknown error";
                    if(response.body() != null) error = response.body().getError();
                    callback.onError("Failed to create classroom on server: " + error);
                }
            }

            @Override
            public void onFailure(Call<ApiCreateResponse> call, Throwable t) {
                callback.onError("Network error during classroom creation: " + t.getMessage());
            }
        });
    }

    @Override
    public void syncAllClassrooms(long teacherId, final ApiCallback<List<Classroom>> callback) {
        // KEY-FIX: Corrected to use the available /api/classrooms endpoint which returns a JSON object.
        apiService.getClassrooms(getApiKey()).enqueue(new Callback<ClassroomListResponse>() {
            @Override
            public void onResponse(Call<ClassroomListResponse> call, Response<ClassroomListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Log.d(TAG, "Sync successful. Response code: " + response.code() + ", Body size: " + response.body().getData().size());
                    List<Classroom> remoteClassrooms = response.body().getData();
                    
                    // Clear existing classrooms and insert new ones. This is a full sync.
                    dbHelper.deleteAllClassroomsForTeacher(teacherId);
                    for (Classroom classroom : remoteClassrooms) {
                        // The response from /api/classrooms does not include teacherId, so we set it here.
                        classroom.setTeacherId(teacherId);
                        dbHelper.insertOrUpdateClassroom(classroom);
                    }
                    callback.onSuccess(remoteClassrooms);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception e) { /* ignore */ }
                    Log.e(TAG, "Sync failed. Response code: " + response.code() + ", Error body: " + errorBody);
                    callback.onError("Failed to sync all classrooms: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ClassroomListResponse> call, Throwable t) {
                Log.e(TAG, "Sync network failure", t);
                callback.onError("Network error during full classroom sync: " + t.getMessage());
            }
        });
    }

    @Override
    public void syncCheckinTasks(long teacherId, ApiCallback<List<CheckinTaskListResponse.CheckinTask>> callback) {
        apiService.getCheckinTasks(getApiKey(), teacherId).enqueue(new Callback<CheckinTaskListResponse>() {
            @Override
            public void onResponse(Call<CheckinTaskListResponse> call, Response<CheckinTaskListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<CheckinTaskListResponse.CheckinTask> tasks = response.body().data;
                    callback.onSuccess(tasks);
                } else {
                    callback.onError("Failed to fetch check-in tasks: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<CheckinTaskListResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    @Override
    public void syncClassroomDeltas(long lastSyncTimestamp, final ApiCallback<ClassroomDeltaSyncResponse> callback) {
        apiService.getClassesDelta(getApiKey(), lastSyncTimestamp).enqueue(new Callback<ClassroomDeltaSyncResponse>() {
            @Override
            public void onResponse(Call<ClassroomDeltaSyncResponse> call, Response<ClassroomDeltaSyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClassroomDeltaSyncResponse deltaResponse = response.body();
                    // Process added classes
                    if (deltaResponse.getAddedClasses() != null) {
                        for (Classroom classroom : deltaResponse.getAddedClasses()) {
                            dbHelper.insertOrUpdateClassroom(classroom);
                        }
                    }
                    // Process updated classes
                    if (deltaResponse.getUpdatedClasses() != null) {
                        for (Classroom classroom : deltaResponse.getUpdatedClasses()) {
                            dbHelper.updateClassroom(classroom);
                        }
                    }
                    // Process deleted class IDs
                    if (deltaResponse.getDeletedClassIds() != null) {
                        for (Long classId : deltaResponse.getDeletedClassIds()) {
                            dbHelper.deleteClassroom(classId);
                        }
                    }
                    
                    // Save the new last sync timestamp
                    preferences.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, deltaResponse.getNewLastSyncTimestamp()).apply();

                    callback.onSuccess(deltaResponse);
                } else {
                    callback.onError("Failed to sync classroom deltas: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ClassroomDeltaSyncResponse> call, Throwable t) {
                callback.onError("Network error during incremental classroom sync: " + t.getMessage());
            }
        });
    }

    // Helper method to get the last sync timestamp
    public long getLastSyncTimestamp() {
        return preferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L); // 0L for initial sync
    }

    // Helper method to set the last sync timestamp (can be used after a successful full sync)
    public void setLastSyncTimestamp(long timestamp) {
        preferences.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply();
    }
}

