package com.example.facecheck.api;

import com.google.gson.annotations.SerializedName;

public class TeacherLoginResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private TeacherData data;

    public boolean isSuccess() {
        return success;
    }

    public TeacherData getData() {
        return data;
    }

    public static class TeacherData {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        @SerializedName("username")
        private String username;

        @SerializedName("role")
        private String role; // Should be "teacher"

        @SerializedName("avatarUri")
        private String avatarUri;

        @SerializedName("token")
        private String accessToken;

        @SerializedName("refreshToken")
        private String refreshToken;

        // Getters
        public long getId() { return id; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getAvatarUri() { return avatarUri; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
