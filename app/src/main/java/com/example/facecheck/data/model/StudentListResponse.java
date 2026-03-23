// StudentListResponse.java - 对应服务端 { data: [...] }
package com.example.facecheck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StudentListResponse {
    @SerializedName("data") public List<StudentApiModel> data;

    public static class StudentApiModel {
        @SerializedName("id")        public long id;
        @SerializedName("classId")   public long classId;
        @SerializedName("name")      public String name;
        @SerializedName("sid")       public String sid;
        @SerializedName("gender")    public String gender;
        @SerializedName("avatarUri") public String avatarUri;
        @SerializedName("email")     public String email;
        @SerializedName("createdAt") public String createdAt;
    }
}