package com.AnhQuoc.studentmanagementapp.student;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View; // <-- THÊM IMPORT
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityStudentDetailsBinding;
import com.AnhQuoc.studentmanagementapp.databinding.DialogAddCertificateBinding;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class StudentDetailsActivity extends AppCompatActivity {

    private ActivityStudentDetailsBinding binding;
    private String currentStudentId;
    private String currentUserRole; // <-- THÊM BIẾN LƯU VAI TRÒ
    private FirebaseFirestore db;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        binding.toolbarDetail.setNavigationOnClickListener(v -> finish());

        // 1. Nhận ID sinh viên VÀ Vai trò người dùng
        if (getIntent().hasExtra("STUDENT_ID")) {
            currentStudentId = getIntent().getStringExtra("STUDENT_ID");
            currentUserRole = getIntent().getStringExtra("USER_ROLE");

            if (currentUserRole == null) {
                currentUserRole = "Employee"; // Mặc định an toàn
            }

            loadStudentData(currentStudentId);
            loadCertificates(currentStudentId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID sinh viên", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 2. Setup RecyclerView (truyền vai trò vào CertificateAdapter)
        setupCertificateRecyclerView();

        // 3. Setup các nút (đã bao gồm phân quyền)
        setupButtonListeners();

        // 4. THỰC THI PHÂN QUYỀN
        applyRoleBasedUI();
    }

    // HÀM MỚI: Ẩn/hiện các nút dựa trên vai trò
    private void applyRoleBasedUI() {
        if ("Employee".equals(currentUserRole)) {
            // Nếu là Employee, ẩn hết các nút hành động
            binding.btnEditStudent.setVisibility(View.GONE);
            binding.btnDeleteStudent.setVisibility(View.GONE);
            binding.fabAddCertificate.setVisibility(View.GONE);
        } else {
            // (Admin/Manager) Hiện các nút
            binding.btnEditStudent.setVisibility(View.VISIBLE);
            binding.btnDeleteStudent.setVisibility(View.VISIBLE);
            binding.fabAddCertificate.setVisibility(View.VISIBLE);
        }
    }

    private void setupCertificateRecyclerView() {
        certificateList = new ArrayList<>();

        // Truyền vai trò vào CertificateAdapter
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
                            deleteCertificateFromFirestore(certificate.getCertificateId());
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(R.drawable.ic_delete)
                        .show();
            }
        }, currentUserRole); // <-- TRUYỀN VAI TRÒ VÀO ĐÂY

        binding.rvCertificates.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCertificates.setAdapter(certificateAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentStudentId != null) {
            // Tải lại dữ liệu khi quay lại (nếu có sửa)
            loadStudentData(currentStudentId);
            loadCertificates(currentStudentId);
        }
    }

    private void setupButtonListeners() {
        binding.btnDeleteStudent.setOnClickListener(v -> {
            new AlertDialog.Builder(StudentDetailsActivity.this)
                    .setTitle("Xác nhận Xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa sinh viên này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteStudent(currentStudentId))
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

    private void loadStudentData(String studentId) {
        binding.toolbarDetail.setTitle("Đang tải...");
        db.collection("students").document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return; // Activity đã bị hủy, không làm gì cả
                    }
                    // =======================================================

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Student student = document.toObject(Student.class);
                            if (student != null) {
                                binding.toolbarDetail.setTitle(student.getName());
                                binding.tvDetailAge.setText("Tuổi: " + student.getAge());
                                binding.tvDetailPhone.setText("SĐT: " + student.getPhone());
                                Glide.with(StudentDetailsActivity.this)
                                        .load(R.drawable.ic_profile)
                                        .into(binding.imgStudentDetail);
                            }
                        } else {
                            Toast.makeText(StudentDetailsActivity.this, "Không tìm thấy sinh viên", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(StudentDetailsActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteStudent(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;

        // (Nâng cao: Xóa sub-collection "certificates" trước khi xóa document cha)
        // Tạm thời chỉ xóa document
        db.collection("students").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(StudentDetailsActivity.this, "Xóa sinh viên thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(StudentDetailsActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

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
                        Certificate newCertificate = new Certificate(name, date);
                        saveCertificateToFirestore(newCertificate);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveCertificateToFirestore(Certificate certificate) {
        if (currentStudentId == null || currentStudentId.isEmpty()) return;
        db.collection("students").document(currentStudentId)
                .collection("certificates")
                .add(certificate)
                .addOnSuccessListener(documentReference -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(StudentDetailsActivity.this, "Thêm chứng chỉ thành công", Toast.LENGTH_SHORT).show();
                    loadCertificates(currentStudentId);
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(StudentDetailsActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCertificates(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        db.collection("students").document(studentId)
                .collection("certificates")
                .get()
                .addOnCompleteListener(task -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================

                    if (task.isSuccessful()) {
                        certificateList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Certificate certificate = document.toObject(Certificate.class);
                            certificate.setCertificateId(document.getId());
                            certificateList.add(certificate);
                        }
                        certificateAdapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting certificates.", task.getException());
                    }
                });
    }

    private void deleteCertificateFromFirestore(String certificateId) {
        if (currentStudentId == null || certificateId == null) return;
        db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificateId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(this, "Xóa chứng chỉ thành công", Toast.LENGTH_SHORT).show();
                    loadCertificates(currentStudentId);
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                        updateCertificateInFirestore(certificateToEdit, newName, newDate);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateCertificateInFirestore(Certificate certificate, String newName, String newDate) {
        if (currentStudentId == null || certificate.getCertificateId() == null) return;

        DocumentReference certRef = db.collection("students").document(currentStudentId)
                .collection("certificates").document(certificate.getCertificateId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("dateIssued", newDate);

        certRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(this, "Cập nhật chứng chỉ thành công", Toast.LENGTH_SHORT).show();
                    loadCertificates(currentStudentId);
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}