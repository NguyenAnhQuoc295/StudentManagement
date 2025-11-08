package com.AnhQuoc.studentmanagementapp.model;

import com.google.firebase.firestore.Exclude;

public class Student {
    // Các trường dữ liệu (fields)
    @Exclude // Bỏ qua, không lưu trường này LÊN Firestore (vì nó đã là ID)
    private String studentId;
    private String name;
    private int age;
    private String phone;
    // (Sau này bạn có thể thêm các trường khác như: profileImageUrl, studentId...)

    // Quan trọng: Firestore cần một constructor rỗng
    public Student() {
    }

    // Constructor để tạo đối tượng nhanh
    public Student(String name, int age, String phone) {
        this.name = name;
        this.age = age;
        this.phone = phone;
    }

    // Getters (Bắt buộc để Firestore đọc dữ liệu)
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    // Setters (Tùy chọn nhưng nên có)
    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}