// ProfileFragment.java
package com.cse441.tluprojectexpo.ui.Profile; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
import com.cse441.tluprojectexpo.auth.LoginActivity; // GIẢ SỬ BẠN CÓ LoginActivity
import com.cse441.tluprojectexpo.ui.detailproject.ProjectDetailActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.ui.createproject.adapter.UserProjectsAdapter;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;

// Khôi phục Firebase Auth
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
import com.cse441.tluprojectexpo.ui.editproject.EditProjectActivity;
import com.cse441.tluprojectexpo.auth.SettingProfileActivity;

public class ProfileFragment extends Fragment implements UserProjectsAdapter.OnProjectActionListener {

    private static final String TAG = "ProfileFragment";

    private CircleImageView avatarImageView;
    private TextView textViewUserName, textViewUserClass, textViewNotLoggedIn;
    private RelativeLayout profileLayout, logoutLayout;
    private EditText searchEditText;
    private RecyclerView projectsRecyclerView;
    private UserProjectsAdapter userProjectsAdapter;
    private List<Project> userProjectList;
    private ProgressBar progressBarProfile;
    private SwipeRefreshLayout swipeRefreshLayoutProfile;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser firebaseCurrentUser;

    private FirebaseFirestore db;
    private CollectionReference projectsRef;
    private CollectionReference usersRef;
    private CollectionReference techRef;
    private CollectionReference projectTechRef;

    private String currentUserId;
    private boolean isLoadingProjects = false;
    private String currentUserProjectSearchQuery = "";


    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        projectsRef = db.collection("Projects");
        usersRef = db.collection("Users");
        techRef = db.collection("Technologies");
        projectTechRef = db.collection("ProjectTechnologies");

