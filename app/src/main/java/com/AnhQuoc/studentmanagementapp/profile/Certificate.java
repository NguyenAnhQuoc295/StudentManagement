package com.AnhQuoc.studentmanagementapp.model;

import com.google.firebase.firestore.Exclude;

public class Certificate {

    @Exclude // Bỏ qua, không lưu trường này LÊN Firestore
    private String certificateId; // ID của chứng chỉ

    private String name; // Tên chứng chỉ
    private String dateIssued; // Ngày cấp

    // Constructor rỗng (Bắt buộc cho Firestore)
    public Certificate() {
    }

    public Certificate(String name, String dateIssued) {
        this.name = name;
        this.dateIssued = dateIssued;
    }

    // --- Getters ---
    public String getCertificateId() {
        return certificateId;
    }

    public String getName() {
        return name;
    }

    public String getDateIssued() {
        return dateIssued;
    }

    // --- Setters ---
    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateIssued(String dateIssued) {
        this.dateIssued = dateIssued;
    }
}