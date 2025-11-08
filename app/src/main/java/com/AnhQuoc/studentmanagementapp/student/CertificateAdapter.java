package com.AnhQuoc.studentmanagementapp.student;

import android.view.LayoutInflater;
import android.view.View; // Đảm bảo đã import
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.AnhQuoc.studentmanagementapp.databinding.ItemCertificateBinding;
import com.AnhQuoc.studentmanagementapp.model.Certificate;
import java.util.List;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private List<Certificate> certificateList;

    // === LỖI CỦA BẠN LÀ BẠN ĐÃ BỎ SÓT DÒNG NÀY ===
    private OnCertificateClickListener listener;
    // ==========================================

    // Interface để xử lý click
    public interface OnCertificateClickListener {
        void onEditClick(Certificate certificate); // Khi nhấn nút Sửa
        void onDeleteClick(Certificate certificate); // Khi nhấn nút Xóa
    }

    // Constructor
    public CertificateAdapter(List<Certificate> certificateList, OnCertificateClickListener listener) {
        this.certificateList = certificateList;
        this.listener = listener; // Dòng này bây giờ sẽ hết báo lỗi
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

        // Gán sự kiện cho 2 nút
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