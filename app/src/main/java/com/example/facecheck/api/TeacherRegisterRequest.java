package com.example.facecheck.api;

public class TeacherRegisterRequest {
    private String name;
    private String username;
    private String password;

    public TeacherRegisterRequest(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }
}
