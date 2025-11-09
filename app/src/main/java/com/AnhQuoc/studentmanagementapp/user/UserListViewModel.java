// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.user;

// import android.util.Log; // Không cần nữa

import androidx.lifecycle.LiveData;
// import androidx.lifecycle.MutableLiveData; // Sẽ lấy từ Repo
import androidx.lifecycle.ViewModel;

import com.AnhQuoc.studentmanagementapp.data.UserRepository; // <-- THÊM
import com.AnhQuoc.studentmanagementapp.model.User;
// import com.google.firebase.firestore.FirebaseFirestore; // <-- XÓA
// import com.google.firebase.firestore.Query; // <-- XÓA
// import com.google.firebase.firestore.QueryDocumentSnapshot; // <-- XÓA

// import java.util.ArrayList; // <-- XÓA
import java.util.List;

import javax.inject.Inject; // <-- THÊM
import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM

@HiltViewModel
public class UserListViewModel extends ViewModel {

    // private static final String TAG = "UserListViewModel"; // <-- XÓA
    // private FirebaseFirestore db = FirebaseFirestore.getInstance(); // <-- XÓA
    private UserRepository userRepository; // <-- THÊM

    private LiveData<List<User>> userListLiveData; // <-- SỬA
    private LiveData<String> toastMessage; // <-- SỬA

    @Inject // <-- THÊM
    public UserListViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userListLiveData = userRepository.getUserListLiveData();
        this.toastMessage = userRepository.getToastMessage();
    }

    public LiveData<List<User>> getUserListLiveData() {
        return userListLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    // Đổi tên hàm
    public void subscribeToUserUpdates(String searchQuery) {
        userRepository.subscribeToUserUpdates(searchQuery);
    }

    public void deleteUserFromFirestore(String userId) {
        if (userId == null || userId.isEmpty()) return;
        userRepository.deleteUserFromFirestore(userId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Hủy lắng nghe
        userRepository.clearUserListListener();
    }
}