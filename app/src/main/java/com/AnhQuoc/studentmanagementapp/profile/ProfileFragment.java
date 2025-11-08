package com.AnhQuoc.studentmanagementapp.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.auth.LoginActivity;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentProfileBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserRole;
    private static final String TAG = "ProfileFragment";

    private StorageReference storageRef;
    private ActivityResultLauncher<String> mGetContent;
    private Uri selectedImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            currentUserRole = getArguments().getString("USER_ROLE");
        }
        if (currentUserRole == null) {
            currentUserRole = "Employee";
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && binding != null && getContext() != null) {
                        selectedImageUri = uri;
                        Glide.with(getContext())
                                .load(selectedImageUri)
                                .into(binding.imgUserProfile);
                        uploadImageToStorage();
                    }
                });

        loadUserProfile();

        // 3. Xử lý nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {

            // === SỬA LỖI QUAN TRỌNG: Thêm kiểm tra getContext() ===
            // Kiểm tra xem Fragment có còn "attached" không trước khi hiển thị Dialog
            if (getContext() == null) {
                return; // Fragment đã bị hủy, không làm gì cả
            }
            // ===================================================

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

        // 4. Xử lý nút Thay đổi ảnh
        binding.btnChangeProfilePic.setOnClickListener(v -> {
            mGetContent.launch("image/*");
        });
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            logoutUser();
            return;
        }

        String uid = firebaseUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || getContext() == null) {
                        return;
                    }

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvProfileName.setText("Tên: " + user.getName());
                            binding.tvProfileRole.setText("Vai trò: " + user.getRole());

                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(getContext())
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_profile)
                                        .into(binding.imgUserProfile);
                            } else {
                                Glide.with(getContext())
                                        .load(R.drawable.ic_profile)
                                        .into(binding.imgUserProfile);
                            }
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy document cho UID: " + uid);
                        binding.tvProfileName.setText("Tên: [Lỗi dữ liệu]");
                        binding.tvProfileRole.setText("Vai trò: [Lỗi dữ liệu]");
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) {
                        return;
                    }
                    Log.w(TAG, "Lỗi khi lấy thông tin user", e);
                });
    }

    private void uploadImageToStorage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selectedImageUri == null) {
            return;
        }

        String uid = user.getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

        if (getContext() != null) {
            Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
        }

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateProfileImageUrlInFirestore(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProfileImageUrlInFirestore(String downloadUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("profileImageUrl", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi khi lưu URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();

        if (getActivity() == null) {
            return;
        }

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}