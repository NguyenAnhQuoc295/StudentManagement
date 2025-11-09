package com.AnhQuoc.studentmanagementapp.user;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryViewModel extends ViewModel {

    private static final String TAG = "LoginHistoryViewModel";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MutableLiveData<List<LoginHistory>> historyListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<List<LoginHistory>> getHistoryListLiveData() {
        return historyListLiveData;
    }
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadLoginHistory(String userIdToView) {
        if (userIdToView == null) {
            toastMessage.setValue("Lỗi: Không tìm thấy ID người dùng");
            return;
        }

        db.collection("users").document(userIdToView)
                .collection("login_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<LoginHistory> historyList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LoginHistory history = document.toObject(LoginHistory.class);
                            historyList.add(history);
                        }
                        historyListLiveData.setValue(historyList);
                    } else {
                        Log.w(TAG, "Error getting login history.", task.getException());
                        toastMessage.setValue("Lỗi khi tải lịch sử");
                    }
                });
    }
}