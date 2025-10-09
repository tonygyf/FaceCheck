package com.example.facecheck.data.model;

public class Teacher {
    private long id;
    private String name;
    private String username;
    private String password;
    private String avatarUri; // 头像图片路径

    private long createdAt;
    private long updatedAt;

    public Teacher() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Teacher(long id, String name, String username, String password, String avatarUri, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.avatarUri = avatarUri;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.updatedAt = System.currentTimeMillis();
    }



    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public void setAvatarUri(String avatarUri) {
        this.avatarUri = avatarUri;
        this.updatedAt = System.currentTimeMillis();
    }
}