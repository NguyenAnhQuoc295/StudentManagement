package com.AnhQuoc.studentmanagementapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.AnhQuoc.studentmanagementapp.MainActivity;
import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityLoginBinding; // Quan trọng: Import file binding

public class LoginActivity extends AppCompatActivity {

    // Khai báo biến binding
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Khởi tạo binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());

        // 2. Thiết lập view, thay thế cho setContentView(R.layout.activity_login)
        setContentView(binding.getRoot());

        // 3. Xử lý sự kiện nhấn nút đăng nhập
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy dữ liệu từ các ô EditText
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();

                // Kiểm tra đơn giản (sau này sẽ thay bằng logic Firebase)
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                } else {
                    // Nếu đã nhập, giả vờ đăng nhập thành công
                    // và chuyển sang MainActivity
                    loginSuccess();
                }
            }
        });
    }

    private void loginSuccess() {
        // Tạo một Intent để chuyển màn hình
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        // (Sau này, bạn sẽ gửi vai trò của người dùng qua đây, ví dụ: intent.putExtra("USER_ROLE", "ADMIN"))

        startActivity(intent);

        // Kết thúc LoginActivity để người dùng không thể quay lại bằng nút back
        finish();
    }
}