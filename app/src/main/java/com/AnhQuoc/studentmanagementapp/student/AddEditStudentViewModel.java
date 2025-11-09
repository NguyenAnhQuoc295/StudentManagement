package com.AnhQuoc.studentmanagementapp.student;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEditStudentViewModel extends ViewModel {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Dùng để tải dữ liệu (chế độ "Sửa")
    private MutableLiveData<Student> studentLiveData = new MutableLiveData<>();
    // Dùng để báo sự kiện (lưu thành công, lỗi)
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> saveSuccessEvent = new MutableLiveData<>(false);

    public LiveData<Student> getStudentLiveData() {
        return studentLiveData;
    }
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
    public LiveData<Boolean> getSaveSuccessEvent() {
        return saveSuccessEvent;
    }

    public void loadStudentDataForEdit(String studentId) {
        if (studentId == null) return;
        db.collection("students").document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Student student = task.getResult().toObject(Student.class);
                        studentLiveData.setValue(student);
                    } else {
                        toastMessage.setValue("Không tìm thấy sinh viên");
                        saveSuccessEvent.setValue(true); // Gây sự kiện đóng
                    }
                });
    }

    public void saveStudentToFirestore(String name, String ageStr, String phone) {
        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            toastMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        int age = Integer.parseInt(ageStr);
        Student student = new Student(name, age, phone);

        db.collection("students")
                .add(student)
                .addOnSuccessListener(documentReference -> {
                    toastMessage.setValue("Thêm sinh viên thành công!");
                    saveSuccessEvent.setValue(true);
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi: " + e.getMessage()));
    }

    public void updateStudentInFirestore(String studentId, String name, String ageStr, String phone) {
        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            toastMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        int age = Integer.parseInt(ageStr);
        Student updatedStudent = new Student(name, age, phone);

        db.collection("students").document(studentId)
                .set(updatedStudent)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Cập nhật thành công!");
                    saveSuccessEvent.setValue(true);
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi cập nhật: " + e.getMessage()));
    }
}