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
import android.widget.ProgressBar; // Đảm bảo import đúng
import android.widget.Toast;     // Để thông báo lỗi nếu cần

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Project;     // Kiểm tra đường dẫn package
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import cho OrderBy
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
    private ProgressBar progressBar;


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
        // Khởi tạo Firestore instance
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ Views
        recyclerViewProjects = view.findViewById(R.id.recyclerViewProjects);
        progressBar = view.findViewById(R.id.progressBar); // Tìm ProgressBar trong layout

        // Thiết lập RecyclerView
        recyclerViewProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        // Khởi tạo adapter với context và danh sách rỗng ban đầu
        projectAdapter = new ProjectAdapter(getContext(), projectDataList);
        recyclerViewProjects.setAdapter(projectAdapter);

        // Tải dữ liệu từ Firestore
        fetchProjectsFromFirestore();
    }

    private void fetchProjectsFromFirestore() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // Hiển thị ProgressBar
        }
        // Tùy chọn: Ẩn RecyclerView khi đang tải để tránh hiển thị dữ liệu cũ (nếu có)
        // recyclerViewProjects.setVisibility(View.GONE);


        // Tên collection của bạn trên Firestore, ví dụ: "projects"
        // Bạn có thể thêm .orderBy() nếu muốn sắp xếp, ví dụ theo tên hoặc một trường timestamp
        db.collection("projects") // <<<< THAY "projects" BẰNG TÊN COLLECTION CỦA BẠN
                //.orderBy("name", Query.Direction.ASCENDING) // Ví dụ: Sắp xếp theo tên tăng dần
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi hoàn tất
                        }
                        // recyclerViewProjects.setVisibility(View.VISIBLE); // Hiển thị lại RecyclerView

                        if (task.isSuccessful()) {
                            projectDataList.clear(); // Xóa dữ liệu cũ trước khi thêm mới
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    // Chuyển đổi DocumentSnapshot thành đối tượng Project
                                    // Đảm bảo class Project có constructor rỗng và các setter/getter
                                    Project project = document.toObject(Project.class);
                                    // Lấy ID của document và set vào đối tượng Project (quan trọng nếu bạn cần ID)
                                    project.setId(document.getId());
                                    projectDataList.add(project);
                                    Log.d(TAG, "Fetched project: " + project.getName() + " with ID: " + project.getId());
                                }
                                // Cập nhật RecyclerView sau khi có dữ liệu mới
                                projectAdapter.notifyDataSetChanged();
                            } else {
                                Log.d(TAG, "No projects found in Firestore.");
                                // Tùy chọn: Hiển thị thông báo không có dữ liệu
                                if (getContext() != null) {
                                    // Toast.makeText(getContext(), "Không tìm thấy dự án nào.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting documents from Firestore.", task.getException());
                            // Xử lý lỗi (ví dụ: hiển thị Toast thông báo lỗi)
                            if (getContext() != null && task.getException() != null) {
                                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            } else if (getContext() != null) {
                                Toast.makeText(getContext(), "Lỗi tải dữ liệu không xác định.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }
}