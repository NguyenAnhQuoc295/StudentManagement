package com.AnhQuoc.studentmanagementapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEditUserViewModel extends ViewModel {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> saveSuccessEvent = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<User> getUserLiveData() { return userLiveData; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getSaveSuccessEvent() { return saveSuccessEvent; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        userLiveData.setValue(user);
                    } else {
                        toastMessage.setValue("Không tìm thấy người dùng");
                        saveSuccessEvent.setValue(true); // Đóng
                    }
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("Lỗi khi tải dữ liệu");
                    saveSuccessEvent.setValue(true); // Đóng
                });
    }

    public void createUserInAuthAndFirestore(String email, String password, String ageStr, String phone, String role, String status) {
        if (email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            toastMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        isLoading.setValue(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser.getUid();
                        int age = Integer.parseInt(ageStr);
                        User newUser = new User(email, age, phone, status, role);

                        db.collection("users").document(uid)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    isLoading.setValue(false);
                                    toastMessage.setValue("Thêm người dùng thành công!");
                                    saveSuccessEvent.setValue(true);
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.setValue(false);
                                    toastMessage.setValue("Lỗi khi lưu vào Firestore: " + e.getMessage());
                                    // (Nâng cao: nên xóa tài khoản Auth nếu lưu Firestore thất bại)
                                });

                    } else {
                        isLoading.setValue(false);
                        toastMessage.setValue("Lỗi khi tạo tài khoản: " + task.getException().getMessage());
                    }
                });
    }

    public void updateUserInFirestore(String userIdToEdit, String ageStr, String phone, String role, String status, User currentUserData) {
        if (ageStr.isEmpty() || phone.isEmpty()) {
            toastMessage.setValue("Vui lòng nhập đầy đủ thông tin (trừ mật khẩu)");
            return;
        }

        // TODO: Xử lý logic cập nhật mật khẩu

        isLoading.setValue(true);
        currentUserData.setAge(Integer.parseInt(ageStr));
        currentUserData.setPhone(phone);
        currentUserData.setRole(role);
        currentUserData.setStatus(status);

        db.collection("users").document(userIdToEdit)
                .set(currentUserData)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Cập nhật thành công!");
                    saveSuccessEvent.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Lỗi khi cập nhật: " + e.getMessage());
                });
    }
}