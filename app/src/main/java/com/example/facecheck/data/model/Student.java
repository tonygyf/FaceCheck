package com.example.facecheck.data.model;

public class Student {
    private long id;
    private long classId;
    private String name;
    private String sid;
    private String gender;
    private String avatarUri;
    private String faceFeatures;  // 人脸特征向量
    private String faceImagePath;   // 人脸图片路径
    private long createdAt;

    public Student() {
        this.createdAt = System.currentTimeMillis();
    }

    public Student(long classId, String name, String sid, String gender) {
        this();
        this.classId = classId;
        this.name = name;
        this.sid = sid;
        this.gender = gender;
    }

    public Student(long id, long classId, String name, String sid, String gender, String avatarUri, long createdAt) {
        this.id = id;
        this.classId = classId;
        this.name = name;
        this.sid = sid;
        this.gender = gender;
        this.avatarUri = avatarUri;
        this.createdAt = createdAt;
    }

    public Student(long id, long classId, String name, String sid, String gender, String avatarUri, String faceFeatures, String faceImagePath, long createdAt) {
        this.id = id;
        this.classId = classId;
        this.name = name;
        this.sid = sid;
        this.gender = gender;
        this.avatarUri = avatarUri;
        this.faceFeatures = faceFeatures;
        this.faceImagePath = faceImagePath;
        this.createdAt = createdAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public void setAvatarUri(String avatarUri) {
        this.avatarUri = avatarUri;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFaceFeatures() {
        return faceFeatures;
    }

    public void setFaceFeatures(String faceFeatures) {
        this.faceFeatures = faceFeatures;
    }

    public String getFaceImagePath() {
        return faceImagePath;
    }

    public void setFaceImagePath(String faceImagePath) {
        this.faceImagePath = faceImagePath;
    }
}