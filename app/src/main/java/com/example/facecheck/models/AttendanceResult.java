package com.example.facecheck.models;

public class AttendanceResult {
    private long id;
    private long sessionId;
    private long studentId;
    private String status; // Present, Absent, Unknown
    private float score;
    private String decidedBy; // AUTO, TEACHER
    private long decidedAt;

    public AttendanceResult() {
        this.decidedAt = System.currentTimeMillis();
    }

    public AttendanceResult(long sessionId, long studentId, String status, float score, String decidedBy) {
        this();
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
        this.score = score;
        this.decidedBy = decidedBy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public long getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(long decidedAt) {
        this.decidedAt = decidedAt;
    }
}