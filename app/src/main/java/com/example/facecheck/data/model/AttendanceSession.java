package com.example.facecheck.data.model;

public class AttendanceSession {
    private long id;
    private long classId;
    private long startedAt;
    private String location;
    private String photoUri;
    private String note;

    public AttendanceSession() {
        this.startedAt = System.currentTimeMillis();
    }

    public AttendanceSession(long classId) {
        this();
        this.classId = classId;
    }

    public AttendanceSession(long id, long classId, long startedAt, String location, String photoUri, String note) {
        this.id = id;
        this.classId = classId;
        this.startedAt = startedAt;
        this.location = location;
        this.photoUri = photoUri;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}