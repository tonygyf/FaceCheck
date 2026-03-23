
package com.example.facecheck.ui.classroom;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class StudentViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public StudentViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StudentViewModel.class)) {
            return (T) new StudentViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
