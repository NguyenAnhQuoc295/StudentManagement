package com.AnhQuoc.studentmanagementapp;

import android.os.Bundle;
import android.view.Menu; // <-- THÊM IMPORT NÀY
import android.view.MenuItem;
import android.view.View; // <-- THÊM IMPORT NÀY

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

// Import các file binding và fragment
import com.AnhQuoc.studentmanagementapp.databinding.ActivityMainBinding;
import com.AnhQuoc.studentmanagementapp.profile.ProfileFragment;
import com.AnhQuoc.studentmanagementapp.student.StudentListFragment;
import com.AnhQuoc.studentmanagementapp.user.UserListFragment;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String currentUserRole; // <-- THÊM BIẾN ĐỂ LƯU VAI TRÒ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Nhận vai trò từ LoginActivity
        currentUserRole = getIntent().getStringExtra("USER_ROLE");
        if (currentUserRole == null) {
            // Xử lý lỗi nếu không có vai trò (mặc định là Employee)
            currentUserRole = "Employee";
        }

        // 2. Tải Fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(new StudentListFragment(), true);
        }

        // 3. THỰC THI PHÂN QUYỀN: Ẩn/hiện tab "Người dùng"
        setupRoleBasedUI();

        // 4. Xử lý logic khi nhấn vào các tab
        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_students) {
                    loadFragment(new StudentListFragment(), false);
                    return true;
                } else if (itemId == R.id.nav_users) {
                    // (Đã được kiểm tra bằng cách ẩn/hiện)
                    loadFragment(new UserListFragment(), false);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    loadFragment(new ProfileFragment(), false);
                    return true;
                }
                return false;
            }
        });
    }

    private void setupRoleBasedUI() {
        Menu navMenu = binding.bottomNavigation.getMenu();
        MenuItem userTab = navMenu.findItem(R.id.nav_users);

        if ("Admin".equals(currentUserRole)) {
            // Nếu là Admin, cho phép tất cả
            userTab.setVisible(true);
        } else if ("Manager".equals(currentUserRole)) {
            // Nếu là Manager, ẩn tab "Người dùng"
            userTab.setVisible(false);
        } else {
            // Nếu là Employee, ẩn tab "Người dùng"
            userTab.setVisible(false);
        }
    }

    private void loadFragment(Fragment fragment, boolean isFirstTime) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // (Nâng cao: Gửi vai trò vào Fragment để phân quyền sâu hơn)
        Bundle bundle = new Bundle();
        bundle.putString("USER_ROLE", currentUserRole);
        fragment.setArguments(bundle);

        transaction.replace(R.id.fragment_container, fragment);

        if (!isFirstTime) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}