package com.example.facecheck.ui.auth;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.facecheck.data.repository.UserRepository;

public class RegisterViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<RegisterUiState> _uiState = new MutableLiveData<>();
    public final LiveData<RegisterUiState> uiState = _uiState;

    public RegisterViewModel(Application application) {
        super(application);
        userRepository = new UserRepository(application);
        _uiState.setValue(new RegisterUiState.Initial());
    }

    public void registerTeacher(String name, String username, String password) {
        _uiState.setValue(new RegisterUiState.Loading());
        
        userRepository.registerTeacher(name, username, password, new UserRepository.RegisterCallback() {
            @Override
            public void onSuccess() {
                _uiState.postValue(new RegisterUiState.Success("注册成功！"));
            }

            @Override
            public void onError(String message) {
                _uiState.postValue(new RegisterUiState.Error(message));
            }
        });
    }
}
