package com.AnhQuoc.studentmanagementapp.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.AnhQuoc.studentmanagementapp.databinding.ItemCertificateBinding;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import java.util.List;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private List<Certificate> certificateList;
    private OnCertificateClickListener listener;
    private String userRole; // <-- THÊM BIẾN LƯU VAI TRÒ

    public interface OnCertificateClickListener {
        void onEditClick(Certificate certificate);
        void onDeleteClick(Certificate certificate);
    }

    // Cập nhật Constructor
    public CertificateAdapter(List<Certificate> certificateList, OnCertificateClickListener listener, String userRole) {
        this.certificateList = certificateList;
        this.listener = listener;
        this.userRole = userRole; // <-- NHẬN VAI TRÒ
    }

    @NonNull
    @Override
    public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CertificateViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
        Certificate certificate = certificateList.get(position);
        if (certificate == null) return;

        holder.binding.tvCertificateName.setText(certificate.getName());
        holder.binding.tvCertificateDate.setText("Ngày cấp: " + certificate.getDateIssued());

        // === THỰC THI PHÂN QUYỀN ===
        if ("Employee".equals(userRole)) {
            holder.binding.btnEditCertificate.setVisibility(View.GONE);
            holder.binding.btnDeleteCertificate.setVisibility(View.GONE);
        } else {
            // (Admin/Manager) Hiện các nút
            holder.binding.btnEditCertificate.setVisibility(View.VISIBLE);
            holder.binding.btnDeleteCertificate.setVisibility(View.VISIBLE);

            // Gán sự kiện (chỉ gán nếu nút hiện)
            holder.binding.btnEditCertificate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(certificate);
                }
            });

            holder.binding.btnDeleteCertificate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(certificate);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return certificateList != null ? certificateList.size() : 0;
    }

    public static class CertificateViewHolder extends RecyclerView.ViewHolder {
        public ItemCertificateBinding binding;
        public CertificateViewHolder(ItemCertificateBinding itemCertificateBinding) {
            super(itemCertificateBinding.getRoot());
            this.binding = itemCertificateBinding;
        }
    }
}