package com.AnhQuoc.studentmanagementapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton // Báo cho Hilt biết đây là một đối tượng duy nhất
public class StudentRepository {

    private static final String TAG = "StudentRepository";
    private FirebaseFirestore db; // Sẽ được tiêm (inject)

    private ListenerRegistration studentListenerRegistration;
    private MutableLiveData<List<Student>> studentListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // CONSTRUCTOR MỚI (DÙNG @Inject)
    @Inject
    public StudentRepository(FirebaseFirestore firestore) {
        this.db = firestore; // Nhận Firestore từ Hilt (AppModule)
    }

    public LiveData<List<Student>> getStudentListLiveData() {
        return studentListLiveData;
    }
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }
    public void subscribeToStudentUpdates(String searchQuery, String sortField, Query.Direction direction) {
        clearStudentListener();
        Query query;
        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = db.collection("students")
                    .orderBy("name")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
        } else {
            query = db.collection("students").orderBy(sortField, direction);
        }
        studentListenerRegistration = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "Lỗi khi lắng nghe Student updates.", error);
                toastMessage.setValue("Lỗi khi tải: " + error.getMessage());
                return;
            }
            if (queryDocumentSnapshots != null) {
                List<Student> studentList = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Student student = document.toObject(Student.class);
                    student.setStudentId(document.getId());
                    studentList.add(student);
                }
                studentListLiveData.setValue(studentList);
            }
        });
    }
    public void clearStudentListener() {
        if (studentListenerRegistration != null) {
            studentListenerRegistration.remove();
            studentListenerRegistration = null;
        }
    }
    public String exportStudentsToCsv(List<Student> currentList) {
        if (currentList == null || currentList.isEmpty()) {
            toastMessage.setValue("Không có dữ liệu để xuất");
            return null;
        }
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Name,Age,Phone\n");
        for (Student student : currentList) {
            csvContent.append(student.getName()).append(",");
            csvContent.append(student.getAge()).append(",");
            csvContent.append(student.getPhone()).append("\n");
        }
        return csvContent.toString();
    }
    public void importStudentsFromCsv(Uri uri, ContentResolver contentResolver) {
        if (uri == null) return;
        List<Student> studentsToImport = new ArrayList<>();
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] columns = line.split(",");
                if (columns.length >= 3) {
                    String name = columns[0].trim();
                    int age = Integer.parseInt(columns[1].trim());
                    String phone = columns[2].trim();
                    studentsToImport.add(new Student(name, age, phone));
                }
            }
            if (studentsToImport.isEmpty()) {
                toastMessage.setValue("Không tìm thấy dữ liệu hợp lệ trong tệp");
                return;
            }
            WriteBatch batch = db.batch();
            for (Student student : studentsToImport) {
                batch.set(db.collection("students").document(), student);
            }
            batch.commit()
                    .addOnSuccessListener(aVoid -> toastMessage.setValue("Nhập " + studentsToImport.size() + " sinh viên thành công!"))
                    .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi nhập: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Error importing CSV", e);
            toastMessage.setValue("Lỗi đọc tệp: " + e.getMessage());
        }
    }
}