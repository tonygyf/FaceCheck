// ReviewRequest.java
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class ReviewRequest {
    @SerializedName("approved") public boolean approved;
    @SerializedName("note")     public String note;

    public ReviewRequest(boolean approved, String note) {
        this.approved = approved;
        this.note = note;
    }
}