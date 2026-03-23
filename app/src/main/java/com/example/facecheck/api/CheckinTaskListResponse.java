// CheckinTaskListResponse.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CheckinTaskListResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("data")    public List<CheckinTask> data;

    public static class CheckinTask {
        @SerializedName("id")        public long id;
        @SerializedName("classId")   public long classId;
        @SerializedName("title")     public String title;
        @SerializedName("status")    public String status;
        @SerializedName("expiresAt") public String expiresAt;
        @SerializedName("note")      public String note;
    }
}