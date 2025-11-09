package com.AnhQuoc.studentmanagementapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData; // <-- THÊM IMPORT NÀY
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.data.UserRepository;
import com.AnhQuoc.studentmanagementapp.model.User;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditUserViewModel extends ViewModel {

    private UserRepository userRepository;
    private LiveData<User> userLiveData;

    // SỬA LỖI:
    // 1. Tạo một MutableLiveData cục bộ cho các toast của ViewModel này
    private MutableLiveData<String> _toastMessage = new MutableLiveData<>();

    // 2. Tạo một MediatorLiveData để gộp toast cục bộ VÀ toast từ repository
    private MediatorLiveData<String> toastMessageMediator = new MediatorLiveData<>();

    // (Các LiveData khác giữ nguyên)
    private MutableLiveData<Boolean> saveSuccessEvent = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    @Inject
    public AddEditUserViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userLiveData = userRepository.getSingleUserLiveData();

        // 3. Hợp nhất các nguồn toast
        // Nguồn 1: Lắng nghe toast cục bộ (_toastMessage)
        toastMessageMediator.addSource(_toastMessage, message -> toastMessageMediator.setValue(message));
        // Nguồn 2: Lắng nghe toast từ Repository (cho các hàm async)
        toastMessageMediator.addSource(userRepository.getToastMessage(), message -> toastMessageMediator.setValue(message));
    }

    // --- Getters ---
    public LiveData<User> getUserLiveData() { return userLiveData; }
    // 4. Getter giờ sẽ trả về Mediator, Activity/Fragment sẽ quan sát cái này
    public LiveData<String> getToastMessage() { return toastMessageMediator; }
    public LiveData<Boolean> getSaveSuccessEvent() { return saveSuccessEvent; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadUserData(String userId) {
        // Yêu cầu repo tải, repo sẽ post lỗi/thành công vào LiveData của nó
        userRepository.loadUserDataForEdit(userId);
    }

    public void createUserInAuthAndFirestore(String email, String password, String ageStr, String phone, String role, String status) {
        if (email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            // 5. SỬA LỖI: Giờ chúng ta post lên LiveData cục bộ
            _toastMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        isLoading.setValue(true);
        // Quan sát sự kiện thành công để tắt loading
        saveSuccessEvent.observeForever(success -> isLoading.setValue(false));

        // 6. Yêu cầu Repo làm việc (NÓ SẼ DÙNG TOAST NỘI BỘ CỦA NÓ)
        userRepository.createUserInAuthAndFirestore(email, password, ageStr, phone, role, status, saveSuccessEvent);
    }

    public void updateUserInFirestore(String userIdToEdit, String newPassword, String ageStr, String phone, String role, String status, User currentUserData) {
        if (ageStr.isEmpty() || phone.isEmpty()) {
            // 5. SỬA LỖI: Giờ chúng ta post lên LiveData cục bộ
            _toastMessage.setValue("Vui lòng nhập đầy đủ thông tin (trừ mật khẩu)");
            return;
        }
        isLoading.setValue(true);
        // Quan sát sự kiện thành công để tắt loading
        saveSuccessEvent.observeForever(success -> isLoading.setValue(false));

        // 6. Yêu cầu Repo làm việc (NÓ SẼ DÙNG TOAST NỘI BỘ CỦA NÓ)
        userRepository.updateUserInFirestore(userIdToEdit, newPassword, ageStr, phone, role, status, currentUserData, saveSuccessEvent);
    }
}