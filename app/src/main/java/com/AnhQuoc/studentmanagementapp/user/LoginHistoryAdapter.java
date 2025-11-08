package com.AnhQuoc.studentmanagementapp.user;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.AnhQuoc.studentmanagementapp.databinding.ItemLoginHistoryBinding;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LoginHistoryAdapter extends RecyclerView.Adapter<LoginHistoryAdapter.HistoryViewHolder> {

    private List<LoginHistory> historyList;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'lúc' hh:mm a", Locale.getDefault());

    public LoginHistoryAdapter(List<LoginHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoginHistoryBinding binding = ItemLoginHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        LoginHistory history = historyList.get(position);
        if (history == null) return;

        if (history.getTimestamp() != null) {
            holder.binding.tvLoginTimestamp.setText(sdf.format(history.getTimestamp()));
        } else {
            holder.binding.tvLoginTimestamp.setText("Đang chờ... ");
        }

        String ip = (history.getIpAddress() != null) ? history.getIpAddress() : "Không rõ";
        String device = (history.getDeviceType() != null) ? history.getDeviceType() : "Không rõ";
        holder.binding.tvLoginInfo.setText("IP: " + ip + " (" + device + ")");
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public ItemLoginHistoryBinding binding;

        public HistoryViewHolder(ItemLoginHistoryBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }
    }
}