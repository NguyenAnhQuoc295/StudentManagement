package com.AnhQuoc.studentmanagementapp.user;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.AnhQuoc.studentmanagementapp.databinding.ActivityLoginHistoryBinding;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryActivity extends AppCompatActivity {

    private ActivityLoginHistoryBinding binding;
    private FirebaseFirestore db;
    private LoginHistoryAdapter adapter;
    private List<LoginHistory> historyList;
    private String userIdToView;
    private static final String TAG = "LoginHistoryActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        historyList = new ArrayList<>();

        // Lấy User ID từ Intent
        if (getIntent().hasExtra("USER_ID_TO_VIEW")) {
            userIdToView = getIntent().getStringExtra("USER_ID_TO_VIEW");
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cài đặt Toolbar
        binding.toolbarLoginHistory.setNavigationOnClickListener(v -> finish());
        // (Tùy chọn: Bạn có thể set tiêu đề bằng tên user nếu muốn)

        // Cài đặt RecyclerView
        setupRecyclerView();

        // Tải dữ liệu
        loadLoginHistory();
    }

    private void setupRecyclerView() {
        adapter = new LoginHistoryAdapter(historyList);
        binding.rvLoginHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLoginHistory.setAdapter(adapter);
    }

    private void loadLoginHistory() {
        if (userIdToView == null) return;

        db.collection("users").document(userIdToView)
                .collection("login_history")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .limit(50) // Giới hạn 50 bản ghi
                .get()
                .addOnCompleteListener(task -> {
                    // === SỬA LỖI: Kiểm tra Activity có còn hoạt động không ===
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    // =======================================================

                    if (task.isSuccessful()) {
                        historyList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LoginHistory history = document.toObject(LoginHistory.class);
                            historyList.add(history);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error getting login history.", task.getException());
                        Toast.makeText(this, "Lỗi khi tải lịch sử", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}