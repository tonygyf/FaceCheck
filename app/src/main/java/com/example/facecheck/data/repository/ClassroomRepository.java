package com.example.facecheck.data.repository;

import androidx.lifecycle.LiveData;
import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.model.ClassroomDeltaSyncResponse;
import java.util.List;

public interface ClassroomRepository {
    LiveData<List<Classroom>> getClassrooms(long teacherId);
    void createClassroom(long teacherId, String name, int year, final ApiCallback<Classroom> callback);
    void syncAllClassrooms(long teacherId, final ApiCallback<List<Classroom>> callback);
    void syncCheckinTasks(long teacherId, ApiCallback<List<com.example.facecheck.api.CheckinTaskListResponse.CheckinTask>> callback);
    void syncClassroomDeltas(long lastSyncTimestamp, final ApiCallback<ClassroomDeltaSyncResponse> callback);
}
