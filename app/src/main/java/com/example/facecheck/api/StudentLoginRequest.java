package com.example.facecheck.api;

public class StudentLoginRequest {
    private String sid;
    private String password;

    public StudentLoginRequest(String sid, String password) {
        this.sid = sid;
        this.password = password;
    }

    // Getters and setters are not strictly necessary for Gson serialization,
    // but are good practice.
    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
