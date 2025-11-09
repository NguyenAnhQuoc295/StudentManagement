package com.AnhQuoc.studentmanagementapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- THÊM IMPORT
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentStudentListBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
// KHÔNG CẦN import FirebaseFirestore
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding binding;
    private StudentAdapter studentAdapter;
    private List<Student> studentList; // Vẫn cần list cho Adapter
    // private FirebaseFirestore db; // Không cần
    private String currentUserRole;

    private StudentListViewModel viewModel; // <-- Biến ViewModel

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

        // 1. Khởi tạo
        studentList = new ArrayList<>();
        viewModel = new ViewModelProvider(this).get(StudentListViewModel.class);

        // 2. Setup Adapter
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

        // 3. Phân quyền UI (Giữ nguyên)
        if ("Employee".equals(currentUserRole)) {
            binding.fabAddStudent.setVisibility(View.GONE);
            binding.btnSortStudents.setVisibility(View.GONE);
        } else {
            binding.fabAddStudent.setVisibility(View.VISIBLE);
            binding.btnSortStudents.setVisibility(View.VISIBLE);
        }

        // 4. QUAN SÁT (OBSERVE) DỮ LIỆU TỪ VIEWMODEL
        viewModel.getStudentListLiveData().observe(getViewLifecycleOwner(), updatedList -> {
            studentList.clear();
            studentList.addAll(updatedList);
            studentAdapter.notifyDataSetChanged();
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // 5. Setup Listeners
        binding.fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditStudentActivity.class);
            startActivity(intent);
        });

        binding.btnSortStudents.setOnClickListener(v -> {
            showSortDialog();
        });

        binding.searchViewStudents.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.loadStudentsFromFirestore(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.loadStudentsFromFirestore(newText);
                return true;
            }
        });

        // 6. Tải dữ liệu ban đầu
        viewModel.loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());
    }

    private void showSortDialog() {
        final String[] sortOptions = {
                "Tên (A-Z)",
                "Tên (Z-A)",
                "Tuổi (Tăng dần)",
                "Tuổi (Giảm dần)"
        };

        // Lấy trạng thái sắp xếp hiện tại từ ViewModel
        int checkedItem = 0;
        String sortField = viewModel.getCurrentSortField();
        Query.Direction direction = viewModel.getCurrentSortDirection();

        if (sortField.equals("name") && direction == Query.Direction.ASCENDING) {
            checkedItem = 0;
        } else if (sortField.equals("name") && direction == Query.Direction.DESCENDING) {
            checkedItem = 1;
        } else if (sortField.equals("age") && direction == Query.Direction.ASCENDING) {
            checkedItem = 2;
        } else if (sortField.equals("age") && direction == Query.Direction.DESCENDING) {
            checkedItem = 3;
        }

        if (getContext() == null) {
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Sắp xếp danh sách")
                .setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewModel.setSortOrder("name", Query.Direction.ASCENDING);
                            break;
                        case 1:
                            viewModel.setSortOrder("name", Query.Direction.DESCENDING);
                            break;
                        case 2:
                            viewModel.setSortOrder("age", Query.Direction.ASCENDING);
                            break;
                        case 3:
                            viewModel.setSortOrder("age", Query.Direction.DESCENDING);
                            break;
                    }
                    if (binding != null) {
                        viewModel.loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // KHÔNG CẦN HÀM loadStudentsFromFirestore() ở đây

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            viewModel.loadStudentsFromFirestore(binding.searchViewStudents.getQuery().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}