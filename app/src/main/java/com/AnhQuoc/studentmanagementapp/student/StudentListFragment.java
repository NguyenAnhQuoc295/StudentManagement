// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.student;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentStudentListBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.Query;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint; // <-- IMPORT MỚI

@AndroidEntryPoint // <-- THÊM DÒNG NÀY
public class StudentListFragment extends Fragment {

    private FragmentStudentListBinding binding;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private String currentUserRole;
    private StudentListViewModel viewModel;

    private ActivityResultLauncher<String> importCsvLauncher;
    private ActivityResultLauncher<Intent> exportCsvLauncher;


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
        // Hilt sẽ tự động xử lý dòng này để cung cấp ViewModel chính xác
        viewModel = new ViewModelProvider(this).get(StudentListViewModel.class);

        // 2. Setup Adapter (Giữ nguyên)
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
            binding.btnImportStudents.setVisibility(View.GONE);
            binding.btnExportStudents.setVisibility(View.GONE);
        } else {
            binding.fabAddStudent.setVisibility(View.VISIBLE);
            binding.btnSortStudents.setVisibility(View.VISIBLE);
            binding.btnImportStudents.setVisibility(View.VISIBLE);
            binding.btnExportStudents.setVisibility(View.VISIBLE);
        }

        // 4. QUAN SÁT (OBSERVE) DỮ LIỆU TỪ VIEWMODEL (Giữ nguyên)
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

        viewModel.getExportCsvData().observe(getViewLifecycleOwner(), csvData -> {
            if (csvData != null && !csvData.isEmpty()) {
                createFileForExport(csvData);
            }
        });


        // 5. Setup Listeners (Giữ nguyên)
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
                viewModel.subscribeToStudentUpdates(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.subscribeToStudentUpdates(newText);
                return true;
            }
        });

        setupActivityLaunchers();

        binding.btnImportStudents.setOnClickListener(v -> {
            importCsvLauncher.launch("text/csv");
        });

        binding.btnExportStudents.setOnClickListener(v -> {
            viewModel.exportStudentsToCsv();
        });


        // 6. Tải dữ liệu ban đầu (Giữ nguyên)
        viewModel.subscribeToStudentUpdates(binding.searchViewStudents.getQuery().toString());
    }

    private void setupActivityLaunchers() {
        // (Giữ nguyên toàn bộ code của hàm này)
        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && getContext() != null) {
                        viewModel.importStudentsFromCsv(uri, getContext().getContentResolver());
                    }
                }
        );

        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        String csvData = viewModel.getExportCsvData().getValue();
                        if (uri != null && csvData != null && getContext() != null) {
                            try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
                                outputStream.write(csvData.getBytes(StandardCharsets.UTF_8));
                                Toast.makeText(getContext(), "Xuất tệp thành công!", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Lỗi khi lưu tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } finally {
                                viewModel.clearExportData();
                            }
                        } else {
                            viewModel.clearExportData();
                        }
                    } else {
                        viewModel.clearExportData();
                    }
                }
        );
    }

    private void createFileForExport(String csvData) {
        // (Giữ nguyên toàn bộ code của hàm này)
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "students_export.csv");
        exportCsvLauncher.launch(intent);
    }

    private void showSortDialog() {
        // (Giữ nguyên toàn bộ code của hàm này)
        final String[] sortOptions = {
                "Tên (A-Z)",
                "Tên (Z-A)",
                "Tuổi (Tăng dần)",
                "Tuổi (Giảm dần)"
        };

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
                        viewModel.subscribeToStudentUpdates(binding.searchViewStudents.getQuery().toString());
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    /*
    @Override
    public void onResume() {
        // (Đã bị xóa ở bước trước)
    }
    */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}