        setupAuthListener();
        Log.d(TAG, "ProfileFragment created.");
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
        textViewNotLoggedIn = view.findViewById(R.id.textViewNotLoggedIn); // Thêm TextView này vào layout của bạn

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
        // Việc tải dữ liệu sẽ được kích hoạt bởi AuthStateListener
        return view;
    }

    private void setupAuthListener() {
        mAuthListener = firebaseAuth -> {
            firebaseCurrentUser = firebaseAuth.getCurrentUser();
            if (firebaseCurrentUser != null) {
                currentUserId = firebaseCurrentUser.getUid();
                Log.d(TAG, "User signed in: " + currentUserId);
                updateUIForLoggedInUser();
            } else {
                currentUserId = null;
                Log.d(TAG, "User signed out.");
                updateUIForLoggedOutUser();
            }
        };
    }

    private void updateUIForLoggedInUser() {
        if (!isAdded()) return; // Fragment not attached

        if (textViewNotLoggedIn != null) textViewNotLoggedIn.setVisibility(View.GONE);
        if (profileLayout != null) profileLayout.setVisibility(View.VISIBLE);
        if (logoutLayout != null) logoutLayout.setVisibility(View.VISIBLE);
        if (searchEditText != null) searchEditText.setVisibility(View.VISIBLE);
        if (projectsRecyclerView != null) projectsRecyclerView.setVisibility(View.VISIBLE);
        if (swipeRefreshLayoutProfile != null) swipeRefreshLayoutProfile.setEnabled(true);


        loadUserProfile();
        loadUserProjects(true);
    }

    private void updateUIForLoggedOutUser() {
        if (!isAdded()) return; // Fragment not attached

        if (avatarImageView != null) avatarImageView.setImageResource(R.mipmap.ic_launcher_round);
        if (textViewUserName != null) textViewUserName.setText("Khách");
        if (textViewUserClass != null) textViewUserClass.setText("Vui lòng đăng nhập");
        if (profileLayout != null) profileLayout.setVisibility(View.GONE); // Hoặc điều chỉnh click listener
        if (logoutLayout != null) logoutLayout.setVisibility(View.GONE);
        if (searchEditText != null) {
            searchEditText.setText("");
            searchEditText.setVisibility(View.GONE);
        }
        if (projectsRecyclerView != null) projectsRecyclerView.setVisibility(View.GONE);
        if (userProjectsAdapter != null) userProjectsAdapter.clearProjects();
        if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
        if (swipeRefreshLayoutProfile != null) {
            swipeRefreshLayoutProfile.setRefreshing(false);
            swipeRefreshLayoutProfile.setEnabled(false);
        }

        if (textViewNotLoggedIn != null) {
            textViewNotLoggedIn.setVisibility(View.VISIBLE);
            textViewNotLoggedIn.setText("Bạn chưa đăng nhập. Nhấn để đăng nhập.");
            textViewNotLoggedIn.setOnClickListener(v -> {
                // Điều hướng đến LoginActivity
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class); // Đảm bảo bạn có LoginActivity
                    // Xóa các activity trước đó khỏi stack để người dùng không quay lại màn hình profile khi chưa đăng nhập
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    // getActivity().finish(); // Cân nhắc nếu đây là activity chính sau đăng nhập
                }
            });
        }
    }


    private void setupListeners() {
        if (profileLayout != null) {
            profileLayout.setOnClickListener(v -> {
                if (currentUserId != null && getContext() != null) {
                    Intent intent = new Intent(getActivity(), SettingProfileActivity.class);
                    startActivity(intent);
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> {
                if (getContext() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Đăng xuất")
                            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                                mAuth.signOut();
                                // AuthStateListener sẽ xử lý việc cập nhật UI và điều hướng nếu cần
                                Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });
        }

        if (swipeRefreshLayoutProfile != null) {
            swipeRefreshLayoutProfile.setOnRefreshListener(() -> {
                if (currentUserId != null) {
                    Log.d(TAG, "Swipe to refresh initiated for user: " + currentUserId);
                    loadUserProfile();
                    loadUserProjects(true);
                } else {
                    swipeRefreshLayoutProfile.setRefreshing(false); // Không làm gì nếu chưa đăng nhập
                    if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (currentUserId != null) {
                        currentUserProjectSearchQuery = s.toString().trim();
                        loadUserProjects(true); // Tải lại với query mới
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (avatarImageView != null) {
            avatarImageView.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Bấm vào vùng ảnh đại diện!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadUserProfile() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "Cannot load user profile, currentUserId is null or empty.");
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                swipeRefreshLayoutProfile.setRefreshing(false);
            }
            // AuthStateListener đã xử lý việc hiển thị UI cho trạng thái chưa đăng nhập
            return;
        }

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

        usersRef.document(currentUserId).get()
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
                        Log.d(TAG, "User document does not exist for UserID: " + currentUserId);
                        if(textViewUserName != null) textViewUserName.setText("Không tìm thấy user");
                        if(textViewUserClass != null) textViewUserClass.setText("Kiểm tra Firestore");
                        if(avatarImageView != null) avatarImageView.setImageResource(R.mipmap.ic_launcher_round);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Error loading user profile for UserID: "+currentUserId, e);
                    if (getContext() != null) Toast.makeText(getContext(), "Lỗi tải thông tin cá nhân: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> { // Luôn ẩn progress bar khi hoàn tất (dù thành công hay thất bại)
                    if (progressBarProfile != null && !isLoadingProjects) progressBarProfile.setVisibility(View.GONE);
                    if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                        swipeRefreshLayoutProfile.setRefreshing(false);
                    }
                });
    }

    private void loadUserProjects(boolean isInitialLoadOrRefresh) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "Cannot load user projects, currentUserId is null or empty.");
            if (userProjectsAdapter != null) userProjectsAdapter.clearProjects();
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                swipeRefreshLayoutProfile.setRefreshing(false);
            }
            return;
        }

        if (isLoadingProjects && !isInitialLoadOrRefresh) return;
        isLoadingProjects = true;

        if (progressBarProfile != null && (swipeRefreshLayoutProfile == null || !swipeRefreshLayoutProfile.isRefreshing())) {
            progressBarProfile.setVisibility(View.VISIBLE);
        }

        Query query;
        if (!currentUserProjectSearchQuery.isEmpty()) {
            query = projectsRef
                    .whereEqualTo("CreatorUserId", currentUserId)
                    .orderBy("Title") // Cần index composite: (CreatorUserId, Title)
                    .startAt(currentUserProjectSearchQuery)
                    .endAt(currentUserProjectSearchQuery + "\uf8ff");
        } else {
            query = projectsRef
                    .whereEqualTo("CreatorUserId", currentUserId)
                    .orderBy("CreatedAt", Query.Direction.DESCENDING); // Cần index: (CreatorUserId, CreatedAt DESC)
        }

        fetchUserProjectsFromQuery(query, isInitialLoadOrRefresh);
    }


    private void fetchUserProjectsFromQuery(Query query, boolean isInitialLoadOrRefresh) {
        query.get().addOnCompleteListener(task -> {
            isLoadingProjects = false; // Luôn đặt lại isLoadingProjects ở đây

            if (!isAdded() || getContext() == null) {
                if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                if (swipeRefreshLayoutProfile != null && swipeRefreshLayoutProfile.isRefreshing()) {
                    swipeRefreshLayoutProfile.setRefreshing(false);
                }
                return;
            }

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
                            // Chỉ thêm dự án nếu CreatorUserId khớp với currentUserId (dù query đã làm điều này, đây là một lớp bảo vệ)
                            if (currentUserId.equals(project.getCreatorUserId())) {
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
                            } else {
                                Log.w(TAG, "Project " + project.getProjectId() + " fetched but CreatorUserId " + project.getCreatorUserId() + " does not match currentUserId " + currentUserId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Project: " + document.getId(), e);
                        }
                    }

                    Tasks.whenAll(tasksToComplete).addOnCompleteListener(allExtraInfoTasks -> {
                        if (!isAdded() || userProjectsAdapter == null) return;
                        if (isInitialLoadOrRefresh) {
                            userProjectsAdapter.updateProjects(fetchedProjects);
                        }
                        if (fetchedProjects.isEmpty() && isInitialLoadOrRefresh) {
                            if (currentUserProjectSearchQuery == null || currentUserProjectSearchQuery.isEmpty()){
                                //  Toast.makeText(getContext(), "Bạn chưa có dự án nào.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Không tìm thấy dự án nào khớp với tìm kiếm.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    if (isInitialLoadOrRefresh && userProjectsAdapter != null) {
                        userProjectsAdapter.clearProjects();
                    }
                    if (getContext() != null && isInitialLoadOrRefresh && (currentUserProjectSearchQuery == null || currentUserProjectSearchQuery.isEmpty())) {
                        // Toast.makeText(getContext(), "Bạn chưa có dự án nào.", Toast.LENGTH_SHORT).show();
                    } else if (getContext() != null && isInitialLoadOrRefresh && !currentUserProjectSearchQuery.isEmpty()){
                        Toast.makeText(getContext(), "Không tìm thấy dự án nào khớp với tìm kiếm.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.e(TAG, "Error getting user projects for UserID: "+currentUserId, task.getException());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải danh sách dự án: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
                if (userProjectsAdapter != null) {
                    userProjectsAdapter.clearProjects();
                }
            }
        });
    }


    @Override
    public void onEditClick(Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null && project != null) {
            // Kiểm tra chủ sở hữu trước khi cho phép sửa
            if (currentUserId.equals(project.getCreatorUserId())) {
                Intent intent = new Intent(getActivity(), EditProjectActivity.class);
                intent.putExtra(EditProjectActivity.EXTRA_PROJECT_ID, project.getProjectId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Bạn không có quyền sửa dự án này.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDeleteClick(final Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null && project != null) {
            if (!currentUserId.equals(project.getCreatorUserId())) {
                Toast.makeText(getContext(), "Bạn không phải chủ dự án này.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle("Xóa dự án")
                    .setMessage("Bạn có chắc chắn muốn xóa dự án '" + project.getTitle() + "'?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteProjectFromFirestore(project))
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    private void deleteProjectFromFirestore(final Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (project.getProjectId() == null || project.getProjectId().isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "ID dự án không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentUserId.equals(project.getCreatorUserId())) {
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi: Bạn không phải chủ dự án.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isAdded() || getContext() == null || userProjectsAdapter == null) return;
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        // TODO: Xóa các bản ghi liên quan trong ProjectTechnologies trước hoặc sau khi xóa project
        // Ví dụ: Xóa ProjectTechnologies liên quan
        Task<Void> deleteProjectTechTask = projectTechRef.whereEqualTo("ProjectId", project.getProjectId()).get()
                .onSuccessTask(queryDocumentSnapshots -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteTasks.add(doc.getReference().delete());
                    }
                    return Tasks.whenAll(deleteTasks);
                });

        deleteProjectTechTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error deleting related ProjectTechnologies for " + project.getProjectId(), task.getException());
                // Có thể quyết định dừng lại hoặc tiếp tục xóa project chính
            }
            return projectsRef.document(project.getProjectId()).delete();
        }).addOnSuccessListener(aVoid -> {
            if (!isAdded() || getContext() == null || userProjectsAdapter == null) return;
            if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Đã xóa dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();
            userProjectsAdapter.removeProject(project);
            Log.i(TAG, "Project " + project.getProjectId() + " and related data deleted for user " + currentUserId);
            // Có thể cần tải lại danh sách dự án nếu có thay đổi ngoài dự án này
            // loadUserProjects(true); // Cân nhắc
        }).addOnFailureListener(e -> {
            if (!isAdded() || getContext() == null) return;
            if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            Log.e(TAG, "Error deleting project: " + project.getProjectId() + " for user " + currentUserId, e);
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
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener); // Đăng ký listener
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener); // Hủy đăng ký listener
        }
    }

    // onResume không cần gọi loadUserProfile và loadUserProjects nữa
    // vì AuthStateListener sẽ xử lý khi trạng thái auth thay đổi (ví dụ sau khi đăng nhập/đăng xuất)
    // và onStart sẽ kích hoạt listener lần đầu.
    // Nếu bạn muốn reload dữ liệu mỗi khi fragment resume (ví dụ, dữ liệu có thể thay đổi từ nơi khác),
    // bạn có thể thêm lại logic load ở đây, nhưng kiểm tra currentUserId != null.
    @Override
    public void onResume() {
        super.onResume();
        // Nếu currentUserId != null, có thể bạn muốn refresh dữ liệu ở đây
        // Ví dụ: if (currentUserId != null) { loadUserProfile(); loadUserProjects(false); }
        // Tuy nhiên, việc này có thể gây load dữ liệu nhiều lần không cần thiết.
        // AuthStateListener và SwipeRefresh đã xử lý hầu hết các trường hợp.
        Log.d(TAG, "ProfileFragment onResume. Current user: " + (currentUserId != null ? currentUserId : "null"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "ProfileFragment onDestroyView.");
        // Giải phóng các view references
        avatarImageView = null;
        textViewUserName = null;
        textViewUserClass = null;
        profileLayout = null;
        logoutLayout = null;
        searchEditText = null;
        textViewNotLoggedIn = null;
        if (projectsRecyclerView != null) {
            projectsRecyclerView.setAdapter(null); // Quan trọng để tránh leak adapter/context
        }
        projectsRecyclerView = null;
        userProjectsAdapter = null; // Adapter sẽ được giải phóng bởi RecyclerView
        if (userProjectList != null) {
            userProjectList.clear();
        }
        // userProjectList = null; // Để lại cho GC
        progressBarProfile = null;
        swipeRefreshLayoutProfile = null;
    }
}