package com.example.facecheck.ui.classroom;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ClassroomViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    public ClassroomViewModelFactory(@NonNull Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ClassroomViewModel.class)) {
            return (T) new ClassroomViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
