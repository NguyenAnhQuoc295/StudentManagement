package com.AnhQuoc.studentmanagementapp.user;

import android.os.Bundle;
import android.view.View; // <-- THÊM IMPORT NÀY
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityAddEditUserBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.auth.FirebaseAuth; // <-- THÊM IMPORT NÀY
import com.google.firebase.auth.FirebaseUser; // <-- THÊM IMPORT NÀY
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEditUserActivity extends AppCompatActivity {

    private ActivityAddEditUserBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // <-- THÊM BIẾN NÀY
    private boolean isEditMode = false;
    private String userIdToEdit;
    private User currentUserData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // <-- KHỞI TẠO BIẾN NÀY

        // Xử lý nút quay lại
        binding.toolbarAddEditUser.setNavigationOnClickListener(v -> finish());

        // Kiểm tra chế độ (Thêm hay Sửa)
        if (getIntent().hasExtra("USER_ID_TO_EDIT")) {
            isEditMode = true;
            userIdToEdit = getIntent().getStringExtra("USER_ID_TO_EDIT");
            binding.toolbarAddEditUser.setTitle("Sửa Thông Tin Người Dùng");
            loadUserData(userIdToEdit);
        } else {
            isEditMode = false;
            binding.toolbarAddEditUser.setTitle("Thêm Người Dùng Mới");
            binding.rbManager.setChecked(true);
            binding.rbNormal.setChecked(true);
            binding.tilUserPassword.setHint("Mật khẩu (Bắt buộc)");
        }

        binding.btnSaveUser.setOnClickListener(v -> {
            if (isEditMode) {
                updateUserInFirestore();
            } else {
                // Đổi tên hàm để rõ ràng hơn
                createUserInAuthAndFirestore();
            }
        });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================

                    if (documentSnapshot.exists()) {
                        currentUserData = documentSnapshot.toObject(User.class);
                        if (currentUserData != null) {
                            binding.etUserName.setText(currentUserData.getName());
                            binding.etUserAge.setText(String.valueOf(currentUserData.getAge()));
                            binding.etUserPhone.setText(currentUserData.getPhone());

                            if ("Manager".equals(currentUserData.getRole())) {
                                binding.rbManager.setChecked(true);
                            } else {
                                binding.rbEmployee.setChecked(true);
                            }

                            if ("Locked".equals(currentUserData.getStatus())) {
                                binding.rbLocked.setChecked(true);
                            } else {
                                binding.rbNormal.setChecked(true);
                            }

                            // Không cho sửa Tên (Email/Username) khi đang edit
                            binding.etUserName.setEnabled(false);
                            binding.tilUserPassword.setHint("Mật khẩu (bỏ trống nếu không đổi)");
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    Toast.makeText(this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // === HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT HOÀN TOÀN ===
    private void createUserInAuthAndFirestore() {
        // 1. Lấy dữ liệu
        String email = binding.etUserName.getText().toString().trim(); // Tên = email
        String password = binding.etUserPassword.getText().toString().trim();
        String ageStr = binding.etUserAge.getText().toString().trim();
        String phone = binding.etUserPhone.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true); // Hiển thị loading

        // 2. Tạo tài khoản trong Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================

                    if (task.isSuccessful()) {
                        // Tạo Auth thành công, giờ lưu vào Firestore
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser.getUid(); // Lấy UID (unique ID)

                        int age = Integer.parseInt(ageStr);
                        String role = ((RadioButton) findViewById(binding.rgRole.getCheckedRadioButtonId())).getText().toString();
                        String status = ((RadioButton) findViewById(binding.rgStatus.getCheckedRadioButtonId())).getText().toString();

                        // Tạo đối tượng User
                        // Lưu ý: trường "name" phải là email để LoginActivity có thể tìm thấy
                        User newUser = new User(email, age, phone, status, role);

                        // 3. Tạo document trong Firestore
                        // (Sử dụng UID từ Auth làm ID document để dễ quản lý)
                        db.collection("users").document(uid)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    // =======================================================
                                    showLoading(false);
                                    Toast.makeText(this, "Thêm người dùng thành công!", Toast.LENGTH_SHORT).show();
                                    finish(); // Đóng Activity
                                })
                                .addOnFailureListener(e -> {
                                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    // =======================================================
                                    showLoading(false);
                                    Toast.makeText(this, "Lỗi khi lưu vào Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    // (Nâng cao: nên xóa tài khoản Auth nếu lưu Firestore thất bại)
                                });

                    } else {
                        // Tạo Auth thất bại
                        showLoading(false);
                        Toast.makeText(this, "Lỗi khi tạo tài khoản: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUserInFirestore() {
        String ageStr = binding.etUserAge.getText().toString().trim();
        String phone = binding.etUserPhone.getText().toString().trim();
        String newPassword = binding.etUserPassword.getText().toString().trim();

        if (ageStr.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin (trừ mật khẩu)", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Xử lý logic cập nhật mật khẩu nếu newPassword không rỗng
        // (Cần bắt người dùng đăng nhập lại)

        showLoading(true);
        currentUserData.setAge(Integer.parseInt(ageStr));
        currentUserData.setPhone(phone);
        currentUserData.setRole(((RadioButton) findViewById(binding.rgRole.getCheckedRadioButtonId())).getText().toString());
        currentUserData.setStatus(((RadioButton) findViewById(binding.rgStatus.getCheckedRadioButtonId())).getText().toString());


        db.collection("users").document(userIdToEdit)
                .set(currentUserData)
                .addOnSuccessListener(aVoid -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    showLoading(false);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================
                    showLoading(false);
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Thêm hàm hiển thị loading
    private void showLoading(boolean isLoading) {
        // (Bạn có thể thêm ProgressBar vào layout nếu muốn)
        binding.btnSaveUser.setEnabled(!isLoading);
    }
}