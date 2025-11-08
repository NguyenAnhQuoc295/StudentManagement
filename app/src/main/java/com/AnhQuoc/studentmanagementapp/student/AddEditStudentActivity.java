package com.AnhQuoc.studentmanagementapp.student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.AnhQuoc.studentmanagementapp.databinding.ActivityAddEditStudentBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEditStudentActivity extends AppCompatActivity {

    private ActivityAddEditStudentBinding binding;
    private FirebaseFirestore db;

    // === THAY ĐỔI 1: Thêm các biến để quản lý 2 chế độ ===
    private boolean isEditMode = false; // Mặc định là chế độ "Thêm mới"
    private String studentIdToEdit; // ID của sinh viên cần sửa
    // =================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        // === THAY ĐỔI 2: Kiểm tra Intent ===
        if (getIntent().hasExtra("STUDENT_ID_TO_EDIT")) {
            // Nếu có ID gửi sang -> Đây là chế độ "Sửa"
            isEditMode = true;
            studentIdToEdit = getIntent().getStringExtra("STUDENT_ID_TO_EDIT");

            // Đổi tiêu đề và tải dữ liệu cũ
            binding.toolbarAddEditStudent.setTitle("Sửa Thông Tin Sinh Viên");
            loadStudentDataForEdit(studentIdToEdit);

        } else {
            // Nếu không có ID -> Đây là chế độ "Thêm mới"
            isEditMode = false;
            binding.toolbarAddEditStudent.setTitle("Thêm Sinh Viên Mới");
        }
        // ==================================

        // Xử lý nút quay lại (ic_arrow_back)
        binding.toolbarAddEditStudent.setNavigationOnClickListener(v -> finish());

        // Xử lý nút "Lưu lại"
        binding.btnSaveStudent.setOnClickListener(v -> {
            // === THAY ĐỔI 3: Quyết định Sửa hay Thêm ===
            if (isEditMode) {
                updateStudentInFirestore(); // Gọi hàm Cập nhật
            } else {
                saveStudentToFirestore(); // Gọi hàm Thêm mới (hàm cũ)
            }
            // ======================================
        });
    }

    // Hàm Thêm mới (giống hệt code cũ của bạn)
    private void saveStudentToFirestore() {
        String name = binding.etStudentName.getText().toString().trim();
        String ageStr = binding.etStudentAge.getText().toString().trim();
        String phone = binding.etStudentPhone.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        int age = Integer.parseInt(ageStr);
        Student student = new Student(name, age, phone);

        db.collection("students")
                .add(student)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddEditStudentActivity.this, "Thêm sinh viên thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditStudentActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // === THÊM HÀM MỚI 1: Tải dữ liệu lên để sửa ===
    private void loadStudentDataForEdit(String studentId) {
        db.collection("students").document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Student student = document.toObject(Student.class);
                            if (student != null) {
                                // Điền thông tin cũ vào các ô
                                binding.etStudentName.setText(student.getName());
                                binding.etStudentAge.setText(String.valueOf(student.getAge()));
                                binding.etStudentPhone.setText(student.getPhone());
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy sinh viên", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }
    // ==========================================

    // === THÊM HÀM MỚI 2: Cập nhật dữ liệu ===
    private void updateStudentInFirestore() {
        String name = binding.etStudentName.getText().toString().trim();
        String ageStr = binding.etStudentAge.getText().toString().trim();
        String phone = binding.etStudentPhone.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        int age = Integer.parseInt(ageStr);

        // Tạo đối tượng Student mới
        Student updatedStudent = new Student(name, age, phone);

        // Gọi hàm .set() để CẬP NHẬT (thay vì .add())
        db.collection("students").document(studentIdToEdit)
                .set(updatedStudent) // .set() sẽ ghi đè dữ liệu cũ
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Đóng màn hình
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    // ======================================
}