// ReviewQueueResponse.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewQueueResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("data")    public List<ReviewItem> data;

    public static class ReviewItem {
        @SerializedName("id")        public long id;
        @SerializedName("studentId") public long studentId;
        @SerializedName("taskId")    public long taskId;
        @SerializedName("imageUri")  public String imageUri;
        @SerializedName("status")    public String status;
    }
}