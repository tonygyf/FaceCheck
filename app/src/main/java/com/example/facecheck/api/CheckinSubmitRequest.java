// CheckinSubmitRequest.java - 学生提交签到
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class CheckinSubmitRequest {
    @SerializedName("studentId") public long studentId;
    @SerializedName("imageUri")  public String imageUri;  // 可选，人脸图片
    @SerializedName("note")      public String note;

    public CheckinSubmitRequest(long studentId) {
        this.studentId = studentId;
    }
}