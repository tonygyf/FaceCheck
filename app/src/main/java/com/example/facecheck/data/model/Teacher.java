package com.example.facecheck.data.model;

public class Teacher {
    private long id;
    private String name;
    private String username;
    private String password;
    private String email;
    private String davUrl;
    private String davUser;
    private String davKeyEnc;
    private long createdAt;
    private long updatedAt;

    public Teacher() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Teacher(String name, String email) {
        this();
        this.name = name;
        this.email = email;
    }

    public Teacher(long id, String name, String username, String password, String email, String davUrl, String davUser, String davKeyEnc, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.davUrl = davUrl;
        this.davUser = davUser;
        this.davKeyEnc = davKeyEnc;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDavUrl() {
        return davUrl;
    }

    public void setDavUrl(String davUrl) {
        this.davUrl = davUrl;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDavUser() {
        return davUser;
    }

    public void setDavUser(String davUser) {
        this.davUser = davUser;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDavKeyEnc() {
        return davKeyEnc;
    }

    public void setDavKeyEnc(String davKeyEnc) {
        this.davKeyEnc = davKeyEnc;
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
}