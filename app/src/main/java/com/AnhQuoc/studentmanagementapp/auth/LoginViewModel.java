// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.data.UserRepository; // <-- THÊM
import javax.inject.Inject; // <-- THÊM
import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private UserRepository userRepository;
    private LiveData<LoginResult> loginResultLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false); // Giữ lại isLoading ở đây

    @Inject
    public LoginViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.loginResultLiveData = userRepository.getLoginResultLiveData();
    }

    public LiveData<LoginResult> getLoginResultLiveData() {
        return loginResultLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public void loginUserWithFirebase(String email, String password) {
        isLoadingLiveData.setValue(true);
        // Quan sát kết quả để tắt loading
        loginResultLiveData.observeForever(result -> isLoadingLiveData.setValue(false));
        userRepository.loginUser(email, password);
    }
}