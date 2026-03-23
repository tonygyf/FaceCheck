// BatchStudentRequest.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BatchStudentRequest {
    @SerializedName("classId")  public long classId;
    @SerializedName("students") public List<StudentRequest> students;

    public BatchStudentRequest(long classId, List<StudentRequest> students) {
        this.classId = classId;
        this.students = students;
    }
}