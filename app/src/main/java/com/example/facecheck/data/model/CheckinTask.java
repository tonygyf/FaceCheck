package com.example.facecheck.data.model;

import com.google.gson.annotations.SerializedName;

public class CheckinTask {
    @SerializedName("id")
    private long id;

    @SerializedName("classId")
    private long classId;

    @SerializedName("teacherId")
    private long teacherId;

    @SerializedName("title")
    private String title;

    @SerializedName("startAt")
    private String startAt;

    @SerializedName("endAt")
    private String endAt;

    @SerializedName("status")
    private String status;

    @SerializedName("locationLat")
    private Double locationLat;

    @SerializedName("locationLng")
    private Double locationLng;

    @SerializedName("locationRadiusM")
    private Integer locationRadiusM;

    @SerializedName("gestureSequence")
    private String gestureSequence;

    @SerializedName("passwordPlain")
    private String passwordPlain;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }
    public long getTeacherId() { return teacherId; }
    public void setTeacherId(long teacherId) { this.teacherId = teacherId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStartAt() { return startAt; }
    public void setStartAt(String startAt) { this.startAt = startAt; }
    public String getEndAt() { return endAt; }
    public void setEndAt(String endAt) { this.endAt = endAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double locationLat) { this.locationLat = locationLat; }
    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double locationLng) { this.locationLng = locationLng; }
    public Integer getLocationRadiusM() { return locationRadiusM; }
    public void setLocationRadiusM(Integer locationRadiusM) { this.locationRadiusM = locationRadiusM; }
    public String getGestureSequence() { return gestureSequence; }
    public void setGestureSequence(String gestureSequence) { this.gestureSequence = gestureSequence; }
    public String getPasswordPlain() { return passwordPlain; }
    public void setPasswordPlain(String passwordPlain) { this.passwordPlain = passwordPlain; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
