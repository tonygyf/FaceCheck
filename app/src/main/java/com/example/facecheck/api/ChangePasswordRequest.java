package com.example.facecheck.api;

public class ChangePasswordRequest {
    private final long teacherId;
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordRequest(long teacherId, String oldPassword, String newPassword) {
        this.teacherId = teacherId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
