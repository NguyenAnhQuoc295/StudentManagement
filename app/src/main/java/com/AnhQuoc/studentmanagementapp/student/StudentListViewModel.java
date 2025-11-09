package com.AnhQuoc.studentmanagementapp.student;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StudentListViewModel extends ViewModel {

    private static final String TAG = "StudentListViewModel";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MutableLiveData<List<Student>> studentListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // LiveData mới cho việc Export
    private MutableLiveData<String> exportCsvData = new MutableLiveData<>();

    // Các biến trạng thái sắp xếp
    private String currentSortField = "name";
    private Query.Direction currentSortDirection = Query.Direction.ASCENDING;

    public LiveData<List<Student>> getStudentListLiveData() {
        return studentListLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    // Getter mới
    public LiveData<String> getExportCsvData() {
        return exportCsvData;
    }

    // === HÀM MỚI ĐỂ FRAGMENT GỌI ===
    public void clearExportData() {
        exportCsvData.setValue(null);
    }


    // Hàm để Fragment gọi khi nhấn nút sắp xếp
    public void setSortOrder(String field, Query.Direction direction) {
        currentSortField = field;
        currentSortDirection = direction;
    }

    public String getCurrentSortField() {
        return currentSortField;
    }

    public Query.Direction getCurrentSortDirection() {
        return currentSortDirection;
    }

    // Logic được di chuyển từ Fragment
    public void loadStudentsFromFirestore(String searchQuery) {
        Query query;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = db.collection("students")
                    .orderBy("name")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
        } else {
            query = db.collection("students").orderBy(currentSortField, currentSortDirection);
        }

        query.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Student> studentList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            student.setStudentId(document.getId());
                            studentList.add(student);
                        }
                        studentListLiveData.setValue(studentList);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        toastMessage.setValue("Lỗi khi tải: " + task.getException().getMessage());
                    }
                });
    }

    // === CHỨC NĂNG MỚI: EXPORT CSV ===
    public void exportStudentsToCsv() {
        List<Student> currentList = studentListLiveData.getValue();
        if (currentList == null || currentList.isEmpty()) {
            toastMessage.setValue("Không có dữ liệu để xuất");
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        // Header
        csvContent.append("Name,Age,Phone\n");

        // Data
        for (Student student : currentList) {
            csvContent.append(student.getName()).append(",");
            csvContent.append(student.getAge()).append(",");
            csvContent.append(student.getPhone()).append("\n");
        }

        exportCsvData.setValue(csvContent.toString());
    }

    // === CHỨC NĂNG MỚI: IMPORT CSV ===
    public void importStudentsFromCsv(Uri uri, ContentResolver contentResolver) {
        if (uri == null) return;

        List<Student> studentsToImport = new ArrayList<>();
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true; // Bỏ qua dòng tiêu đề
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

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

            // Dùng WriteBatch để thêm nhiều sinh viên cùng lúc
            WriteBatch batch = db.batch();
            for (Student student : studentsToImport) {
                batch.set(db.collection("students").document(), student);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        toastMessage.setValue("Nhập " + studentsToImport.size() + " sinh viên thành công!");
                        loadStudentsFromFirestore(""); // Tải lại danh sách
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