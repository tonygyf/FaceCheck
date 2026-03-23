// SyncUploadRequest.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SyncUploadRequest {
    @SerializedName("teacherId") public long teacherId;
    @SerializedName("sessions")  public List<SessionPayload> sessions;

    public SyncUploadRequest(long teacherId, List<SessionPayload> sessions) {
        this.teacherId = teacherId;
        this.sessions = sessions;
    }

    public static class SessionPayload {
        @SerializedName("classId")    public long classId;
        @SerializedName("startedAt")  public String startedAt;
        @SerializedName("location")   public String location;
        @SerializedName("note")       public String note;
        @SerializedName("results")    public List<ResultPayload> results;
    }

    public static class ResultPayload {
        @SerializedName("studentId")  public long studentId;
        @SerializedName("status")     public String status;
        @SerializedName("score")      public float score;
        @SerializedName("decidedBy")  public String decidedBy;
        @SerializedName("decidedAt")  public String decidedAt;
    }
}