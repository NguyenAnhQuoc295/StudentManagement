package com.AnhQuoc.studentmanagementapp.data;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.AnhQuoc.studentmanagementapp.auth.LoginResult;
import com.AnhQuoc.studentmanagementapp.model.LoginHistory;
import com.AnhQuoc.studentmanagementapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserRepository {

    private static final String TAG = "UserRepository";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    // Listener cho User List
    private ListenerRegistration userListenerRegistration;
    private MutableLiveData<List<User>> userListLiveData = new MutableLiveData<>();

    // Listener cho User Profile
    private ListenerRegistration userProfileListener;
    private MutableLiveData<User> userProfileLiveData = new MutableLiveData<>();

    // LiveData cho các kết quả khác
    private MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResultLiveData = new MutableLiveData<>();
    private MutableLiveData<List<LoginHistory>> historyListLiveData = new MutableLiveData<>();
    private MutableLiveData<User> singleUserLiveData = new MutableLiveData<>(); // Dùng cho AddEditUser

    @Inject
    public UserRepository(FirebaseFirestore firestore, FirebaseAuth auth, FirebaseStorage storage) {
        this.db = firestore;
        this.mAuth = auth;
        this.storageRef = storage.getReference();
    }

    // --- Getters cho LiveData ---
    public LiveData<List<User>> getUserListLiveData() { return userListLiveData; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<LoginResult> getLoginResultLiveData() { return loginResultLiveData; }
    public LiveData<User> getUserProfileLiveData() { return userProfileLiveData; }
    public LiveData<List<LoginHistory>> getHistoryListLiveData() { return historyListLiveData; }
    public LiveData<User> getSingleUserLiveData() { return singleUserLiveData; }

    // --- UserListFragment Logic ---
    public void subscribeToUserUpdates(String searchQuery) {
        clearUserListListener(); // Xóa listener cũ
        Query query = db.collection("users");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = query.orderBy("name").startAt(searchQuery).endAt(searchQuery + "\uf8ff");
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        userListenerRegistration = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Lỗi khi lắng nghe User updates.", error);
                toastMessage.setValue("Lỗi khi tải danh sách người dùng");
                return;
            }
            if (value != null) {
                List<User> userList = new ArrayList<>();
                for (QueryDocumentSnapshot document : value) {
                    User user = document.toObject(User.class);
                    user.setUserId(document.getId());
                    userList.add(user);
                }
                userListLiveData.setValue(userList);
            }
        });
    }

    public void clearUserListListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    public void deleteUserFromFirestore(String userId) {
        if (userId == null || userId.isEmpty()) return;
        // TODO: Xóa cả Firebase Auth (cần Cloud Function)
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> toastMessage.setValue("Xóa người dùng thành công"))
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi xóa: " + e.getMessage()));
    }

    // --- LoginViewModel Logic ---
    public void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        getUserRoleAndProceed(user.getUid());
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        loginResultLiveData.setValue(new LoginResult(LoginResult.Status.ERROR, "Đăng nhập thất bại: " + task.getException().getMessage()));
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

    // --- ProfileViewModel Logic ---
    public void subscribeToUserProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            return;
        }
        String uid = firebaseUser.getUid();

        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe Profile update", error);
                        toastMessage.setValue("Lỗi khi tải hồ sơ: " + error.getMessage());
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        userProfileLiveData.setValue(user);
                    } else {
                        Log.w(TAG, "Không tìm thấy document cho UID: " + uid);
                        toastMessage.setValue("Lỗi: Không tìm thấy dữ liệu người dùng.");
                    }
                });
    }

    public void clearUserProfileListener() {
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
    }

    public void uploadProfileImage(Uri selectedImageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || selectedImageUri == null) return;

        toastMessage.setValue("Đang tải ảnh lên...");
        String uid = user.getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + uid + ".jpg");

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> updateProfileImageUrlInFirestore(uri.toString())))
                .addOnFailureListener(e -> toastMessage.setValue("Tải ảnh thất bại: " + e.getMessage()));
    }

    private void updateProfileImageUrlInFirestore(String downloadUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("profileImageUrl", downloadUrl)
                .addOnSuccessListener(aVoid -> toastMessage.setValue("Cập nhật ảnh đại diện thành công!"))
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi lưu URL: " + e.getMessage()));
    }

    public void logoutUser() {
        mAuth.signOut();
    }

    // --- LoginHistoryViewModel Logic ---
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
                            historyList.add(document.toObject(LoginHistory.class));
                        }
                        historyListLiveData.setValue(historyList);
                    } else {
                        Log.w(TAG, "Error getting login history.", task.getException());
                        toastMessage.setValue("Lỗi khi tải lịch sử");
                    }
                });
    }

    // --- AddEditUserViewModel Logic ---
    public void loadUserDataForEdit(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        singleUserLiveData.setValue(documentSnapshot.toObject(User.class));
                    } else {
                        toastMessage.setValue("Không tìm thấy người dùng");
                    }
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi tải dữ liệu"));
    }

    public void createUserInAuthAndFirestore(String email, String password, String ageStr, String phone, String role, String status, MutableLiveData<Boolean> saveSuccessEvent) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User newUser = new User(email, Integer.parseInt(ageStr), phone, status, role);

                        db.collection("users").document(uid)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    toastMessage.setValue("Thêm người dùng thành công!");
                                    saveSuccessEvent.setValue(true);
                                })
                                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi lưu vào Firestore: " + e.getMessage()));
                    } else {
                        toastMessage.setValue("Lỗi khi tạo tài khoản: " + task.getException().getMessage());
                    }
                });
    }

    public void updateUserInFirestore(String userIdToEdit, String newPassword, String ageStr, String phone, String role, String status, User currentUserData, MutableLiveData<Boolean> saveSuccessEvent) {
        currentUserData.setAge(Integer.parseInt(ageStr));
        currentUserData.setPhone(phone);
        currentUserData.setRole(role);
        currentUserData.setStatus(status);

        db.collection("users").document(userIdToEdit)
                .set(currentUserData)
                .addOnSuccessListener(aVoid -> {
                    if (newPassword != null && !newPassword.isEmpty()) {
                        updatePasswordInAuth(newPassword, saveSuccessEvent);
                        // DÒNG LỖI "AGAIN_PATH" ĐÃ BỊ XÓA KHỎI ĐÂY
                    } else {
                        toastMessage.setValue("Cập nhật thông tin thành công!");
                        saveSuccessEvent.setValue(true);
                    }
                })
                .addOnFailureListener(e -> toastMessage.setValue("Lỗi khi cập nhật Firestore: " + e.getMessage()));
    }

    private void updatePasswordInAuth(String newPassword, MutableLiveData<Boolean> saveSuccessEvent) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            toastMessage.setValue("Lỗi: Không tìm thấy người dùng hiện tại để đổi mật khẩu.");
            return;
        }
        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toastMessage.setValue("Cập nhật thông tin VÀ mật khẩu thành công!");
                    } else {
                        toastMessage.setValue("Cập nhật thông tin thành công, NHƯNG mật khẩu thất bại: " + task.getException().getMessage());
                    }
                    saveSuccessEvent.setValue(true);
                });
    }
}