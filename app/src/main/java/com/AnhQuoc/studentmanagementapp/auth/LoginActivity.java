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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        // FirebaseUser currentUser = mAuth.getCurrentUser();
        // if (currentUser != null) {
        //     // Nếu đã đăng nhập, ta nên lấy vai trò và chuyển thẳng
        //     // Tạm thời bỏ qua để tập trung vào logic login
        // }

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
                        getUserRoleAndProceed(user.getEmail());
                    } else {
                        // Đăng nhập thất bại
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void getUserRoleAndProceed(String userEmail) {
        // Truy vấn collection "users" nơi trường "name" (email) khớp
        // (Lưu ý: Tên trường phải khớp với file AddEditUserActivity)
        Query query = db.collection("users").whereEqualTo("name", userEmail);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                // Tìm thấy user trong Firestore
                // (Giả sử email là duy nhất, lấy document đầu tiên)
                String userRole = task.getResult().getDocuments().get(0).getString("role");
                String userStatus = task.getResult().getDocuments().get(0).getString("status");

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

                // Đăng nhập thành công và có vai trò
                loginSuccess(userRole);

            } else {
                // Không tìm thấy user trong Firestore hoặc có lỗi
                Toast.makeText(this, "Lỗi khi lấy thông tin vai trò người dùng.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Error getting user role.", task.getException());
                showLoading(false);
                mAuth.signOut(); // Đăng xuất nếu có lỗi
            }
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