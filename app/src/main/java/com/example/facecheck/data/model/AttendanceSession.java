package com.example.facecheck.data.model;

public class AttendanceSession {
    private long id;
    private long classId;
    private long teacherId;
    private long startedAt;
    private long endedAt;
    private String location;
    private String photoUri;
    private String note;
    private String status;
    private String attendanceType;

    public AttendanceSession() {
        this.startedAt = System.currentTimeMillis();
        this.attendanceType = "FACE";
        this.status = "ACTIVE";
    }

    public AttendanceSession(long classId) {
        this();
        this.classId = classId;
    }

    public AttendanceSession(long id, long classId, long teacherId, long startedAt, long endedAt,
            String location, String photoUri, String note, String status, String attendanceType) {
        this.id = id;
        this.classId = classId;
        this.teacherId = teacherId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.location = location;
        this.photoUri = photoUri;
        this.note = note;
        this.status = status;
        this.attendanceType = attendanceType;
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

    public long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(long teacherId) {
        this.teacherId = teacherId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttendanceType() {
        return attendanceType;
    }

    public void setAttendanceType(String attendanceType) {
        this.attendanceType = attendanceType;
    }
}