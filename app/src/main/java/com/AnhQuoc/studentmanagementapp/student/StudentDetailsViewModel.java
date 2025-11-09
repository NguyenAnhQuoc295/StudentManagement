package com.AnhQuoc.studentmanagementapp.student;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    // LiveData mới cho Export
    private MutableLiveData<String> exportCertCsvData = new MutableLiveData<>();

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
    // Getter mới
    public LiveData<String> getExportCertCsvData() {
        return exportCertCsvData;
    }

    // ... (Giữ nguyên hàm setStudentId, refreshData, loadStudentData) ...

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
        // ... (Giữ nguyên) ...
        db.collection("students").document(currentStudentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Xóa sinh viên thành công");
                    closeActivityEvent.setValue(true);
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }

    public void saveCertificateToFirestore(String name, String date) {
        // ... (Giữ nguyên) ...
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
        // ... (Giữ nguyên) ...
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
        // ... (Giữ nguyên) ...
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

    // === CHỨC NĂNG MỚI: EXPORT CERTIFICATES ===
    public void exportCertificatesToCsv() {
        List<Certificate> currentList = certificatesLiveData.getValue();
        if (currentList == null || currentList.isEmpty()) {
            toastMessage.setValue("Không có chứng chỉ để xuất");
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Name,DateIssued\n"); // Header
        for (Certificate cert : currentList) {
            csvContent.append(cert.getName()).append(",");
            csvContent.append(cert.getDateIssued()).append("\n");
        }
        exportCertCsvData.setValue(csvContent.toString());
    }

    // === CHỨC NĂNG MỚI: IMPORT CERTIFICATES ===
    public void importCertificatesFromCsv(Uri uri, ContentResolver contentResolver) {
        if (uri == null) return;

        List<Certificate> certsToImport = new ArrayList<>();
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] columns = line.split(",");
                if (columns.length >= 2) {
                    String name = columns[0].trim();
                    String date = columns[1].trim();
                    certsToImport.add(new Certificate(name, date));
                }
            }

            if (certsToImport.isEmpty()) {
                toastMessage.setValue("Không tìm thấy dữ liệu hợp lệ trong tệp");
                return;
            }

            WriteBatch batch = db.batch();
            for (Certificate cert : certsToImport) {
                DocumentReference docRef = db.collection("students").document(currentStudentId)
                        .collection("certificates").document();
                batch.set(docRef, cert);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        toastMessage.setValue("Nhập " + certsToImport.size() + " chứng chỉ thành công!");
                        loadCertificates(); // Tải lại
                    })
                    .addOnFailureListener(e -> {
                        toastMessage.setValue("Lỗi khi nhập: " + e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error importing CSV", e);
            toastMessage.setValue("Lỗi đọc tệp: " + e.getMessage());
        }
    }
}