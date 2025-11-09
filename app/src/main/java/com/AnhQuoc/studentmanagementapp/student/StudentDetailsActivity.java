package com.AnhQuoc.studentmanagementapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // <-- THÊM IMPORT
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityStudentDetailsBinding;
import com.AnhQuoc.studentmanagementapp.databinding.DialogAddCertificateBinding;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.bumptech.glide.Glide;
// KHÔNG CẦN import Firebase
import java.util.ArrayList;
import java.util.List;

public class StudentDetailsActivity extends AppCompatActivity {

    private ActivityStudentDetailsBinding binding;
    private String currentStudentId;
    private String currentUserRole;
    private StudentDetailsViewModel viewModel; // <-- Biến ViewModel
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbarDetail.setNavigationOnClickListener(v -> finish());

        // 1. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(StudentDetailsViewModel.class);

        // 2. Nhận ID sinh viên VÀ Vai trò người dùng
        if (getIntent().hasExtra("STUDENT_ID")) {
            currentStudentId = getIntent().getStringExtra("STUDENT_ID");
            currentUserRole = getIntent().getStringExtra("USER_ROLE");
            if (currentUserRole == null) currentUserRole = "Employee";

            // 3. Đặt ID cho ViewModel (ViewModel sẽ tự tải dữ liệu)
            viewModel.setStudentId(currentStudentId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID sinh viên", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Setup RecyclerView
        setupCertificateRecyclerView();

        // 5. Setup các nút (đã bao gồm phân quyền)
        setupButtonListeners();
        applyRoleBasedUI();

        // 6. QUAN SÁT (OBSERVE) DỮ LIỆU TỪ VIEWMODEL
        observeViewModel();
    }

    private void observeViewModel() {
        // Quan sát thông tin sinh viên
        viewModel.getStudentLiveData().observe(this, student -> {
            if (student != null) {
                binding.toolbarDetail.setTitle(student.getName());
                binding.tvDetailAge.setText("Tuổi: " + student.getAge());
                binding.tvDetailPhone.setText("SĐT: " + student.getPhone());
                Glide.with(StudentDetailsActivity.this)
                        .load(R.drawable.ic_profile)
                        .into(binding.imgStudentDetail);
            }
        });

        // Quan sát danh sách chứng chỉ
        viewModel.getCertificatesLiveData().observe(this, certificates -> {
            certificateList.clear();
            certificateList.addAll(certificates);
            certificateAdapter.notifyDataSetChanged();
        });

        // Quan sát thông báo
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Quan sát sự kiện đóng
        viewModel.getCloseActivityEvent().observe(this, shouldClose -> {
            if (shouldClose) {
                finish();
            }
        });
    }

    private void applyRoleBasedUI() {
        if ("Employee".equals(currentUserRole)) {
            binding.btnEditStudent.setVisibility(View.GONE);
            binding.btnDeleteStudent.setVisibility(View.GONE);
            binding.fabAddCertificate.setVisibility(View.GONE);
        } else {
            binding.btnEditStudent.setVisibility(View.VISIBLE);
            binding.btnDeleteStudent.setVisibility(View.VISIBLE);
            binding.fabAddCertificate.setVisibility(View.VISIBLE);
        }
    }

    private void setupCertificateRecyclerView() {
        certificateList = new ArrayList<>();
        certificateAdapter = new CertificateAdapter(certificateList, new CertificateAdapter.OnCertificateClickListener() {
            @Override
            public void onEditClick(Certificate certificate) {
                showEditCertificateDialog(certificate);
            }

            @Override
            public void onDeleteClick(Certificate certificate) {
                new AlertDialog.Builder(StudentDetailsActivity.this)
                        .setTitle("Xác nhận Xóa")
                        .setMessage("Xóa chứng chỉ: " + certificate.getName() + "?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            viewModel.deleteCertificateFromFirestore(certificate.getCertificateId());
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(R.drawable.ic_delete)
                        .show();
            }
        }, currentUserRole);

        binding.rvCertificates.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCertificates.setAdapter(certificateAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Yêu cầu ViewModel tải lại dữ liệu (vì có thể đã sửa)
        viewModel.refreshData();
    }

    private void setupButtonListeners() {
        binding.btnDeleteStudent.setOnClickListener(v -> {
            new AlertDialog.Builder(StudentDetailsActivity.this)
                    .setTitle("Xác nhận Xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa sinh viên này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteStudent())
                    .setNegativeButton("Hủy", null)
                    .setIcon(R.drawable.ic_delete)
                    .show();
        });

        binding.btnEditStudent.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDetailsActivity.this, AddEditStudentActivity.class);
            intent.putExtra("STUDENT_ID_TO_EDIT", currentStudentId);
            startActivity(intent);
        });

        binding.fabAddCertificate.setOnClickListener(v -> showAddCertificateDialog());
    }

    // KHÔNG CẦN CÁC HÀM loadStudentData, deleteStudent, loadCertificates,
    // saveCertificateToFirestore, deleteCertificateFromFirestore, updateCertificateInFirestore

    private void showAddCertificateDialog() {
        DialogAddCertificateBinding dialogBinding = DialogAddCertificateBinding.inflate(LayoutInflater.from(this));
        new AlertDialog.Builder(this)
                .setTitle("Thêm Chứng Chỉ Mới")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = dialogBinding.etCertificateName.getText().toString().trim();
                    String date = dialogBinding.etCertificateDate.getText().toString().trim();
                    if (name.isEmpty() || date.isEmpty()) {
                        Toast.makeText(StudentDetailsActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.saveCertificateToFirestore(name, date);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditCertificateDialog(Certificate certificateToEdit) {
        DialogAddCertificateBinding dialogBinding = DialogAddCertificateBinding.inflate(LayoutInflater.from(this));
        dialogBinding.etCertificateName.setText(certificateToEdit.getName());
        dialogBinding.etCertificateDate.setText(certificateToEdit.getDateIssued());

        new AlertDialog.Builder(this)
                .setTitle("Sửa Chứng Chỉ")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = dialogBinding.etCertificateName.getText().toString().trim();
                    String newDate = dialogBinding.etCertificateDate.getText().toString().trim();

                    if (newName.isEmpty() || newDate.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.updateCertificateInFirestore(certificateToEdit.getCertificateId(), newName, newDate);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}