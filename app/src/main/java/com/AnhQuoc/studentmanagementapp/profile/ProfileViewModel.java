package com.AnhQuoc.studentmanagementapp.profile;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";

    // Firebase instances
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    // Dùng MutableLiveData để giữ dữ liệu
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    // Dùng để thông báo (thành công/lỗi)
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // Fragment sẽ "quan sát" LiveData này (chỉ có thể đọc)
    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    // Constructor
    public ProfileViewModel() {
        loadUserProfile(); // Tải dữ liệu ngay khi ViewModel được tạo
    }

    // --- LOGIC ĐƯỢC DI CHUYỂN TỪ FRAGMENT SANG ĐÂY ---

    public void loadUserProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            // (Trong ViewModel, chúng ta không điều hướng,
            // chúng ta nên set một trạng thái "đã đăng xuất")
            return;
        }

        String uid = firebaseUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        // Gửi dữ liệu vào LiveData
                        userLiveData.setValue(user);
                    } else {
                        Log.w(TAG, "Không tìm thấy document cho UID: " + uid);
                        toastMessage.setValue("Lỗi: Không tìm thấy dữ liệu người dùng.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lỗi khi lấy thông tin user", e);
                    toastMessage.setValue("Lỗi khi tải hồ sơ: " + e.getMessage());
                });
    }

    public void uploadImageToStorage(Uri selectedImageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selectedImageUri == null) {
            return;
        }

        toastMessage.setValue("Đang tải ảnh lên...");

        String uid = user.getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateProfileImageUrlInFirestore(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("Tải ảnh thất bại: " + e.getMessage());
                });
    }

    private void updateProfileImageUrlInFirestore(String downloadUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("profileImageUrl", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("Cập nhật ảnh đại diện thành công!");
                    // Tải lại dữ liệu user để cập nhật ảnh
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("Lỗi khi lưu URL: " + e.getMessage());
                });
    }
}