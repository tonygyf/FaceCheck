// CheckinTaskRequest.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class CheckinTaskRequest {
    @SerializedName("classId")    public long classId;
    @SerializedName("title")      public String title;
    @SerializedName("expiresAt")  public String expiresAt; // ISO 8601
    @SerializedName("note")       public String note;

    public CheckinTaskRequest(long classId, String title, String expiresAt) {
        this.classId = classId;
        this.title = title;
        this.expiresAt = expiresAt;
    }
}