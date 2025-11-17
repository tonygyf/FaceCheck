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
        
        UserRepository.UserLoginResult res = userRepository.loginAny(username, password);
        if (res != null) {
            _uiState.setValue(new LoginUiState.Success(res.userId, res.role, res.displayName));
        } else {
            _uiState.setValue(new LoginUiState.Error("用户名或密码错误"));
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Repository会在不需要时自动清理资源
    }
}
