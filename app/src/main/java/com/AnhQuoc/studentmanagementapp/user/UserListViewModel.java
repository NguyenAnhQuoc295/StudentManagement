package com.AnhQuoc.studentmanagementapp.user;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserListViewModel extends ViewModel {

    private static final String TAG = "UserListViewModel";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MutableLiveData<List<User>> userListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<List<User>> getUserListLiveData() {
        return userListLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadUsersFromFirestore(String searchQuery) {
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
                        List<User> userList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId());
                            userList.add(user);
                        }
                        userListLiveData.setValue(userList); // Gửi danh sách
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        toastMessage.setValue("Lỗi khi tải danh sách người dùng");
                    }
                });
    }

    public void deleteUserFromFirestore(String userId, String currentSearchQuery) {
        if (userId == null || userId.isEmpty()) return;

        // TODO: Nên xóa cả tài khoản trong Firebase Auth

        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Xóa người dùng thành công");
                    // Tải lại danh sách sau khi xóa
                    loadUsersFromFirestore(currentSearchQuery);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("Lỗi khi xóa: " + e.getMessage());
                });
    }
}