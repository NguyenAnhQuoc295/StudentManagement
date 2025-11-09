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
import androidx.lifecycle.ViewModelProvider;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.auth.LoginActivity;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentProfileBinding; // <-- THÊM DÒNG NÀY ĐỂ SỬA LỖI
import com.AnhQuoc.studentmanagementapp.model.User;
import com.bumptech.glide.Glide;
// import com.google.firebase.auth.FirebaseAuth; // Đã chuyển sang ViewModel
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding; // Dòng này sẽ hết báo lỗi
    private String currentUserRole;
    private ProfileViewModel viewModel;
    private ActivityResultLauncher<String> mGetContent;

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

        // 1. LẤY VIEWMODEL (Hilt sẽ tự cung cấp)
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // 2. QUAN SÁT (OBSERVE)
        viewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvProfileName.setText("Tên: " + user.getName());
                binding.tvProfileRole.setText("Vai trò: " + user.getRole());

                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
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

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // 3. KHỞI CHẠY ACTIVITY LAUNCHER
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
        viewModel.logout(); // Yêu cầu ViewModel đăng xuất
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