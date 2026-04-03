package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class AppealRequest {
    @SerializedName("studentId")
    public long studentId;
    @SerializedName("reason")
    public String reason;

    public AppealRequest(long studentId, String reason) {
        this.studentId = studentId;
        this.reason = reason;
    }
}
