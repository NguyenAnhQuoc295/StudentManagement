package com.AnhQuoc.studentmanagementapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // <-- DÒNG IMPORT ĐÃ ĐƯỢC THÊM VÀO ĐÂY
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentStudentListBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding binding;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private FirebaseFirestore db;
    private String currentUserRole;

    private String currentSortField = "name";
    private Query.Direction currentSortDirection = Query.Direction.ASCENDING;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentListBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            currentUserRole = getArguments().getString("USER_ROLE");
        }
        if (currentUserRole == null) {
            currentUserRole = "Employee";
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        studentList = new ArrayList<>();

        studentAdapter = new StudentAdapter(studentList, new StudentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Student student) {
                Intent intent = new Intent(getActivity(), StudentDetailsActivity.class);
                intent.putExtra("STUDENT_ID", student.getStudentId());
                intent.putExtra("USER_ROLE", currentUserRole);
                startActivity(intent);
            }
        });

        binding.rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStudents.setAdapter(studentAdapter);

        if ("Employee".equals(currentUserRole)) {
            binding.fabAddStudent.setVisibility(View.GONE);
            binding.btnSortStudents.setVisibility(View.GONE);
        } else {
            binding.fabAddStudent.setVisibility(View.VISIBLE);
            binding.btnSortStudents.setVisibility(View.VISIBLE);
        }

        binding.fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditStudentActivity.class);
            startActivity(intent);
        });

        binding.btnSortStudents.setOnClickListener(v -> {
            showSortDialog();
        });

        loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());

        binding.searchViewStudents.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadStudentsFromFirestore(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                loadStudentsFromFirestore(newText);
                return true;
            }
        });
    }

    private void showSortDialog() {
        final String[] sortOptions = {
                "Tên (A-Z)",
                "Tên (Z-A)",
                "Tuổi (Tăng dần)",
                "Tuổi (Giảm dần)"
        };

        int checkedItem = 0;
        if (currentSortField.equals("name") && currentSortDirection == Query.Direction.ASCENDING) {
            checkedItem = 0;
        } else if (currentSortField.equals("name") && currentSortDirection == Query.Direction.DESCENDING) {
            checkedItem = 1;
        } else if (currentSortField.equals("age") && currentSortDirection == Query.Direction.ASCENDING) {
            checkedItem = 2;
        } else if (currentSortField.equals("age") && currentSortDirection == Query.Direction.DESCENDING) {
            checkedItem = 3;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Sắp xếp danh sách")
                .setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortField = "name";
                            currentSortDirection = Query.Direction.ASCENDING;
                            break;
                        case 1:
                            currentSortField = "name";
                            currentSortDirection = Query.Direction.DESCENDING;
                            break;
                        case 2:
                            currentSortField = "age";
                            currentSortDirection = Query.Direction.ASCENDING;
                            break;
                        case 3:
                            currentSortField = "age";
                            currentSortDirection = Query.Direction.DESCENDING;
                            break;
                    }
                    loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadStudentsFromFirestore(String searchQuery) {

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
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            student.setStudentId(document.getId());
                            studentList.add(student);
                        }
                        studentAdapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                        // Đây là dòng đã gây lỗi (giờ đã fix)
                        Toast.makeText(getContext(), "Lỗi khi tải: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}