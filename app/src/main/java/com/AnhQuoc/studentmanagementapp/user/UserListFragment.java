package com.AnhQuoc.studentmanagementapp.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- THÊM IMPORT
import androidx.recyclerview.widget.LinearLayoutManager;

import com.AnhQuoc.studentmanagementapp.R;
import com.AnhQuoc.studentmanagementapp.databinding.FragmentUserListBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
// KHÔNG CẦN import FirebaseFirestore

import java.util.ArrayList;
import java.util.List;

public class UserListFragment extends Fragment {

    private FragmentUserListBinding binding;
    private UserAdapter userAdapter;
    private List<User> userList; // Vẫn cần list này cho Adapter

    private UserListViewModel viewModel; // <-- Biến ViewModel
    // private FirebaseFirestore db; // Không cần nữa

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
        // db = FirebaseFirestore.getInstance(); // Không cần
        userList = new ArrayList<>();
        viewModel = new ViewModelProvider(this).get(UserListViewModel.class);

        // 2. Setup Adapter (với Listener)
        userAdapter = new UserAdapter(userList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(getActivity(), AddEditUserActivity.class);
                intent.putExtra("USER_ID_TO_EDIT", user.getUserId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(User user) {
                if (getContext() == null) {
                    return;
                }
                new AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận Xóa")
                        .setMessage("Bạn có chắc muốn xóa " + user.getName() + "?\n(Hành động này không thể hoàn tác)")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Yêu cầu ViewModel xóa
                            String currentQuery = binding.searchViewUsers.getQuery().toString();
                            viewModel.deleteUserFromFirestore(user.getUserId(), currentQuery);
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(R.drawable.ic_delete)
                        .show();
            }

            @Override
            public void onHistoryClick(User user) {
                Intent intent = new Intent(getActivity(), LoginHistoryActivity.class);
                intent.putExtra("USER_ID_TO_VIEW", user.getUserId());
                startActivity(intent);
            }
        });

        // 3. Cài đặt RecyclerView
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUsers.setAdapter(userAdapter);

        // 4. QUAN SÁT (OBSERVE) DỮ LIỆU TỪ VIEWMODEL
        viewModel.getUserListLiveData().observe(getViewLifecycleOwner(), updatedUserList -> {
            // Khi LiveData thay đổi, cập nhật danh sách của Adapter
            userList.clear();
            userList.addAll(updatedUserList);
            userAdapter.notifyDataSetChanged();
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // 5. Xử lý sự kiện nhấn nút "+" (Giữ nguyên)
        binding.fabAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditUserActivity.class);
            startActivity(intent);
        });

        // 6. Tải dữ liệu ban đầu
        viewModel.loadUsersFromFirestore(""); // Tải tất cả khi query rỗng

        // 7. Xử lý thanh tìm kiếm
        binding.searchViewUsers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.loadUsersFromFirestore(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.loadUsersFromFirestore(newText);
                return true;
            }
        });
    }

    // KHÔNG CẦN CÁC HÀM loadUsersFromFirestore và deleteUserFromFirestore ở đây nữa

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu khi quay lại màn hình này
        if (binding != null) {
            String currentQuery = binding.searchViewUsers.getQuery().toString();
            viewModel.loadUsersFromFirestore(currentQuery);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak
    }
}