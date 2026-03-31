// CheckinSubmitRequest.java - 学生提交签到
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class CheckinSubmitRequest {
    @SerializedName("studentId")     public long studentId;
    @SerializedName("lat")           public Double lat;
    @SerializedName("lng")           public Double lng;
    @SerializedName("gestureInput")  public String gestureInput;
    @SerializedName("passwordInput") public String passwordInput;

    // imageUri 通常通过 Multipart 方式单独上传，而不是放在这个JSON请求体中

    public CheckinSubmitRequest(long studentId) {
        this.studentId = studentId;
    }
}