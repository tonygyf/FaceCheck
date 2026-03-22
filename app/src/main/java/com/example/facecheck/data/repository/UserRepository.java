package com.example.facecheck.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.database.DatabaseHelper;
import com.example.facecheck.utils.SessionManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class UserRepository {

    private final DatabaseHelper databaseHelper;
    private final OkHttpClient httpClient;
    private final SessionManager sessionManager;
    // FIXME: 硬编码的API地址与Key，后续应移到BuildConfig
    private static final String API_BASE_URL = "https://omni.gyf123.dpdns.org";

    public UserRepository(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
        this.httpClient = new OkHttpClient();
        this.sessionManager = new SessionManager(context);
    }
    
    public static class UserLoginResult {
        public final long userId;
        public final String role;
        public final String username;
        public final String name;
        public final String avatarUri;
        public final String accessToken;
        public final String refreshToken;

        public UserLoginResult(long userId, String role, String username, String name, String avatarUri, String accessToken, String refreshToken) {
            this.userId = userId;
            this.role = role;
            this.username = username;
            this.name = name;
            this.avatarUri = avatarUri;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public interface LoginCallback {
        void onSuccess(UserLoginResult result);
        void onError(String message);
    }

    public void loginAny(String username, String password, final LoginCallback callback) {
        new Thread(() -> {
            try {
                String url = API_BASE_URL + "/api/auth/login";
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("username", username);
                jsonBody.put("password", password);

                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                String apiKey = sessionManager.getApiKey();
                if (apiKey == null) {
                    callback.onError("API Key not available.");
                    return;
                }
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("X-API-Key", apiKey)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("网络请求失败: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            callback.onError("服务器错误: " + response.code());
                            return;
                        }

                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody == null) {
                                callback.onError("服务器响应为空");
                                return;
                            }
                            String responseData = responseBody.string();
                            JSONObject json = new JSONObject(responseData);

                            if (json.has("data")) {
                                JSONObject data = json.getJSONObject("data");
                                long userId = data.getLong("id");
                                String role = data.getString("role");
                                String name = data.getString("name");
                                String username = data.getString("username");
                                String avatarUri = data.optString("avatarUri", null);
                                String accessToken = data.getString("token");
                                String refreshToken = data.optString("refreshToken", "");
                                callback.onSuccess(new UserLoginResult(userId, role, username, name, avatarUri, accessToken, refreshToken));
                            } else {
                                callback.onError(json.optString("error", "登录失败，响应格式不正确"));
                            }

                        } catch (JSONException e) {
                            callback.onError("解析响应失败: " + e.getMessage());
                        }
                    }
                });
            } catch (JSONException e) {
                callback.onError("创建请求失败: " + e.getMessage());
            }
        }).start();
    }
    
    public interface RegisterCallback {
        void onSuccess();
        void onError(String message);
    }

    public void registerTeacher(String name, String username, String password, final RegisterCallback callback) {
        new Thread(() -> {
            try {
                String url = API_BASE_URL + "/api/auth/register";
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("name", name);
                jsonBody.put("username", username);
                jsonBody.put("password", password);

                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                String apiKey = sessionManager.getApiKey();
                if (apiKey == null) {
                    // For registration, if API Key is not yet available (e.g., first-time user),
                    // it might not be strictly necessary to send it. Depending on the backend API.
                    // For now, we will proceed without it for registration, or handle it as an error
                    // if the backend truly requires it for registration.
                    Log.w("UserRepository", "API Key not available during registration. Proceeding without.");
                    // Or: callback.onError("API Key not available."); return;
                }
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
                if (apiKey != null) {
                    requestBuilder.addHeader("X-API-Key", apiKey);
                }
                Request request = requestBuilder.build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("网络请求失败: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            try (ResponseBody responseBody = response.body()) {
                                String errorBody = responseBody != null ? responseBody.string() : "{}";
                                JSONObject json = new JSONObject(errorBody);
                                callback.onError(json.optString("error", "注册失败，服务器错误码: " + response.code()));
                            } catch (JSONException e) {
                                callback.onError("解析错误响应失败: " + e.getMessage());
                            }
                        }
                    }
                });
            } catch (JSONException e) {
                callback.onError("创建请求失败: " + e.getMessage());
            }
        }).start();
    }

    
    public Teacher getTeacherById(long teacherId) {
        return databaseHelper.getTeacherById(teacherId);
    }
    
    public boolean updateTeacher(Teacher teacher) {
        return databaseHelper.updateTeacher(teacher);
    }
}
