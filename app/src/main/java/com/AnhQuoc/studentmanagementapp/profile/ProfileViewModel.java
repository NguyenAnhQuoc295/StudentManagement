// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.profile;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.data.UserRepository; // <-- THÊM
import com.AnhQuoc.studentmanagementapp.model.User;
import javax.inject.Inject; // <-- THÊM
import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private UserRepository userRepository;
    private LiveData<User> userLiveData;
    private LiveData<String> toastMessage;

    @Inject
    public ProfileViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userLiveData = userRepository.getUserProfileLiveData();
        this.toastMessage = userRepository.getToastMessage();
        // Bắt đầu lắng nghe khi ViewModel được tạo
        userRepository.subscribeToUserProfile();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void uploadImageToStorage(Uri selectedImageUri) {
        userRepository.uploadProfileImage(selectedImageUri);
    }

    public void logout() {
        userRepository.logoutUser();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Hủy lắng nghe
        userRepository.clearUserProfileListener();
    }
}