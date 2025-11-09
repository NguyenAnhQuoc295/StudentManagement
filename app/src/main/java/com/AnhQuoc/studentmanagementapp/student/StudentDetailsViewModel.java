package com.AnhQuoc.studentmanagementapp.student;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.data.StudentRepository;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentDetailsViewModel extends ViewModel {

    private StudentRepository studentRepository;
    private FirebaseFirestore db;

    private String currentStudentId;
    private ListenerRegistration certificateListener;

    private MutableLiveData<Student> studentLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Certificate>> certificatesLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage;
    private MutableLiveData<Boolean> closeActivityEvent = new MutableLiveData<>(false);
    private MutableLiveData<String> exportCertCsvData = new MutableLiveData<>();

    @Inject
    public StudentDetailsViewModel(StudentRepository studentRepository, FirebaseFirestore firestore) {
        this.studentRepository = studentRepository;
        this.db = firestore;
        this.toastMessage = (MutableLiveData<String>) studentRepository.getToastMessage();
    }

    // --- Getters ---
    public LiveData<Student> getStudentLiveData() { return studentLiveData; }
    public LiveData<List<Certificate>> getCertificatesLiveData() { return certificatesLiveData; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getCloseActivityEvent() { return closeActivityEvent; }
    public LiveData<String> getExportCertCsvData() { return exportCertCsvData; }

    public void setStudentId(String studentId) {
        if (studentId == null || studentId.equals(currentStudentId)) return;
        this.currentStudentId = studentId;
        loadStudentData();
        subscribeToCertificateUpdates();
    }
    public void refreshData() {
        if (currentStudentId != null) {
            loadStudentData();
            subscribeToCertificateUpdates();
        }
    }
    public void loadStudentData() {
        db.collection("students").document(currentStudentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        studentLiveData.setValue(task.getResult().toObject(Student.class));
                    } else {
                        toastMessage.setValue("Lỗi khi tải dữ liệu sinh viên");
                        closeActivityEvent.setValue(true);
                    }
                });
    }
    public void subscribeToCertificateUpdates() {
        if (certificateListener != null) certificateListener.remove();
        certificateListener = db.collection("students").document(currentStudentId)
                .collection("certificates")
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.w("StudentDetailsVM", "Error", error); return; }
                    if (value != null) {
                        List<Certificate> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Certificate c = doc.toObject(Certificate.class);
                            c.setCertificateId(doc.getId());
                            list.add(c);
                        }
                        certificatesLiveData.setValue(list);
                    }
                });
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (certificateListener != null) certificateListener.remove();
    }
    public void deleteStudent() {
        db.collection("students").document(currentStudentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Xóa sinh viên thành công");
                    closeActivityEvent.setValue(true);
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }
    public void saveCertificateToFirestore(String name, String date) {
        db.collection("students").document(currentStudentId)
                .collection("certificates")
                .add(new Certificate(name, date))
                .addOnSuccessListener(ref -> toastMessage.setValue("Thêm thành công"))
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi: " + e.getMessage()));
    }
    public void deleteCertificateFromFirestore(String certificateId) {
        db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificateId)
                .delete()
                .addOnSuccessListener(aVoid -> toastMessage.setValue("Xóa thành công"))
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }
    public void updateCertificateInFirestore(String certificateId, String newName, String newDate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("dateIssued", newDate);
        db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificateId)
                .update(updates)
                .addOnSuccessListener(aVoid -> toastMessage.setValue("Cập nhật thành công"))
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi cập nhật: " + e.getMessage()));
    }
    public void exportCertificatesToCsv() {
        List<Certificate> list = certificatesLiveData.getValue();
        if (list == null || list.isEmpty()) { toastMessage.setValue("Không có gì để xuất"); return; }
        StringBuilder csv = new StringBuilder("Name,DateIssued\n");
        for (Certificate cert : list) csv.append(cert.getName()).append(",").append(cert.getDateIssued()).append("\n");
        exportCertCsvData.setValue(csv.toString());
    }
    public void clearExportData() { exportCertCsvData.setValue(null); }
    public void importCertificatesFromCsv(Uri uri, ContentResolver cr) {
        if (uri == null) return;
        List<Certificate> certs = new ArrayList<>();
        try (InputStream is = cr.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line; boolean head = true;
            while ((line = reader.readLine()) != null) {
                if (head) { head = false; continue; }
                String[] cols = line.split(",");
                if (cols.length >= 2) certs.add(new Certificate(cols[0].trim(), cols[1].trim()));
            }
            if (certs.isEmpty()) { toastMessage.setValue("Không có dữ liệu"); return; }
            WriteBatch batch = db.batch();
            for (Certificate cert : certs) {
                batch.set(db.collection("students").document(currentStudentId).collection("certificates").document(), cert);
            }
            batch.commit()
                    .addOnSuccessListener(a -> toastMessage.setValue("Nhập " + certs.size() + " chứng chỉ thành công!"))
                    .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi nhập: " + e.getMessage()));
        } catch (Exception e) {
            toastMessage.setValue("Lỗi đọc tệp: " + e.getMessage());
        }
    }
}