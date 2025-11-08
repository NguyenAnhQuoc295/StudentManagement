package com.AnhQuoc.studentmanagementapp.model;

import com.google.firebase.firestore.Exclude;

public class User {

    @Exclude
    private String userId; // ID của document trên Firestore
    private String name;
    private int age;
    private String phone;
    private String status; // Ví dụ: "Normal" hoặc "Locked"
    private String role;   // Ví dụ: "Admin", "Manager", "Employee"
    private String profileImageUrl; // <-- THÊM TRƯỜNG MỚI

    // Constructor rỗng (bắt buộc cho Firestore)
    public User() {
    }

    // Constructor để tạo đối tượng
    public User(String name, int age, String phone, String status, String role) {
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.status = status;
        this.role = role;
    }

    // --- Getters ---
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    // Getter mới
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    // --- Setters ---
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Setter mới
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}