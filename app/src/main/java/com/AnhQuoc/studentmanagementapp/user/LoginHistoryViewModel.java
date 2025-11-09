// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.data.UserRepository; // <-- THÊM
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import java.util.List;
import javax.inject.Inject; // <-- THÊM
import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM

@HiltViewModel
public class LoginHistoryViewModel extends ViewModel {

    private UserRepository userRepository;
    private LiveData<List<LoginHistory>> historyListLiveData;
    private LiveData<String> toastMessage;

    @Inject
    public LoginHistoryViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.historyListLiveData = userRepository.getHistoryListLiveData();
        this.toastMessage = userRepository.getToastMessage();
    }

    public LiveData<List<LoginHistory>> getHistoryListLiveData() {
        return historyListLiveData;
    }
    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadLoginHistory(String userIdToView) {
        userRepository.loadLoginHistory(userIdToView);
    }
}