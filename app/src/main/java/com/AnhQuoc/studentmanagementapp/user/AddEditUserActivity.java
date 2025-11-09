package com.AnhQuoc.studentmanagementapp.user;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityAddEditUserBinding; // <-- THÊM DÒNG NÀY ĐỂ SỬA LỖI
import com.AnhQuoc.studentmanagementapp.model.User;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Đã thêm Hilt
public class AddEditUserActivity extends AppCompatActivity {

    private ActivityAddEditUserBinding binding; // Dòng này sẽ hết báo lỗi
    private AddEditUserViewModel viewModel;
    private boolean isEditMode = false;
    private String userIdToEdit;
    private User currentUserData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi tạo ViewModel (Hilt sẽ cung cấp)
        viewModel = new ViewModelProvider(this).get(AddEditUserViewModel.class);

        // 2. Xử lý nút quay lại
        binding.toolbarAddEditUser.setNavigationOnClickListener(v -> finish());

        // 3. Kiểm tra chế độ (Thêm hay Sửa)
        if (getIntent().hasExtra("USER_ID_TO_EDIT")) {
            isEditMode = true;
            userIdToEdit = getIntent().getStringExtra("USER_ID_TO_EDIT");
            binding.toolbarAddEditUser.setTitle("Sửa Thông Tin Người Dùng");
            viewModel.loadUserData(userIdToEdit); // Yêu cầu VM tải
        } else {
            isEditMode = false;
            binding.toolbarAddEditUser.setTitle("Thêm Người Dùng Mới");
            binding.rbManager.setChecked(true);
            binding.rbNormal.setChecked(true);
            binding.tilUserPassword.setHint("Mật khẩu (Bắt buộc)");
        }

        // 4. QUAN SÁT (OBSERVE) DỮ LIỆU
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                currentUserData = user; // Lưu lại để cập nhật
                binding.etUserName.setText(currentUserData.getName());
                binding.etUserAge.setText(String.valueOf(currentUserData.getAge()));
                binding.etUserPhone.setText(currentUserData.getPhone());
                if ("Manager".equals(currentUserData.getRole())) binding.rbManager.setChecked(true);
                else binding.rbEmployee.setChecked(true);
                if ("Locked".equals(currentUserData.getStatus())) binding.rbLocked.setChecked(true);
                else binding.rbNormal.setChecked(true);
                binding.etUserName.setEnabled(false);
                binding.tilUserPassword.setHint("Mật khẩu (bỏ trống nếu không đổi)");
            }
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSaveSuccessEvent().observe(this, success -> {
            if (success) {
                finish();
            }
        });

        viewModel.getIsLoading().observe(this, this::showLoading);

        // 5. Setup Listeners
        binding.btnSaveUser.setOnClickListener(v -> {
            if (isEditMode) {
                updateUser();
            } else {
                createUser();
            }
        });
    }

    private void createUser() {
        String email = binding.etUserName.getText().toString().trim();
        String password = binding.etUserPassword.getText().toString().trim();
        String ageStr = binding.etUserAge.getText().toString().trim();
        String phone = binding.etUserPhone.getText().toString().trim();
        String role = ((RadioButton) findViewById(binding.rgRole.getCheckedRadioButtonId())).getText().toString();
        String status = ((RadioButton) findViewById(binding.rgStatus.getCheckedRadioButtonId())).getText().toString();

        viewModel.createUserInAuthAndFirestore(email, password, ageStr, phone, role, status);
    }

    private void updateUser() {
        String ageStr = binding.etUserAge.getText().toString().trim();
        String phone = binding.etUserPhone.getText().toString().trim();
        String role = ((RadioButton) findViewById(binding.rgRole.getCheckedRadioButtonId())).getText().toString();
        String status = ((RadioButton) findViewById(binding.rgStatus.getCheckedRadioButtonId())).getText().toString();

        // Lấy mật khẩu mới
        String newPassword = binding.etUserPassword.getText().toString().trim();

        if (currentUserData == null) {
            Toast.makeText(this, "Chưa tải xong dữ liệu, vui lòng đợi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Truyền mật khẩu mới vào ViewModel
        viewModel.updateUserInFirestore(userIdToEdit, newPassword, ageStr, phone, role, status, currentUserData);
    }

    private void showLoading(boolean isLoading) {
        binding.btnSaveUser.setEnabled(!isLoading);
        // Bạn cũng có thể thêm ProgressBar và Bật/Tắt nó ở đây
    }
}