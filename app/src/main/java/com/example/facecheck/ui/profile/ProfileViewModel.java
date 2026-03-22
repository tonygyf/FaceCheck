package com.example.facecheck.ui.profile;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.facecheck.api.ApiResponse;
import com.example.facecheck.data.repository.ApiCallback;
import com.example.facecheck.data.repository.ProfileRepository;
import com.example.facecheck.data.repository.ProfileRepositoryImpl;
import com.example.facecheck.utils.SessionManager;

public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository profileRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<Boolean> _passwordChangeSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> passwordChangeSuccess = _passwordChangeSuccess;

    private final MutableLiveData<Boolean> _avatarUploadSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> avatarUploadSuccess = _avatarUploadSuccess;

    private final MutableLiveData<Boolean> _usernameChangeSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> usernameChangeSuccess = _usernameChangeSuccess;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    public ProfileViewModel(Application application) {
        super(application);
        this.profileRepository = new ProfileRepositoryImpl(application);
        this.sessionManager = new SessionManager(application);
    }

    public void changePassword(String oldPassword, String newPassword) {
        long teacherId = sessionManager.getTeacherId();
        if (teacherId == -1) {
            _errorMessage.postValue("User not logged in.");
            return;
        }

        profileRepository.changePassword(teacherId, oldPassword, newPassword, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _passwordChangeSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _passwordChangeSuccess.postValue(false);
            }
        });
    }

    public void uploadAvatar(String filePath) {
        long teacherId = sessionManager.getTeacherId();
        if (teacherId == -1) {
            _errorMessage.postValue("User not logged in.");
            return;
        }

        profileRepository.uploadAvatar(teacherId, filePath, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _avatarUploadSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _avatarUploadSuccess.postValue(false);
            }
        });
    }

    public void changeUsername(String newName) {
        long teacherId = sessionManager.getTeacherId();
        if (teacherId == -1) {
            _errorMessage.postValue("User not logged in.");
            return;
        }
        profileRepository.changeUsername(teacherId, newName, new ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse data) {
                _usernameChangeSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _usernameChangeSuccess.postValue(false);
            }
        });
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public void clearPasswordChangeStatus() {
        _passwordChangeSuccess.setValue(null);
    }
}
