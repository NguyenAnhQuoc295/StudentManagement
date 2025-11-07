package com.AnhQuoc.studentmanagementapp;

import android.os.Bundle;
import android.view.MenuItem;

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

    // Khai báo biến binding
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo và thiết lập View Binding
        // Dòng code này thay thế cho code cũ của bạn
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Tải Fragment mặc định (StudentListFragment) khi app vừa mở
        if (savedInstanceState == null) {
            loadFragment(new StudentListFragment(), true);
        }

        // 2. Xử lý logic khi nhấn vào các tab
        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.nav_students) {
                    loadFragment(new StudentListFragment(), false);
                    return true;
                } else if (itemId == R.id.nav_users) {
                    // TODO: Sau này sẽ kiểm tra, nếu là ADMIN mới cho vào
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

    // 3. Hàm trợ giúp để tải (thay thế) các Fragment
    private void loadFragment(Fragment fragment, boolean isFirstTime) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Thay thế nội dung của 'fragment_container' bằng Fragment mới
        transaction.replace(R.id.fragment_container, fragment);

        if (!isFirstTime) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}