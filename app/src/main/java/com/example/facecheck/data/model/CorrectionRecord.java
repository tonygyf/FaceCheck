package com.example.facecheck.data.model;

public class CorrectionRecord {
    private int id;
    private String studentName;
    private String studentId;
    private String correctionType;
    private String correctionTime;
    private String description;

    public CorrectionRecord(int id, String studentName, String studentId, String correctionType, 
                          String correctionTime, String description) {
        this.id = id;
        this.studentName = studentName;
        this.studentId = studentId;
        this.correctionType = correctionType;
        this.correctionTime = correctionTime;
        this.description = description;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCorrectionType() {
        return correctionType;
    }

    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }

    public String getCorrectionTime() {
        return correctionTime;
    }

    public void setCorrectionTime(String correctionTime) {
        this.correctionTime = correctionTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}