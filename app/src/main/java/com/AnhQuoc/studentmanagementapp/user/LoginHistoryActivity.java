package com.AnhQuoc.studentmanagementapp.user;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityLoginHistoryBinding; // <-- THÊM DÒNG NÀY ĐỂ SỬA LỖI
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Đã thêm Hilt
public class LoginHistoryActivity extends AppCompatActivity {

    private ActivityLoginHistoryBinding binding; // Dòng này sẽ hết báo lỗi
    private LoginHistoryViewModel viewModel;
    private LoginHistoryAdapter adapter;
    private List<LoginHistory> historyList;
    private String userIdToView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Khởi tạo
        historyList = new ArrayList<>();
        viewModel = new ViewModelProvider(this).get(LoginHistoryViewModel.class);

        // 2. Lấy User ID
        if (getIntent().hasExtra("USER_ID_TO_VIEW")) {
            userIdToView = getIntent().getStringExtra("USER_ID_TO_VIEW");
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Cài đặt Toolbar
        binding.toolbarLoginHistory.setNavigationOnClickListener(v -> finish());

        // 4. Cài đặt RecyclerView
        setupRecyclerView();

        // 5. QUAN SÁT (OBSERVE) DỮ LIỆU
        viewModel.getHistoryListLiveData().observe(this, updatedList -> {
            historyList.clear();
            historyList.addAll(updatedList);
            adapter.notifyDataSetChanged();
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 6. Tải dữ liệu
        viewModel.loadLoginHistory(userIdToView);
    }

    private void setupRecyclerView() {
        adapter = new LoginHistoryAdapter(historyList);
        binding.rvLoginHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLoginHistory.setAdapter(adapter);
    }
}