package com.cse441.tluprojectexpo.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.adapter.ProjectAdapter;
import com.cse441.tluprojectexpo.model.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;


import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements ProjectAdapter.OnProjectActionListener {

    private static final String TAG = "ProfileFragment";

    private CircleImageView avatarImageView;
    private TextView textViewUserName;
    private TextView textViewUserClass;
    private RelativeLayout profileLayout;
    private RelativeLayout logoutLayout;
    private EditText searchEditText;
    private RecyclerView projectsRecyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ProjectAdapter projectAdapter;
    // No need for allProjectsList here if adapter manages its own full list for filtering
    // private List<Project> displayedProjectsList; // This will be the list adapter uses

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        // displayedProjectsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarImageView = view.findViewById(R.id.avatarImageView);
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewUserClass = view.findViewById(R.id.textViewUserClass);
        profileLayout = view.findViewById(R.id.profileLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        projectsRecyclerView = view.findViewById(R.id.projectsRecyclerView);

        setupRecyclerView();
        loadSampleUserProfile(); // Placeholder for actual user data
        loadProjectsForProfile(); // Load projects relevant to this profile (all for now)


        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (projectAdapter != null) {
                    projectAdapter.filter(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        profileLayout.setOnClickListener(v -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Chức năng sửa Profile (Chưa triển khai)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        // Initialize adapter with an empty list and 'this' as the listener
        projectAdapter = new ProjectAdapter(getContext(), new ArrayList<>(), this);
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        projectsRecyclerView.setAdapter(projectAdapter);
    }

    private void loadSampleUserProfile() {
        // This will be replaced with actual user data loading logic
        textViewUserName.setText("Người Dùng Mẫu");
        textViewUserClass.setText("Lớp Mẫu");
        if (getContext() != null && avatarImageView != null) {
            Glide.with(getContext())
                    .load(R.mipmap.ic_launcher_round) // Replace with actual avatar URL
                    .placeholder(R.drawable.ic_image_placeholder) // Placeholder for Glide
                    .error(R.drawable.error) // Error image for Glide
                    .into(avatarImageView);
        }
    }

    private void loadProjectsForProfile() {
        if (db == null) {
            Log.e(TAG, "Firestore instance is null.");
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // For now, load all projects. Later, this could be filtered by user ID.
        // Assuming 'createdAt' is a Timestamp field for ordering.
        // If not, order by 'name' or another suitable field.
        db.collection("projects")
                .orderBy("name", Query.Direction.ASCENDING) // Or "createdAt", Query.Direction.DESCENDING
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    List<Project> loadedProjects = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Project project = document.toObject(Project.class);
                            project.setId(document.getId()); // Crucial: Set the document ID
                            loadedProjects.add(project);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Project: " + document.getId(), e);
                        }
                    }
                    if (projectAdapter != null) {
                        projectAdapter.updateProjects(loadedProjects); // Update adapter's list
                    }
                    if (loadedProjects.isEmpty()) {
                        Toast.makeText(getContext(), "Không tìm thấy dự án nào.", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "Loaded " + loadedProjects.size() + " projects for profile.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Error loading projects for profile", e);
                    Toast.makeText(getContext(), "Không thể tải dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    @Override
    public void onEditProject(Project project) {
        if (getContext() == null || getView() == null || project == null || project.getId() == null) {
            Log.e(TAG, "Cannot edit: Context, View, Project, or Project ID is null.");
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi: Không thể sửa dự án.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "Sửa: " + project.getName(), Toast.LENGTH_SHORT).show();
        NavController navController = Navigation.findNavController(requireView());
        Bundle editArgs = new Bundle();
        editArgs.putString("projectIdToEdit", project.getId());
        try {
            // Ensure this action is defined in your nav_graph.xml
            navController.navigate(R.id.action_profileFragment_to_createFragment_for_edit, editArgs);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Navigation to edit failed. Action or arguments might be incorrect.", e);
            Toast.makeText(getContext(), "Lỗi điều hướng sửa. Kiểm tra cấu hình.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Unknown error during navigation to edit.", e);
            Toast.makeText(getContext(), "Lỗi khi mở màn hình sửa.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeleteProject(Project project, int position) {
        if (getContext() == null || project == null || project.getId() == null) {
            Log.e(TAG, "Cannot delete: Context, Project, or Project ID is null.");
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi: Không thể xóa dự án.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Xóa Dự Án")
                .setMessage("Bạn có chắc chắn muốn xóa '" + project.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (db == null) {
                        if (getContext() != null) Toast.makeText(getContext(), "Lỗi cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String projectId = project.getId();
                    db.collection("projects").document(projectId).delete()
                            .addOnSuccessListener(aVoid -> {
                                if (!isAdded() || getContext() == null) return;
                                Toast.makeText(getContext(), "'" + project.getName() + "' đã được xóa.", Toast.LENGTH_SHORT).show();
                                if (projectAdapter != null) {
                                    projectAdapter.removeProject(position);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || getContext() == null) return;
                                Toast.makeText(getContext(), "Lỗi khi xóa dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Error deleting project: " + projectId, e);
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSampleUserProfile(); // Refresh user info (placeholder for now)
        loadProjectsForProfile(); // Refresh projects list
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (projectAdapter != null) {
            projectAdapter.shutdownExecutor();
        }
        // Nullify views to help garbage collector
        avatarImageView = null;
        textViewUserName = null;
        textViewUserClass = null;
        profileLayout = null;
        logoutLayout = null;
        searchEditText = null;
        projectsRecyclerView = null;
    }
}