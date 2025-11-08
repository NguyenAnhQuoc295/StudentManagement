package com.AnhQuoc.studentmanagementapp.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.AnhQuoc.studentmanagementapp.databinding.ItemStudentBinding;
import com.AnhQuoc.studentmanagementapp.model.Student;
// import com.bumptech.glide.Glide;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private OnItemClickListener listener;
    // private String userRole; // <-- KHÔNG CẦN NỮA, chúng ta sẽ ẩn nó trong Fragment

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    // Cập nhật Constructor (không cần vai trò ở đây nữa)
    public StudentAdapter(List<Student> studentList, OnItemClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
        // this.userRole = userRole; // <-- KHÔNG CẦN
    }

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

        // Gán sự kiện Click cho cả item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(student);
            }
        });

        // XỬ LÝ PHÂN QUYỀN
        // (Tạm thời chúng ta sẽ ẩn nút này cho tất cả,
        // vì logic xóa nên nằm trong StudentDetailsActivity để nhất quán)
        holder.binding.btnDeleteItem.setVisibility(View.GONE);

        // (Nếu bạn muốn Admin xóa ngay từ danh sách, bạn sẽ phải truyền
        // userRole vào Adapter và hiện/ẩn nút này)

        // if ("Admin".equals(userRole)) {
        //     holder.binding.btnDeleteItem.setVisibility(View.VISIBLE);
        //     holder.binding.btnDeleteItem.setOnClickListener(v -> {
        //         // Xử lý logic xóa nhanh
        //     });
        // } else {
        //     holder.binding.btnDeleteItem.setVisibility(View.GONE);
        // }
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