package com.AnhQuoc.studentmanagementapp.student;

import android.content.ContentResolver;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.AnhQuoc.studentmanagementapp.data.StudentRepository;
import com.AnhQuoc.studentmanagementapp.model.Student;
import com.google.firebase.firestore.Query;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel // Báo cho Hilt biết đây là một ViewModel
public class StudentListViewModel extends ViewModel {

    private StudentRepository studentRepository; // Sẽ được tiêm

    // LiveData từ Repository
    private LiveData<List<Student>> studentListLiveData;
    private LiveData<String> toastMessage;

    // LiveData mới cho việc Export
    private MutableLiveData<String> exportCsvData = new MutableLiveData<>();

    // Các biến trạng thái sắp xếp (Vẫn giữ ở ViewModel)
    private String currentSortField = "name";
    private Query.Direction currentSortDirection = Query.Direction.ASCENDING;

    // --- CONSTRUCTOR MỚI (DÙNG @Inject) ---
    @Inject
    public StudentListViewModel(StudentRepository studentRepository) {
        this.studentRepository = studentRepository; // Nhận Repository từ Hilt
        this.studentListLiveData = studentRepository.getStudentListLiveData();
        this.toastMessage = studentRepository.getToastMessage();
    }
    // ------------------------------------

    public LiveData<List<Student>> getStudentListLiveData() {
        return studentListLiveData;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<String> getExportCsvData() {
        return exportCsvData;
    }

    public void clearExportData() {
        exportCsvData.setValue(null);
    }


    public void setSortOrder(String field, Query.Direction direction) {
        currentSortField = field;
        currentSortDirection = direction;
    }

    public String getCurrentSortField() {
        return currentSortField;
    }

    public Query.Direction getCurrentSortDirection() {
        return currentSortDirection;
    }

    /**
     * Yêu cầu Repository đăng ký lắng nghe các thay đổi
     */
    public void subscribeToStudentUpdates(String searchQuery) {
        studentRepository.subscribeToStudentUpdates(searchQuery, currentSortField, currentSortDirection);
    }

    /**
     * Hủy đăng ký lắng nghe khi ViewModel bị hủy
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        studentRepository.clearStudentListener();
    }

    // === CHỨC NĂNG EXPORT CSV ===
    public void exportStudentsToCsv() {
        List<Student> currentList = studentListLiveData.getValue();
        String csvContent = studentRepository.exportStudentsToCsv(currentList);
        if (csvContent != null) {
            exportCsvData.setValue(csvContent);
        }
    }

    // === CHỨC NĂNG IMPORT CSV ===
    public void importStudentsFromCsv(Uri uri, ContentResolver contentResolver) {
        studentRepository.importStudentsFromCsv(uri, contentResolver);
    }
}