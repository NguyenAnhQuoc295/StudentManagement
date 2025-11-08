package com.AnhQuoc.studentmanagementapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.AnhQuoc.studentmanagementapp.MainActivity;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityLoginBinding;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory; // <-- THÊM IMPORT
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // <-- THÊM IMPORT
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi tạo Firebase Auth và Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // (Tùy chọn) Kiểm tra xem user đã đăng nhập từ lần trước chưa
        // ...

        // 2. Xử lý sự kiện nhấn nút đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm đăng nhập
            loginUserWithFirebase(email, password);
        });
    }

    private void loginUserWithFirebase(String email, String password) {
        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập Auth thành công
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Bây giờ, lấy vai trò (role) từ Firestore
                        // === CẬP NHẬT: TRUYỀN CẢ UID VÀ EMAIL ===
                        getUserRoleAndProceed(user.getUid(), user.getEmail());
                    } else {
                        // Đăng nhập thất bại
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    // === CẬP NHẬT: HÀM NÀY GIỜ NHẬN CẢ UID ===
    private void getUserRoleAndProceed(String uid, String userEmail) {
        // Truy vấn collection "users"
        // (Chúng ta dùng UID để lấy document, vì bạn đã thiết lập nó trong AddEditUserActivity)
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Tìm thấy user trong Firestore
                            String userRole = document.getString("role");
                            String userStatus = document.getString("status");

                            if (userRole == null) {
                                Toast.makeText(this, "Không tìm thấy vai trò cho người dùng này.", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                                mAuth.signOut(); // Đăng xuất nếu có lỗi
                                return;
                            }

                            if ("Locked".equals(userStatus)) {
                                Toast.makeText(this, "Tài khoản của bạn đã bị khóa.", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                                mAuth.signOut(); // Đăng xuất
                                return;
                            }

                            // === GỌI HÀM GHI LỊCH SỬ ===
                            recordLoginHistory(uid);
                            // ============================

                            // Đăng nhập thành công và có vai trò
                            loginSuccess(userRole);
                        } else {
                            // Không tìm thấy user trong Firestore
                            Toast.makeText(this, "Không tìm thấy thông tin (database) của người dùng.", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error getting user role, document does not exist.");
                            showLoading(false);
                            mAuth.signOut(); // Đăng xuất nếu có lỗi
                        }
                    } else {
                        // Lỗi khi truy vấn
                        Toast.makeText(this, "Lỗi khi lấy thông tin vai trò người dùng.", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Error getting user role.", task.getException());
                        showLoading(false);
                        mAuth.signOut(); // Đăng xuất nếu có lỗi
                    }
                });
    }

    // === HÀM MỚI: GHI LỊCH SỬ ĐĂNG NHẬP ===
    private void recordLoginHistory(String uid) {
        // Tạo một đối tượng lịch sử mới
        // (Tạm thời bỏ qua IP, chỉ lưu loại thiết bị)
        LoginHistory historyEntry = new LoginHistory(null, "Android");

        // Lưu vào sub-collection của user
        db.collection("users").document(uid)
                .collection("login_history")
                .add(historyEntry)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Login history recorded successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error recording login history.", e);
                    // Không cần thông báo cho user, chỉ cần log lại
                });
    }


    private void loginSuccess(String userRole) {
        showLoading(false);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Gửi vai trò của người dùng sang MainActivity
        intent.putExtra("USER_ROLE", userRole);
        startActivity(intent);
        finish(); // Đóng LoginActivity
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
        }
    }
}