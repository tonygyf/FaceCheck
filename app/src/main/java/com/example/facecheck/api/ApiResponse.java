package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private String error;

    @SerializedName("data")
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("id")
        private long id;
        @SerializedName("avatarUri")
        private String avatarUri;

        public long getId() {
            return id;
        }

        public String getAvatarUri() {
            return avatarUri;
        }
    }
}
