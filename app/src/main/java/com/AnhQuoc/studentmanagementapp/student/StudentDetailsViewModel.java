package com.AnhQuoc.studentmanagementapp.student;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDetailsViewModel extends ViewModel {

    private static final String TAG = "StudentDetailsViewModel";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentStudentId;

    // LiveData cho thông tin sinh viên
    private MutableLiveData<Student> studentLiveData = new MutableLiveData<>();
    // LiveData cho danh sách chứng chỉ
    private MutableLiveData<List<Certificate>> certificatesLiveData = new MutableLiveData<>();
    // LiveData cho thông báo
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();
    // LiveData cho sự kiện đóng Activity (ví dụ: sau khi xóa)
    private MutableLiveData<Boolean> closeActivityEvent = new MutableLiveData<>(false);

    // Getters
    public LiveData<Student> getStudentLiveData() {
        return studentLiveData;
    }
    public LiveData<List<Certificate>> getCertificatesLiveData() {
        return certificatesLiveData;
    }
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
    public LiveData<Boolean> getCloseActivityEvent() {
        return closeActivityEvent;
    }

    // Hàm này được Activity gọi đầu tiên
    public void setStudentId(String studentId) {
        if (studentId == null || studentId.equals(currentStudentId)) {
            return;
        }
        this.currentStudentId = studentId;
        loadStudentData();
        loadCertificates();
    }

    public void refreshData() {
        if (currentStudentId != null) {
            loadStudentData();
            loadCertificates();
        }
    }

    // --- LOGIC TỪ ACTIVITY ĐƯỢC DI CHUYỂN VÀO ĐÂY ---

    public void loadStudentData() {
        db.collection("students").document(currentStudentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Student student = task.getResult().toObject(Student.class);
                        studentLiveData.setValue(student);
                    } else {
                        toastMessage.setValue("Lỗi khi tải dữ liệu sinh viên");
                        closeActivityEvent.setValue(true); // Đóng nếu không tải được
                    }
                });
    }

    public void loadCertificates() {
        db.collection("students").document(currentStudentId)
                .collection("certificates")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Certificate> list = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Certificate certificate = document.toObject(Certificate.class);
                            certificate.setCertificateId(document.getId());
                            list.add(certificate);
                        }
                        certificatesLiveData.setValue(list);
                    } else {
                        Log.w(TAG, "Error getting certificates.", task.getException());
                    }
                });
    }

    public void deleteStudent() {
        // (Nâng cao: Nên xóa cả sub-collection, nhưng hiện tại ta chỉ xóa student)
        db.collection("students").document(currentStudentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Xóa sinh viên thành công");
                    closeActivityEvent.setValue(true);
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }

    public void saveCertificateToFirestore(String name, String date) {
        Certificate certificate = new Certificate(name, date);
        db.collection("students").document(currentStudentId)
                .collection("certificates")
                .add(certificate)
                .addOnSuccessListener(documentReference -> {
                    toastMessage.setValue("Thêm chứng chỉ thành công");
                    loadCertificates(); // Tải lại danh sách
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi: " + e.getMessage()));
    }

    public void deleteCertificateFromFirestore(String certificateId) {
        db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificateId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Xóa chứng chỉ thành công");
                    loadCertificates(); // Tải lại
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }

    public void updateCertificateInFirestore(String certificateId, String newName, String newDate) {
        DocumentReference certRef = db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificateId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("dateIssued", newDate);

        certRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Cập nhật chứng chỉ thành công");
                    loadCertificates(); // Tải lại
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi cập nhật: " + e.getMessage()));
    }
}