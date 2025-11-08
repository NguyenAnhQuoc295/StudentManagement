package com.AnhQuoc.studentmanagementapp.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.auth.LoginActivity;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentProfileBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserRole;
    private static final String TAG = "ProfileFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        // 1. Nhận vai trò từ Bundle (do MainActivity gửi)
        if (getArguments() != null) {
            currentUserRole = getArguments().getString("USER_ROLE");
        }
        if (currentUserRole == null) {
            currentUserRole = "Employee"; // Mặc định an toàn
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Tải thông tin người dùng
        loadUserProfile();

        // 3. Xử lý nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            // Hiển thị dialog xác nhận
            new AlertDialog.Builder(getContext())
                    .setTitle("Xác nhận Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        logoutUser();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // 4. Xử lý nút Thay đổi ảnh (Sẽ làm ở bước sau)
        binding.btnChangeProfilePic.setOnClickListener(v -> {
            // TODO: Triển khai logic chọn ảnh và upload lên Firebase Storage
            Toast.makeText(getContext(), "Chức năng thay đổi ảnh đại diện đang phát triển.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            // Không có ai đăng nhập, quay về Login
            logoutUser();
            return;
        }

        String uid = firebaseUser.getUid();

        // Lấy thông tin từ Firestore bằng UID
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvProfileName.setText("Tên: " + user.getName());
                            binding.tvProfileRole.setText("Vai trò: " + user.getRole());

                            // (Nâng cao: Tải ảnh đại diện bằng Glide nếu có)
                            // Glide.with(this).load(user.getProfileImageUrl()).into(binding.imgUserProfile);
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy document cho UID: " + uid);
                        binding.tvProfileName.setText("Tên: [Lỗi dữ liệu]");
                        binding.tvProfileRole.setText("Vai trò: [Lỗi dữ liệu]");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lỗi khi lấy thông tin user", e);
                });
    }

    private void logoutUser() {
        mAuth.signOut(); // Đăng xuất khỏi Firebase Auth

        // Chuyển về màn hình Login
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        // Xóa hết các Activity cũ (MainActivity, v.v.) khỏi stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc Activity hiện tại (MainActivity)
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak
    }
}