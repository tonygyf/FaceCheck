package com.example.facecheck.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    public interface Task<T> { T run() throws Exception; }
    public interface Callback<T> { void onResult(T result); }
    public interface ErrorCallback { void onError(Throwable t); }

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public <T> void run(Task<T> task, Callback<T> callback, ErrorCallback error) {
        executor.submit(() -> {
            try {
                T result = task.run();
                mainHandler.post(() -> { if (callback != null) callback.onResult(result); });
            } catch (Throwable t) {
                mainHandler.post(() -> { if (error != null) error.onError(t); });
            }
        });
    }

    public void runVoid(Runnable task, Runnable callback, ErrorCallback error) {
        executor.submit(() -> {
            try {
                task.run();
                mainHandler.post(() -> { if (callback != null) callback.run(); });
            } catch (Throwable t) {
                mainHandler.post(() -> { if (error != null) error.onError(t); });
            }
        });
    }
}

