package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MySubmissionsResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<Item> data;

    public static class Item {
        @SerializedName("id")
        public long id;
        @SerializedName("taskId")
        public long taskId;
        @SerializedName("title")
        public String title;
        @SerializedName("classId")
        public long classId;
        @SerializedName("submittedAt")
        public String submittedAt;
        @SerializedName("finalResult")
        public String finalResult;
        @SerializedName("reason")
        public String reason;
        @SerializedName("gestureInput")
        public String gestureInput;
        @SerializedName("passwordInput")
        public String passwordInput;
    }
}
