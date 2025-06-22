// ProfileFragment.java
package com.cse441.tluprojectexpo.ui.Profile; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.createproject.adapter.UserProjectsAdapter;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;

// KHÔNG CẦN FirebaseAuth cho việc giả lập
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements UserProjectsAdapter.OnProjectActionListener {

    private static final String TAG = "ProfileFragment";

    // --- GIẢ LẬP ĐĂNG NHẬP VỚI USER_ID CỤ THỂ ---
    // Đảm bảo rằng trong collection "Users" của bạn có document với ID là "user_001"
    // và trong collection "Projects" có các dự án với CreatorUserId là "user_001"
    private static final String SIMULATED_USER_ID = "user_002";
    // --- KẾT THÚC PHẦN GIẢ LẬP ---

    private CircleImageView avatarImageView;
    private TextView textViewUserName, textViewUserClass;
    private RelativeLayout profileLayout, logoutLayout;
    private EditText searchEditText;
    private RecyclerView projectsRecyclerView;
    private UserProjectsAdapter userProjectsAdapter;
    private List<Project> userProjectList;
    private ProgressBar progressBarProfile;
    private SwipeRefreshLayout swipeRefreshLayoutProfile;

    // Không cần FirebaseAuth và FirebaseUser cho giả lập
    // private FirebaseAuth mAuth;
    // private FirebaseUser firebaseCurrentUser;

    private FirebaseFirestore db;
    private CollectionReference projectsRef;
    private CollectionReference usersRef;
    private CollectionReference techRef;
    private CollectionReference projectTechRef;

    private String currentUserId; // Sẽ được gán giá trị của SIMULATED_USER_ID
    private boolean isLoadingProjects = false;
    private String currentUserProjectSearchQuery = "";


    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Không khởi tạo FirebaseAuth
        // mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        projectsRef = db.collection("Projects");
        usersRef = db.collection("Users");
        techRef = db.collection("Technologies");
        projectTechRef = db.collection("ProjectTechnologies");

        // Gán trực tiếp UserID giả lập
        currentUserId = SIMULATED_USER_ID;
        Log.d(TAG, "ProfileFragment created. SIMULATING UserID: " + currentUserId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        avatarImageView = view.findViewById(R.id.avatarImageView);
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewUserClass = view.findViewById(R.id.textViewUserClass);
        profileLayout = view.findViewById(R.id.profileLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        projectsRecyclerView = view.findViewById(R.id.projectsRecyclerView);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);
        swipeRefreshLayoutProfile = view.findViewById(R.id.swipeRefreshLayoutProfile);

        // Xử lý logoutLayout cho trường hợp giả lập
        if (logoutLayout != null) {
            // Có thể ẩn đi hoàn toàn, hoặc cho nó một hành động khác nếu muốn
            logoutLayout.setVisibility(View.GONE); // Ẩn đi khi giả lập
            // Hoặc nếu muốn giữ lại và thông báo:
            // logoutLayout.setOnClickListener(v -> {
            // if (getContext() != null) Toast.makeText(getContext(), "Đang ở chế độ giả lập người dùng.", Toast.LENGTH_SHORT).show();
            // });
        }

        userProjectList = new ArrayList<>();
        if (getContext() != null) {
            userProjectsAdapter = new UserProjectsAdapter(getContext(), userProjectList, this);
            if (projectsRecyclerView != null) {
                projectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                projectsRecyclerView.setAdapter(userProjectsAdapter);
            } else {
                Log.e(TAG, "projectsRecyclerView is null in onCreateView");
            }
        } else {
            Log.e(TAG, "Context is null for ProfileFragment adapter init.");
        }

        setupListeners();

        // Vì đã giả lập currentUserId, trực tiếp tải thông tin
        // Không cần kiểm tra currentUserId != null nữa vì nó luôn được gán
        loadUserProfile();
        loadUserProjects(true);

        return view;
    }

    // Bỏ phương thức showNotLoggedInUI() vì không cần thiết khi giả lập

    private void setupListeners() {
        if (profileLayout != null) {
            profileLayout.setOnClickListener(v -> {
                // Không cần kiểm tra currentUserId == null vì đã giả lập
                if (getContext() != null)
                    Toast.makeText(getContext(), "Chức năng xem/sửa profile chi tiết (chưa code)", Toast.LENGTH_SHORT).show();
            });
        }

        // logoutLayout đã được xử lý ở onCreateView

        if (swipeRefreshLayoutProfile != null) {
            swipeRefreshLayoutProfile.setOnRefreshListener(() -> {
                Log.d(TAG, "Swipe to refresh initiated for SIMULATED user: " + currentUserId);
                loadUserProfile();
                loadUserProjects(true);
            });
        }

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentUserProjectSearchQuery = s.toString().trim();
                    loadUserProjects(true);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadUserProfile() {
        // Không cần kiểm tra currentUserId == null vì đã giả lập
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not ready or context null in loadUserProfile.");
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                swipeRefreshLayoutProfile.setRefreshing(false);
            }
            return;
        }

        if (progressBarProfile != null && (swipeRefreshLayoutProfile == null || !swipeRefreshLayoutProfile.isRefreshing())) {
            progressBarProfile.setVisibility(View.VISIBLE);
        }

        usersRef.document(currentUserId).get() // Sử dụng currentUserId là SIMULATED_USER_ID
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || getContext() == null) return;

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && avatarImageView != null && textViewUserName != null && textViewUserClass != null) {
                            textViewUserName.setText(user.getFullName());
                            textViewUserClass.setText(user.getClassName());
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.mipmap.ic_launcher_round)
                                        .error(R.mipmap.ic_launcher_round)
                                        .into(avatarImageView);
                            } else {
                                avatarImageView.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        }
                    } else {
                        Log.d(TAG, "User document does not exist for SIMULATED UserID: " + currentUserId);
                        if(textViewUserName != null) textViewUserName.setText("Không tìm thấy user " + currentUserId);
                        if(textViewUserClass != null) textViewUserClass.setText("Kiểm tra Firestore");
                        if(avatarImageView != null) avatarImageView.setImageResource(R.mipmap.ic_launcher_round);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if (progressBarProfile != null && !isLoadingProjects) progressBarProfile.setVisibility(View.GONE);
                    if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                        swipeRefreshLayoutProfile.setRefreshing(false);
                    }
                    Log.e(TAG, "Error loading user profile for SIMULATED UserID: "+currentUserId, e);
                    Toast.makeText(getContext(), "Lỗi tải thông tin cá nhân: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProjects(boolean isInitialLoadOrRefresh) {
        // Không cần kiểm tra currentUserId == null vì đã giả lập
        if (isLoadingProjects && !isInitialLoadOrRefresh) return;
        isLoadingProjects = true;

        if (progressBarProfile != null && (swipeRefreshLayoutProfile == null || !swipeRefreshLayoutProfile.isRefreshing())) {
            progressBarProfile.setVisibility(View.VISIBLE);
        }

        Query query;
        if (!currentUserProjectSearchQuery.isEmpty()) {
            query = projectsRef
                    .whereEqualTo("CreatorUserId", currentUserId) // currentUserId là SIMULATED_USER_ID
                    .orderBy("Title")
                    .whereGreaterThanOrEqualTo("Title", currentUserProjectSearchQuery)
                    .whereLessThanOrEqualTo("Title", currentUserProjectSearchQuery + "\uf8ff");
        } else {
            query = projectsRef
                    .whereEqualTo("CreatorUserId", currentUserId) // currentUserId là SIMULATED_USER_ID
                    .orderBy("CreatedAt", Query.Direction.DESCENDING);
        }

        fetchUserProjectsFromQuery(query, isInitialLoadOrRefresh);
    }


    private void fetchUserProjectsFromQuery(Query query, boolean isInitialLoadOrRefresh) {
        query.get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) {
                isLoadingProjects = false;
                if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                    swipeRefreshLayoutProfile.setRefreshing(false);
                }
                return;
            }

            isLoadingProjects = false;
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                swipeRefreshLayoutProfile.setRefreshing(false);
            }

            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    List<Project> fetchedProjects = new ArrayList<>();
                    List<Task<Void>> tasksToComplete = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            Project project = document.toObject(Project.class);
                            project.setProjectId(document.getId());
                            fetchedProjects.add(project);

                            Task<Void> techTask = projectTechRef.whereEqualTo("ProjectId", project.getProjectId()).get()
                                    .continueWithTask(projectTechQueryTask -> {
                                        if (projectTechQueryTask.isSuccessful() && projectTechQueryTask.getResult() != null) {
                                            List<Task<DocumentSnapshot>> techNameTasks = new ArrayList<>();
                                            for (QueryDocumentSnapshot ptDoc : projectTechQueryTask.getResult()) {
                                                String techId = ptDoc.getString("TechnologyId");
                                                if (techId != null && !techId.isEmpty()) {
                                                    techNameTasks.add(techRef.document(techId).get());
                                                }
                                            }
                                            return Tasks.whenAllSuccess(techNameTasks);
                                        }
                                        return Tasks.forResult(new ArrayList<>());
                                    }).continueWith(techNameResultsTask -> {
                                        if (techNameResultsTask.isSuccessful() && techNameResultsTask.getResult() != null) {
                                            List<?> results = (List<?>) techNameResultsTask.getResult();
                                            List<String> techNames = new ArrayList<>();
                                            for (Object resultItem : results) {
                                                if (resultItem instanceof DocumentSnapshot) {
                                                    DocumentSnapshot techDoc = (DocumentSnapshot) resultItem;
                                                    if (techDoc.exists()) {
                                                        techNames.add(techDoc.getString("Name"));
                                                    }
                                                }
                                            }
                                            project.setTechnologyNames(techNames);
                                        } else {
                                            Log.w(TAG, "Error getting tech names for project " + project.getProjectId(), techNameResultsTask.getException());
                                            project.setTechnologyNames(new ArrayList<>());
                                        }
                                        return null;
                                    });
                            tasksToComplete.add(techTask);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Project: " + document.getId(), e);
                        }
                    }

                    Tasks.whenAll(tasksToComplete).addOnCompleteListener(allExtraInfoTasks -> {
                        if (!isAdded() || userProjectsAdapter == null) return;
                        if (isInitialLoadOrRefresh) {
                            userProjectsAdapter.updateProjects(fetchedProjects);
                        }
                    });

                } else {
                    if (isInitialLoadOrRefresh && userProjectsAdapter != null) {
                        userProjectsAdapter.clearProjects();
                    }
                    if (getContext() != null && isInitialLoadOrRefresh && (currentUserProjectSearchQuery == null || currentUserProjectSearchQuery.isEmpty())) {
                        // Toast.makeText(getContext(), "User " + currentUserId + " chưa có dự án nào.", Toast.LENGTH_SHORT).show();
                    } else if (getContext() != null && isInitialLoadOrRefresh && !currentUserProjectSearchQuery.isEmpty()){
                        Toast.makeText(getContext(), "Không tìm thấy dự án nào khớp với tìm kiếm cho user " + currentUserId + ".", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.e(TAG, "Error getting user projects for SIMULATED UserID: "+currentUserId, task.getException());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải danh sách dự án: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
                if (userProjectsAdapter != null) {
                    userProjectsAdapter.clearProjects();
                }
            }
        });
    }

    // Các phương thức onEditClick, onDeleteClick, deleteProjectFromFirestore, onItemClick giữ nguyên
    // nhưng không cần kiểm tra currentUserId == null vì đã giả lập

    @Override
    public void onEditClick(Project project) {
        if (getContext() != null && project != null)
            Toast.makeText(getContext(), "Sửa dự án: " + project.getTitle() + " (user: " + currentUserId + ") (chưa code)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(final Project project) {
        if (getContext() != null && project != null) {
            // Kiểm tra xem simulated user có phải là chủ dự án không (dù đang giả lập)
            if (!currentUserId.equals(project.getCreatorUserId())) {
                Toast.makeText(getContext(), "User giả lập ("+currentUserId+") không phải chủ dự án này ("+project.getCreatorUserId()+").", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle("Xóa dự án")
                    .setMessage("Bạn có chắc chắn muốn xóa dự án '" + project.getTitle() + "' của user " + currentUserId + "?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteProjectFromFirestore(project))
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    private void deleteProjectFromFirestore(final Project project) {
        if (project.getProjectId() == null || project.getProjectId().isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "ID dự án không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Double check ownership, even in simulation
        if (!currentUserId.equals(project.getCreatorUserId())) {
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi: User giả lập ("+currentUserId+") không khớp chủ dự án ("+project.getCreatorUserId()+").", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isAdded() || getContext() == null || userProjectsAdapter == null) return;
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        projectsRef.document(project.getProjectId()).delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null || userProjectsAdapter == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Đã xóa dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();
                    userProjectsAdapter.removeProject(project);
                    Log.i(TAG, "Project " + project.getProjectId() + " deleted for SIMULATED user " + currentUserId + ". REMEMBER TO DELETE RELATED DATA.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Error deleting project: " + project.getProjectId() + " for SIMULATED user " + currentUserId, e);
                    Toast.makeText(getContext(), "Lỗi xóa dự án: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(Project project) {
        if (getActivity() != null && project != null && project.getProjectId() != null) {
            Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getProjectId());
            startActivity(intent);
        } else {
            Log.e(TAG, "Cannot start ProjectDetailActivity. Context, project, or project ID is null.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Không thể mở chi tiết dự án.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Khi giả lập, currentUserId không thay đổi, chỉ cần tải lại dữ liệu nếu cần
        Log.d(TAG, "ProfileFragment onResume (SIMULATED User: " + currentUserId + "). Reloading data.");
        loadUserProfile();
        loadUserProjects(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "ProfileFragment onDestroyView.");
        avatarImageView = null;
        textViewUserName = null;
        textViewUserClass = null;
        profileLayout = null;
        logoutLayout = null;
        searchEditText = null;
        if (projectsRecyclerView != null) {
            projectsRecyclerView.setAdapter(null);
        }
        projectsRecyclerView = null;
        userProjectsAdapter = null;
        if (userProjectList != null) {
            userProjectList.clear();
        }
        userProjectList = null;
        progressBarProfile = null;
        swipeRefreshLayoutProfile = null;
    }
}