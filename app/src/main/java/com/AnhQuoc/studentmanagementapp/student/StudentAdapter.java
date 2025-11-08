package com.AnhQuoc.studentmanagementapp.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.AnhQuoc.studentmanagementapp.databinding.ItemStudentBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
// import com.bumptech.glide.Glide; // Bỏ comment khi bạn thêm logic ảnh

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;

    // === THAY ĐỔI 1: Thêm Interface ===
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }
    // ===================================

    // === THAY ĐỔI 2: Cập nhật Constructor ===
    public StudentAdapter(List<Student> studentList, OnItemClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }
    // =======================================

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        if (student == null) {
            return;
        }

        holder.binding.tvStudentNameItem.setText(student.getName());
        holder.binding.tvStudentInfoItem.setText(student.getAge() + " tuổi - " + student.getPhone());

        // === THAY ĐỔI 3: Gán sự kiện Click ===
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(student);
            }
        });
        // ===================================

        // (Bỏ comment khi bạn có URL ảnh)
        // Glide.with(holder.itemView.getContext())
        //         .load(student.getProfileImageUrl())
        //         .into(holder.binding.imgStudentItem);
    }

    @Override
    public int getItemCount() {
        if (studentList != null) {
            return studentList.size();
        }
        return 0;
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        public ItemStudentBinding binding;

        public StudentViewHolder(ItemStudentBinding itemStudentBinding) {
            super(itemStudentBinding.getRoot());
            this.binding = itemStudentBinding;
        }
    }
}