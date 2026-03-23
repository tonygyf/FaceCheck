// StudentRequest.java - 创建/更新学生的请求体
package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class StudentRequest {
    @SerializedName("classId") public long classId;
    @SerializedName("name")    public String name;
    @SerializedName("sid")     public String sid;
    @SerializedName("gender")  public String gender;
    @SerializedName("email")   public String email;
    @SerializedName("password") public String password;
    @SerializedName("avatarUri") public String avatarUri;

    public StudentRequest(long classId, String name, String sid, String gender) {
        this.classId = classId;
        this.name = name;
        this.sid = sid;
        this.gender = gender;
    }
}