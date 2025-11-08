package com.AnhQuoc.studentmanagementapp.user;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.AnhQuoc.studentmanagementapp.databinding.ItemUserBinding;
import com.AnhQuoc.studentmanagementapp.model.User;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onDeleteClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) {
            return;
        }

        // === THAY ĐỔI QUAN TRỌNG: KIỂM TRA NULL ===
        // Nếu trường 'name' không có (null), hiển thị "Lỗi Tên"
        if (user.getName() != null) {
            holder.binding.tvUserNameItem.setText(user.getName());
        } else {
            holder.binding.tvUserNameItem.setText("[Lỗi Tên]");
        }

        // Nếu trường 'role' không có (null), hiển thị "Không rõ"
        if (user.getRole() != null) {
            holder.binding.tvUserRoleItem.setText("Vai trò: " + user.getRole());
        } else {
            holder.binding.tvUserRoleItem.setText("Vai trò: [Không rõ]");
        }

        // Nếu trường 'status' không có (null), hiển thị "Không rõ"
        if (user.getStatus() != null) {
            holder.binding.tvUserStatusItem.setText("Trạng thái: " + user.getStatus());
        } else {
            holder.binding.tvUserStatusItem.setText("Trạng thái: [Không rõ]");
        }
        // ===========================================

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });

        holder.binding.btnDeleteUser.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        public ItemUserBinding binding;

        public UserViewHolder(ItemUserBinding itemUserBinding) {
            super(itemUserBinding.getRoot());
            this.binding = itemUserBinding;
        }
    }
}