package com.AnhQuoc.studentmanagementapp.student;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityAddEditStudentBinding; // <-- THÊM DÒNG NÀY ĐỂ SỬA LỖI
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Đã thêm Hilt
public class AddEditStudentActivity extends AppCompatActivity {

    private ActivityAddEditStudentBinding binding; // Dòng này sẽ hết báo lỗi
    private AddEditStudentViewModel viewModel;
    private boolean isEditMode = false;
    private String studentIdToEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi tạo ViewModel (Hilt sẽ cung cấp)
        viewModel = new ViewModelProvider(this).get(AddEditStudentViewModel.class);

        // 2. Kiểm tra Intent (Chế độ Sửa hay Thêm)
        if (getIntent().hasExtra("STUDENT_ID_TO_EDIT")) {
            isEditMode = true;
            studentIdToEdit = getIntent().getStringExtra("STUDENT_ID_TO_EDIT");
            binding.toolbarAddEditStudent.setTitle("Sửa Thông Tin Sinh Viên");
            // Yêu cầu ViewModel tải dữ liệu
            viewModel.loadStudentDataForEdit(studentIdToEdit);
        } else {
            isEditMode = false;
            binding.toolbarAddEditStudent.setTitle("Thêm Sinh Viên Mới");
        }

        // 3. QUAN SÁT (OBSERVE) DỮ LIỆU
        viewModel.getStudentLiveData().observe(this, student -> {
            if (student != null) {
                binding.etStudentName.setText(student.getName());
                binding.etStudentAge.setText(String.valueOf(student.getAge()));
                binding.etStudentPhone.setText(student.getPhone());
            }
        });

        viewModel.getSaveSuccessEvent().observe(this, success -> {
            if (success) {
                finish(); // Đóng Activity
            }
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Setup Listeners
        binding.toolbarAddEditStudent.setNavigationOnClickListener(v -> finish());

        binding.btnSaveStudent.setOnClickListener(v -> {
            String name = binding.etStudentName.getText().toString().trim();
            String ageStr = binding.etStudentAge.getText().toString().trim();
            String phone = binding.etStudentPhone.getText().toString().trim();

            if (isEditMode) {
                viewModel.updateStudentInFirestore(studentIdToEdit, name, ageStr, phone);
            } else {
                viewModel.saveStudentToFirestore(name, ageStr, phone);
            }
        });
    }
}