package com.example.facecheck.api;

public class TeacherLoginRequest {
    private String username;
    private String password;

    public TeacherLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
