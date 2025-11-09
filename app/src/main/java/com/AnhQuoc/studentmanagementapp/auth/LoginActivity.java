package com.AnhQuoc.studentmanagementapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.AnhQuoc.studentmanagementapp.MainActivity;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityLoginBinding; // <-- THÊM DÒNG NÀY ĐỂ SỬA LỖI
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding; // Dòng này sẽ hết báo lỗi
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi tạo ViewModel (Hilt sẽ cung cấp)
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 2. Quan sát (Observe) LiveData
        viewModel.getLoginResultLiveData().observe(this, loginResult -> {
            if (loginResult == null) return;

            switch (loginResult.getStatus()) {
                case SUCCESS:
                    loginSuccess(loginResult.getData()); // data là "role"
                    break;
                case ERROR:
                    Toast.makeText(LoginActivity.this, loginResult.getData(), Toast.LENGTH_SHORT).show(); // data là "message"
                    break;
                case LOCKED:
                    Toast.makeText(LoginActivity.this, loginResult.getData(), Toast.LENGTH_SHORT).show(); // data là "message"
                    break;
            }
        });

        viewModel.getIsLoadingLiveData().observe(this, this::showLoading);

        // 3. Xử lý sự kiện nhấn nút đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi ViewModel để đăng nhập
            viewModel.loginUserWithFirebase(email, password);
        });
    }

    private void loginSuccess(String userRole) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_ROLE", userRole);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
    }
}