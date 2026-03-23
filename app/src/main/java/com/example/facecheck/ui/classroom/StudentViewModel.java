
package com.example.facecheck.ui.classroom;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.facecheck.data.model.Student;
import com.example.facecheck.data.repository.StudentRepository;
import java.util.List;

public class StudentViewModel extends AndroidViewModel {

    private final StudentRepository studentRepository;
    private final MutableLiveData<List<Student>> _students = new MutableLiveData<>();
    public final LiveData<List<Student>> students = _students;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _actionResult = new MutableLiveData<>();
    public final LiveData<Boolean> actionResult = _actionResult;

    public void createStudent(long classId, String name, String sid, String gender, String email) {
        studentRepository.createStudent(classId, name, sid, gender, email, new StudentRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                _actionResult.postValue(true);
                loadStudentsForClass(classId); // Refresh the list
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _actionResult.postValue(false);
            }
        });
    }

    public void updateStudent(long studentId, long classId, String name, String sid, String gender, String email) {
        studentRepository.updateStudent(studentId, classId, name, sid, gender, email, new StudentRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                _actionResult.postValue(true);
                loadStudentsForClass(classId); // Refresh the list
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _actionResult.postValue(false);
            }
        });
    }

    public void deleteStudent(long studentId, long classId) {
        studentRepository.deleteStudent(studentId, new StudentRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                _actionResult.postValue(true);
                loadStudentsForClass(classId); // Refresh the list
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
                _actionResult.postValue(false);
            }
        });
    }

    public StudentViewModel(Application application) {
        super(application);
        this.studentRepository = new StudentRepository(application);
    }

    public void loadStudentsForClass(long classId) {
        studentRepository.getStudentsByClass(classId, new StudentRepository.StudentsCallback() {
            @Override
            public void onSuccess(List<Student> studentList) {
                _students.postValue(studentList);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue(message);
            }
        });
    }
}
