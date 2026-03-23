// SyncUploadResponse.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class SyncUploadResponse {
    @SerializedName("success")           public boolean success;
    @SerializedName("processedSessions") public int processedSessions;
}