package com.AnhQuoc.studentmanagementapp.student;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class StudentListViewModel extends ViewModel {

    private static final String TAG = "StudentListViewModel";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MutableLiveData<List<Student>> studentListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // Các biến trạng thái sắp xếp
    private String currentSortField = "name";
    private Query.Direction currentSortDirection = Query.Direction.ASCENDING;

    public LiveData<List<Student>> getStudentListLiveData() {
        return studentListLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
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
}