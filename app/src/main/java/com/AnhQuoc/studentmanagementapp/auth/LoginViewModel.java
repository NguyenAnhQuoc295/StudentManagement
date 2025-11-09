package com.AnhQuoc.studentmanagementapp.auth;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData cho trạng thái đăng nhập
    private MutableLiveData<LoginResult> loginResultLiveData = new MutableLiveData<>();
    // LiveData cho trạng thái loading
    private MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    public LiveData<LoginResult> getLoginResultLiveData() {
        return loginResultLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public void loginUserWithFirebase(String email, String password) {
        isLoadingLiveData.setValue(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        getUserRoleAndProceed(user.getUid());
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        loginResultLiveData.setValue(new LoginResult(LoginResult.Status.ERROR, "Đăng nhập thất bại: " + task.getException().getMessage()));
                        isLoadingLiveData.setValue(false);
                    }
                });
    }

    private void getUserRoleAndProceed(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userRole = document.getString("role");
                            String userStatus = document.getString("status");

                            if (userRole == null) {
                                loginResultLiveData.setValue(new LoginResult(LoginResult.Status.ERROR, "Không tìm thấy vai trò."));
                                mAuth.signOut();
                            } else if ("Locked".equals(userStatus)) {
                                loginResultLiveData.setValue(new LoginResult(LoginResult.Status.LOCKED, "Tài khoản của bạn đã bị khóa."));
                                mAuth.signOut();
                            } else {
                                recordLoginHistory(uid);
                                loginResultLiveData.setValue(new LoginResult(LoginResult.Status.SUCCESS, userRole));
                            }
                        } else {
                            loginResultLiveData.setValue(new LoginResult(LoginResult.Status.ERROR, "Không tìm thấy thông tin (database) của người dùng."));
                            mAuth.signOut();
                        }
                    } else {
                        loginResultLiveData.setValue(new LoginResult(LoginResult.Status.ERROR, "Lỗi khi lấy thông tin vai trò người dùng."));
                        mAuth.signOut();
                    }
                    isLoadingLiveData.setValue(false);
                });
    }

    private void recordLoginHistory(String uid) {
        LoginHistory historyEntry = new LoginHistory(null, "Android");
        db.collection("users").document(uid)
                .collection("login_history")
                .add(historyEntry)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Login history recorded."))
                .addOnFailureListener(e -> Log.w(TAG, "Error recording login history.", e));
    }
}