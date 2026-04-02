package com.example.facecheck.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateCheckinTaskRequest {
    @SerializedName("classId")
    public long classId;
    @SerializedName("teacherId")
    public long teacherId;
    @SerializedName("title")
    public String title;
    @SerializedName("startAt")
    public String startAt;
    @SerializedName("endAt")
    public String endAt;
    @SerializedName("status")
    public String status;
    @SerializedName("locationLat")
    public Double locationLat;
    @SerializedName("locationLng")
    public Double locationLng;
    @SerializedName("locationRadiusM")
    public Integer locationRadiusM;
    @SerializedName("gestureSequence")
    public String gestureSequence;
    @SerializedName("passwordPlain")
    public String passwordPlain;
}
