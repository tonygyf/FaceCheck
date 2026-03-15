package com.example.facecheck.ui.auth;

import android.app.Application;
import android.text.TextUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.facecheck.data.model.Teacher;
import com.example.facecheck.data.repository.UserRepository;

public class LoginViewModel extends AndroidViewModel {
    
    private final UserRepository userRepository;
    private final MutableLiveData<LoginUiState> _uiState = new MutableLiveData<>();
    public final LiveData<LoginUiState> uiState = _uiState;
    
    public LoginViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);
        _uiState.setValue(new LoginUiState.Initial());
    }
    
    public void login(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            _uiState.setValue(new LoginUiState.Error("用户名和密码不能为空"));
            return;
        }
        
        _uiState.setValue(new LoginUiState.Loading());
        
        userRepository.loginAny(username, password, new UserRepository.LoginCallback() {
            @Override
            public void onSuccess(UserRepository.UserLoginResult result) {
                _uiState.postValue(new LoginUiState.Success(result.userId, result.role, result.username, result.name, result.avatarUri, result.accessToken, result.refreshToken));
            }

            @Override
            public void onError(String message) {
                _uiState.postValue(new LoginUiState.Error(message));
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Repository会在不需要时自动清理资源
    }
}
