package com.AnhQuoc.studentmanagementapp.auth;

// Class này dùng để đóng gói kết quả đăng nhập cho LiveData
public class LoginResult {

    // Trạng thái: SUCCESS, ERROR, LOCKED
    public enum Status {
        SUCCESS,
        ERROR,
        LOCKED
    }

    private final Status status;
    private final String data; // Sẽ là "role" nếu SUCCESS, "message" nếu ERROR/LOCKED

    public LoginResult(Status status, String data) {
        this.status = status;
        this.data = data;
    }

    public Status getStatus() {
        return status;
    }

    public String getData() {
        return data;
    }
}