package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class CheckinPhotoUploadResponse {
    @SerializedName("ok")
    private boolean ok;

    @SerializedName("key")
    private String key;

    @SerializedName("error")
    private String error;

    public boolean isOk() {
        return ok;
    }

    public String getKey() {
        return key;
    }

    public String getError() {
        return error;
    }
}
