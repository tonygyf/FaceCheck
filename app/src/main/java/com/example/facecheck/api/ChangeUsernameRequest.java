package com.example.facecheck.api;

public class ChangeUsernameRequest {
    private final long teacherId;
    private final String name;

    public ChangeUsernameRequest(long teacherId, String name) {
        this.teacherId = teacherId;
        this.name = name;
    }
}
