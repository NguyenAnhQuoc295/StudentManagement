package com.AnhQuoc.studentmanagementapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class LoginHistory {

    private String ipAddress; // (Tùy chọn, nâng cao)
    private String deviceType; // Ví dụ: "Android"
    @ServerTimestamp // Tự động lấy giờ của server
    private Date timestamp;

    // Constructor rỗng cho Firestore
    public LoginHistory() {
    }

    // Constructor để tạo đối tượng
    public LoginHistory(String ipAddress, String deviceType) {
        this.ipAddress = ipAddress;
        this.deviceType = deviceType;
        // timestamp sẽ được gán tự động bởi server
    }

    // --- Getters ---
    public String getIpAddress() {
        return ipAddress;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    // --- Setters ---
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}