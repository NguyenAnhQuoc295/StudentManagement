package com.AnhQuoc.studentmanagementapp.profile;

import android.content.Intent;
import android.net.Uri; // <-- THÊM IMPORT
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // <-- THÊM IMPORT
import androidx.activity.result.contract.ActivityResultContracts; // <-- THÊM IMPORT
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.auth.LoginActivity;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentProfileBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.bumptech.glide.Glide; // <-- THÊM IMPORT
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage; // <-- THÊM IMPORT
import com.google.firebase.storage.StorageReference; // <-- THÊM IMPORT

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserRole;
    private static final String TAG = "ProfileFragment";

    // === THÊM CÁC BIẾN MỚI ===
    private StorageReference storageRef;
    private ActivityResultLauncher<String> mGetContent;
    private Uri selectedImageUri;
    // ==========================

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
        storageRef = FirebaseStorage.getInstance().getReference(); // Khởi tạo Storage

        // === KHỞI TẠO LAUNCHER CHỌN ẢNH ===
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    // === SỬA LỖI: Kiểm tra trước khi dùng binding ===
                    if (uri != null && binding != null && getContext() != null) {
                        selectedImageUri = uri;
                        // Hiển thị ảnh vừa chọn lên ImageView
                        Glide.with(getContext()) // Dùng getContext() an toàn hơn
                                .load(selectedImageUri)
                                .into(binding.imgUserProfile);

                        // Tự động tải ảnh lên
                        uploadImageToStorage();
                    }
                });
        // ==================================

        // 2. Tải thông tin người dùng (ĐÃ CẬP NHẬT)
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

        // 4. Xử lý nút Thay đổi ảnh (ĐÃ CẬP NHẬT)
        binding.btnChangeProfilePic.setOnClickListener(v -> {
            // Mở thư viện ảnh
            mGetContent.launch("image/*");
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
                    // === SỬA LỖI: Kiểm tra binding và context trước khi cập nhật UI ===
                    if (binding == null || getContext() == null) {
                        return; // View đã bị hủy, không làm gì cả
                    }
                    // ==============================================================

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvProfileName.setText("Tên: " + user.getName());
                            binding.tvProfileRole.setText("Vai trò: " + user.getRole());

                            // === CẬP NHẬT LOGIC TẢI ẢNH ===
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(getContext()) // Dùng getContext() an toàn hơn
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_profile) // Ảnh chờ
                                        .into(binding.imgUserProfile);
                            } else {
                                // Nếu không có ảnh, dùng ảnh mặc định
                                Glide.with(getContext()) // Dùng getContext() an toàn hơn
                                        .load(R.drawable.ic_profile)
                                        .into(binding.imgUserProfile);
                            }
                            // =============================
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy document cho UID: " + uid);
                        binding.tvProfileName.setText("Tên: [Lỗi dữ liệu]");
                        binding.tvProfileRole.setText("Vai trò: [Lỗi dữ liệu]");
                    }
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra binding và context trước khi log lỗi ===
                    if (binding == null) {
                        return; // View đã bị hủy
                    }
                    // ==========================================================
                    Log.w(TAG, "Lỗi khi lấy thông tin user", e);
                });
    }

    // === HÀM MỚI 1: TẢI ẢNH LÊN STORAGE ===
    private void uploadImageToStorage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selectedImageUri == null) {
            return;
        }

        String uid = user.getUid();
        // Tạo đường dẫn trên Storage: profile_images/USER_ID.jpg
        StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

        // === SỬA LỖI: Kiểm tra context trước khi hiển thị Toast ===
        if (getContext() != null) {
            Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
        }
        // ======================================================

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Tải lên thành công, lấy URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        // Cập nhật URL này vào Firestore
                        updateProfileImageUrlInFirestore(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra context trước khi hiển thị Toast ===
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    // ======================================================
                });
    }

    // === HÀM MỚI 2: LƯU URL VÀO FIRESTORE ===
    private void updateProfileImageUrlInFirestore(String downloadUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("profileImageUrl", downloadUrl) // Cập nhật trường mới
                .addOnSuccessListener(aVoid -> {
                    // === SỬA LỖI: Kiểm tra context trước khi hiển thị Toast ===
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                    }
                    // ======================================================
                })
                .addOnFailureListener(e -> {
                    // === SỬA LỖI: Kiểm tra context trước khi hiển thị Toast ===
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi khi lưu URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    // ======================================================
                });
    }

    private void logoutUser() {
        mAuth.signOut(); // Đăng xuất khỏi Firebase Auth

        // === SỬA LỖI: Kiểm tra getActivity() trước khi dùng ===
        if (getActivity() == null) {
            return;
        }
        // ===================================================

        // Chuyển về màn hình Login
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        // Xóa hết các Activity cũ (MainActivity, v.v.) khỏi stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc Activity hiện tại (MainActivity)
        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak
    }
}