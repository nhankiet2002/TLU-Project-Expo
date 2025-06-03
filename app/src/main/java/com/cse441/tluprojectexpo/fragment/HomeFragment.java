package com.cse441.tluprojectexpo.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;


import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment"; // Tag cho logging

    private RecyclerView recyclerViewProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectDataList;
    private FirebaseFirestore db;
    private ProgressBar progressBar; // Thêm ProgressBar


    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectDataList = new ArrayList<>();
        db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore instance
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        progressBar = view.findViewById(R.id.progressBar); // Tìm ProgressBar trong layout

        // Thiết lập RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        projectAdapter = new ProjectAdapter(getContext(), projectDataList);
        recyclerViewProjects.setAdapter(projectAdapter);

        // Tải dữ liệu từ Firestore
        fetchProjectsFromFirestore();
    }

    private void fetchProjectsFromFirestore() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // Hiển thị ProgressBar
        }
        recyclerViewProjects.setVisibility(View.GONE); // Ẩn RecyclerView khi đang tải


        db.collection("projects") // Tên collection của bạn trên Firestore
                // .orderBy("timestamp", Query.Direction.DESCENDING) // Ví dụ sắp xếp nếu có trường timestamp
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi hoàn tất
                    }
                    recyclerViewProjects.setVisibility(View.VISIBLE); // Hiển thị lại RecyclerView


                    if (task.isSuccessful()) {
                        projectDataList.clear(); // Xóa dữ liệu cũ trước khi thêm mới
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Project project = document.toObject(Project.class);
                                project.setId(document.getId()); // Lấy ID của document và set vào đối tượng Project
                                projectDataList.add(project);
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                            projectAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                        } else {
                            Log.d(TAG, "No such documents");
                            // Xử lý trường hợp không có dữ liệu (ví dụ: hiển thị một TextView thông báo)
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        // Xử lý lỗi (ví dụ: hiển thị Toast thông báo lỗi)
                        if (getContext() != null) {
                            // Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}