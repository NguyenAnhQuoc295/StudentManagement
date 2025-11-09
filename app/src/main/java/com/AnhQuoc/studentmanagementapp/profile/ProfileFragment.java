package com.AnhQuoc.studentmanagementapp.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider; // <-- THÊM IMPORT

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.auth.LoginActivity;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentProfileBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
// KHÔNG CẦN import FirebaseFirestore hay Storage nữa

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private String currentUserRole;

    private ProfileViewModel viewModel; // <-- Biến ViewModel
    private ActivityResultLauncher<String> mGetContent;

    // Không cần các biến Firebase db hay storageRef ở đây

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

        // 1. LẤY VIEWMODEL
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // 2. QUAN SÁT (OBSERVE) DỮ LIỆU TỪ VIEWMODEL

        // Quan sát dữ liệu người dùng
        viewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            // Code này CHỈ CHẠY khi Fragment "sống" và user != null
            if (user != null) {
                binding.tvProfileName.setText("Tên: " + user.getName());
                binding.tvProfileRole.setText("Vai trò: " + user.getRole());

                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    // Cần kiểm tra getContext() vì Glide chạy bất đồng bộ
                    if (getContext() != null) {
                        Glide.with(getContext())
                                .load(user.getProfileImageUrl())
                                .placeholder(R.drawable.ic_profile)
                                .into(binding.imgUserProfile);
                    }
                } else {
                    if (getContext() != null) {
                        Glide.with(getContext())
                                .load(R.drawable.ic_profile)
                                .into(binding.imgUserProfile);
                    }
                }
            }
        });

        // Quan sát thông báo (Toast)
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // (Có thể reset message trong ViewModel nếu cần)
            }
        });

        // 3. KHỞI CHẠY ACTIVITY LAUNCHER (Giữ nguyên)
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && binding != null && getContext() != null) {
                        // Khi có ảnh, đưa nó cho ViewModel xử lý
                        viewModel.uploadImageToStorage(uri);

                        // Cập nhật UI tạm thời
                        Glide.with(getContext())
                                .load(uri)
                                .into(binding.imgUserProfile);
                    }
                });

        // KHÔNG CẦN GỌI loadUserProfile() ở đây nữa
        // vì ViewModel tự gọi trong constructor của nó

        // 4. Xử lý nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            if (getContext() == null) {
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle("Xác nhận Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        logoutUser();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // 5. Xử lý nút Thay đổi ảnh
        binding.btnChangeProfilePic.setOnClickListener(v -> {
            mGetContent.launch("image/*");
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