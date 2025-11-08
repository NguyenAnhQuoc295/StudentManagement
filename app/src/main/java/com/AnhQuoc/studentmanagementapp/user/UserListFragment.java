package com.AnhQuoc.studentmanagementapp.user;

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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentUserListBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserListFragment extends Fragment {

    private FragmentUserListBinding binding;
    private UserAdapter userAdapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private static final String TAG = "UserListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo
        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();

        // 2. Setup Adapter (với Listener)
        userAdapter = new UserAdapter(userList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // Chuyển sang màn hình AddEditUserActivity (chế độ Sửa)
                Intent intent = new Intent(getActivity(), AddEditUserActivity.class);
                intent.putExtra("USER_ID_TO_EDIT", user.getUserId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(User user) {
                // TODO: Chỉ cho phép Admin xóa. Tạm thời cho phép xóa.
                new AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận Xóa")
                        .setMessage("Bạn có chắc muốn xóa " + user.getName() + "?\n(Hành động này không thể hoàn tác)")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteUserFromFirestore(user.getUserId());
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(R.drawable.ic_delete)
                        .show();
            }
        });

        // 3. Cài đặt RecyclerView
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUsers.setAdapter(userAdapter);

        // 4. Xử lý sự kiện nhấn nút "+"
        binding.fabAddUser.setOnClickListener(v -> {
            // Chuyển sang màn hình AddEditUserActivity (chế độ Thêm)
            Intent intent = new Intent(getActivity(), AddEditUserActivity.class);
            startActivity(intent);
        });

        // 5. Tải dữ liệu ban đầu
        loadUsersFromFirestore(""); // Tải tất cả khi query rỗng

        // 6. Xử lý thanh tìm kiếm
        binding.searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadUsersFromFirestore(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                loadUsersFromFirestore(newText);
                return true;
            }
        });
    }

    private void loadUsersFromFirestore(String searchQuery) {
        Query query = db.collection("users");

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = query.orderBy("name")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        query.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId()); // Gán ID của document
                            userList.add(user);
                        }
                        userAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(getContext(), "Lỗi khi tải danh sách người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Thêm hàm xóa User
    private void deleteUserFromFirestore(String userId) {
        if (userId == null || userId.isEmpty()) return;

        // TODO: Nên xóa cả tài khoản trong Firebase Auth

        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xóa người dùng thành công", Toast.LENGTH_SHORT).show();
                    loadUsersFromFirestore(binding.searchViewUsers.getQuery().toString()); // Tải lại danh sách
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu khi quay lại màn hình này
        String currentQuery = binding.searchViewUsers.getQuery().toString();
        loadUsersFromFirestore(currentQuery);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak
    }
}