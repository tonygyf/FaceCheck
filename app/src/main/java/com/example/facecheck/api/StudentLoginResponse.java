package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

// This class is designed to capture the successful login response for a student.
public class StudentLoginResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private StudentData data;

    public boolean isSuccess() {
        return success;
    }

    public StudentData getData() {
        return data;
    }

    public static class StudentData {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        @SerializedName("sid")
        private String sid;

        @SerializedName("classId")
        private long classId;

        @SerializedName("role")
        private String role; // Should be "student"

        @SerializedName("accessToken")
        private String accessToken;

        @SerializedName("refreshToken")
        private String refreshToken;
        @SerializedName("avatarUri")
        private String avatarUri;

        public String getAvatarUri() {
            return avatarUri;
        }
        // Getters for all fields
        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSid() {
            return sid;
        }

        public long getClassId() {
            return classId;
        }

        public String getRole() {
            return role;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
