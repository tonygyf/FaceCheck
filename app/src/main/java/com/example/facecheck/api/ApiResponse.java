package com.example.facecheck.api;

import java.util.List;

// A generic wrapper for API responses
public class ApiResponse<T> {
    public boolean success;
    public T data;
    public String error;
}
