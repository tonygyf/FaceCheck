package com.example.facecheck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ClassroomListResponse {
    @SerializedName("data")
    private List<Classroom> data;

    public List<Classroom> getData() {
        return data;
    }

    public void setData(List<Classroom> data) {
        this.data = data;
    }
}
