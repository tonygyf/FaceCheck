package com.example.facecheck.ui.classroom;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.facecheck.data.model.Classroom;
import com.example.facecheck.data.repository.ApiCallback;
import com.example.facecheck.data.repository.ClassroomRepository;
import com.example.facecheck.data.repository.ClassroomRepositoryImpl;
import com.example.facecheck.utils.SessionManager;

import java.util.List;

public class ClassroomViewModel extends AndroidViewModel {

    private final ClassroomRepository classroomRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<List<Classroom>> _classrooms = new MutableLiveData<>();
    public final LiveData<List<Classroom>> classrooms = _classrooms;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _classroomCreated = new MutableLiveData<>();
    public final LiveData<Boolean> classroomCreated = _classroomCreated;

    public ClassroomViewModel(Application application) {
        super(application);
        this.classroomRepository = new ClassroomRepositoryImpl(application);
        this.sessionManager = new SessionManager(application);
    }

    public void loadClassrooms() {
        long teacherId = sessionManager.getTeacherId();
        if (teacherId != -1) {
            classroomRepository.getClassrooms(teacherId).observeForever(data -> {
                _classrooms.postValue(data);
            });
        } else {
            _errorMessage.postValue("教师ID未找到，无法加载班级。");
        }
    }

    public void createClassroom(String name, int year) {
        long teacherId = sessionManager.getTeacherId();
        if (teacherId != -1) {
            classroomRepository.createClassroom(teacherId, name, year, new ApiCallback<Classroom>() {
                @Override
                public void onSuccess(Classroom data) {
                    _classroomCreated.postValue(true);
                    loadClassrooms(); // Refresh list after creation
                }

                @Override
                public void onError(String message) {
                    _errorMessage.postValue("创建班级失败: " + message);
                    _classroomCreated.postValue(false);
                }
            });
        } else {
            _errorMessage.postValue("教师ID未找到，无法创建班级。");
            _classroomCreated.postValue(false);
        }
    }

    public LiveData<List<Classroom>> getClassrooms(long teacherId) {
        // This method is called by ClassroomFragment to observe changes.
        // The actual loading should be triggered by loadClassrooms()
        // For now, let's keep it simple and just return the LiveData.
        // The loading logic is in loadClassrooms().
        // In a real app, you might want to trigger loadClassrooms() here if it's not already loaded.
        return _classrooms;
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public void clearClassroomCreatedEvent() {
        _classroomCreated.setValue(false);
    }
}
