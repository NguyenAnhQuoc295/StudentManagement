package com.AnhQuoc.studentmanagementapp.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.AnhQuoc.studentmanagementapp.R;

public class StudentListFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Liên kết file Java này với file layout XML của nó
        return inflater.inflate(R.layout.fragment_student_list, container, false);
    }
}