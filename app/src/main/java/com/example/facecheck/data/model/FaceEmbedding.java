package com.example.facecheck.data.model;

public class FaceEmbedding {
    private long id;
    private long studentId;
    private String modelVer;
    private byte[] vector;
    private float quality;
    private long createdAt;

    public FaceEmbedding() {
        this.createdAt = System.currentTimeMillis();
    }

    public FaceEmbedding(long studentId, String modelVer, byte[] vector, float quality) {
        this();
        this.studentId = studentId;
        this.modelVer = modelVer;
        this.vector = vector;
        this.quality = quality;
    }

    public FaceEmbedding(long id, long studentId, String modelVer, byte[] vector, float quality, long createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.modelVer = modelVer;
        this.vector = vector;
        this.quality = quality;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getModelVer() {
        return modelVer;
    }

    public void setModelVer(String modelVer) {
        this.modelVer = modelVer;
    }

    public byte[] getVector() {
        return vector;
    }

    public void setVector(byte[] vector) {
        this.vector = vector;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}