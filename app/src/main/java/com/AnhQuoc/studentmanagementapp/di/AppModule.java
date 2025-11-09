// TỆP ĐƯỢC CẬP NHẬT
package com.AnhQuoc.studentmanagementapp.di;

import com.AnhQuoc.studentmanagementapp.data.StudentRepository; // <-- THÊM
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage; // <-- THÊM

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public FirebaseFirestore provideFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseAuth provideAuth() {
        return FirebaseAuth.getInstance();
    }

    // --- THÊM CÁC PROVIDER SAU ---

    @Provides
    @Singleton
    public FirebaseStorage provideStorage() {
        // Cung cấp Firebase Storage
        return FirebaseStorage.getInstance();
    }

    @Provides
    @Singleton
    public StudentRepository provideStudentRepository(FirebaseFirestore db) {
        // Cung cấp StudentRepository (để các ViewModel khác dùng)
        return new StudentRepository(db);
    }
}