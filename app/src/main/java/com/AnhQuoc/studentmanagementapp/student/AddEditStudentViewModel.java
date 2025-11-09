// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.student;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.AnhQuoc.studentmanagementapp.data.StudentRepository; // <-- THÊM
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject; // <-- THÊM
import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM

@HiltViewModel
public class AddEditStudentViewModel extends ViewModel {

    private FirebaseFirestore db; // Tạm thời giữ lại db cho logic (hoặc chuyển sang Repo)
    private LiveData<String> toastMessage; // Lấy từ Repo

    private MutableLiveData<Student> studentLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> saveSuccessEvent = new MutableLiveData<>(false);

    @Inject
    public AddEditStudentViewModel(StudentRepository studentRepository, FirebaseFirestore firestore) {
        this.db = firestore;
        // Dùng chung Toast từ StudentRepository
        this.toastMessage = studentRepository.getToastMessage();
    }

    public LiveData<Student> getStudentLiveData() { return studentLiveData; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getSaveSuccessEvent() { return saveSuccessEvent; }

    // (Tất cả logic load/save/update giữ nguyên)
    public void loadStudentDataForEdit(String studentId) {
        if (studentId == null) return;
        db.collection("students").document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        studentLiveData.setValue(task.getResult().toObject(Student.class));
                    } else {
                        ((MutableLiveData<String>)toastMessage).setValue("Không tìm thấy sinh viên");
                        saveSuccessEvent.setValue(true);
                    }
                });
    }
    public void saveStudentToFirestore(String name, String ageStr, String phone) {
        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            ((MutableLiveData<String>)toastMessage).setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        Student student = new Student(name, Integer.parseInt(ageStr), phone);
        db.collection("students")
                .add(student)
                .addOnSuccessListener(docRef -> {
                    ((MutableLiveData<String>)toastMessage).setValue("Thêm sinh viên thành công!");
                    saveSuccessEvent.setValue(true);
                })
                .addOnFailureListener(e -> ((MutableLiveData<String>)toastMessage).setValue("Lỗi: " + e.getMessage()));
    }
    public void updateStudentInFirestore(String studentId, String name, String ageStr, String phone) {
        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            ((MutableLiveData<String>)toastMessage).setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        Student updatedStudent = new Student(name, Integer.parseInt(ageStr), phone);
        db.collection("students").document(studentId)
                .set(updatedStudent)
                .addOnSuccessListener(aVoid -> {
                    ((MutableLiveData<String>)toastMessage).setValue("Cập nhật thành công!");
                    saveSuccessEvent.setValue(true);
                })
                .addOnFailureListener(e -> ((MutableLiveData<String>)toastMessage).setValue("Lỗi khi cập nhật: " + e.getMessage()));
    }
}