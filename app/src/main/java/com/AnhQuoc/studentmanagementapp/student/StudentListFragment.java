package com.AnhQuoc.studentmanagementapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView; // <-- THÊM IMPORT NÀY
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentStudentListBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // <-- THÊM IMPORT NÀY
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding binding;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo
        db = FirebaseFirestore.getInstance();
        studentList = new ArrayList<>();

        // 2. Setup Adapter (với Listener)
        studentAdapter = new StudentAdapter(studentList, new StudentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Student student) {
                Intent intent = new Intent(getActivity(), StudentDetailsActivity.class);
                intent.putExtra("STUDENT_ID", student.getStudentId());
                startActivity(intent);
            }
        });

        // 3. Cài đặt RecyclerView
        binding.rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStudents.setAdapter(studentAdapter);

        // 4. Xử lý sự kiện nhấn nút "+"
        binding.fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditStudentActivity.class);
            startActivity(intent);
        });

        // 5. Tải dữ liệu ban đầu
        loadStudentsFromFirestore(""); // Tải tất cả khi query rỗng

        // === THÊM MỚI: XỬ LÝ THANH TÌM KIẾM ===
        binding.searchViewStudents.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Khi người dùng nhấn "Enter"
                loadStudentsFromFirestore(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Khi người dùng gõ từng chữ
                loadStudentsFromFirestore(newText);
                return true;
            }
        });
        // ======================================
    }

    // === CẬP NHẬT: HÀM TẢI DỮ LIỆU ĐỂ HỖ TRỢ TÌM KIẾM ===
    private void loadStudentsFromFirestore(String searchQuery) {

        // Tạo một truy vấn (query) cơ bản
        Query query = db.collection("students");

        // Nếu có nội dung tìm kiếm...
        if (searchQuery != null && !searchQuery.isEmpty()) {
            // ...thêm điều kiện lọc
            // (Tìm tất cả tên bắt đầu bằng searchQuery)
            query = query.orderBy("name")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
            // \uf8ff là một ký tự Unicode rất cao, giúp Firestore
            // tìm tất cả các chuỗi có tiền tố là searchQuery
        } else {
            // Nếu không tìm gì, chỉ sắp xếp theo tên
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        // Lấy dữ liệu
        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            studentList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Student student = document.toObject(Student.class);
                                student.setStudentId(document.getId());
                                studentList.add(student);
                            }
                            studentAdapter.notifyDataSetChanged();
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
    // ===============================================

    // Tải lại dữ liệu khi quay lại màn hình này
    @Override
    public void onResume() {
        super.onResume();
        // Lấy query hiện tại trên thanh search để tải lại
        String currentQuery = binding.searchViewStudents.getQuery().toString();
        loadStudentsFromFirestore(currentQuery);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}