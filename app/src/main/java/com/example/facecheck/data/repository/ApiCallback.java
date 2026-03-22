package com.example.facecheck.data.repository;

public interface ApiCallback<T> {
    void onSuccess(T data);
    void onError(String message);
}
