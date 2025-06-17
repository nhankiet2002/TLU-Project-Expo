// ProfileFragment.java
package com.cse441.tluprojectexpo.fragment; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN

import android.app.AlertDialog;
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

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.Project.adapter.UserProjectsAdapter; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.model.Project; // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
import com.cse441.tluprojectexpo.model.User;    // THAY ĐỔI CHO ĐÚNG PACKAGE CỦA BẠN
// import com.cse441.tluprojectexpo.activities.LoginActivity; // Bỏ comment nếu bạn có LoginActivity

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements UserProjectsAdapter.OnProjectActionListener {

    private static final String TAG = "ProfileFragment";

    private CircleImageView avatarImageView;
    private TextView textViewUserName, textViewUserClass;
    private RelativeLayout profileLayout, logoutLayout;
    private EditText searchEditText;
    private RecyclerView projectsRecyclerView;
    private UserProjectsAdapter userProjectsAdapter;
    private List<Project> userProjectList;
    private ProgressBar progressBarProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseCurrentUser;
    private String currentUserId;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseCurrentUser = mAuth.getCurrentUser();

        if (firebaseCurrentUser != null) {
            currentUserId = firebaseCurrentUser.getUid();
        } else {
            Log.e(TAG, "User not authenticated when ProfileFragment created. This should be handled by parent activity.");
            // Consider navigating away or disabling UI if this state is reached unexpectedly.
            // For now, it will proceed, and onCreateView will handle UI based on null currentUserId.
        }
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

        userProjectList = new ArrayList<>();
        if (getContext() != null) {
            userProjectsAdapter = new UserProjectsAdapter(getContext(), userProjectList, this);
            if (projectsRecyclerView != null) { // Thêm kiểm tra null cho projectsRecyclerView
                projectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                projectsRecyclerView.setAdapter(userProjectsAdapter);
            } else {
                Log.e(TAG, "projectsRecyclerView is null in onCreateView");
            }
        } else {
            Log.e(TAG, "Context is null in onCreateView for ProfileFragment, cannot initialize adapter.");
        }

        setupListeners();

        if (currentUserId != null) {
            loadUserProfile();
            loadUserProjects(null); // Load ban đầu không có search query
        } else {
            // Xử lý UI nếu người dùng không được xác thực (dù giả định là luôn đăng nhập)
            Log.w(TAG, "currentUserId is null in onCreateView, showing not logged in UI.");
            showNotLoggedInUI();
        }

        return view;
    }

    private void showNotLoggedInUI() {
        if (profileLayout != null) profileLayout.setVisibility(View.GONE);
        if (logoutLayout != null) logoutLayout.setVisibility(View.GONE); // Hoặc đổi thành nút "Đăng nhập"
        if (searchEditText != null) searchEditText.setVisibility(View.GONE);
        if (projectsRecyclerView != null) projectsRecyclerView.setVisibility(View.GONE);
        if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);

        if (avatarImageView != null) avatarImageView.setImageResource(R.mipmap.ic_launcher_round); // Ảnh mặc định
        if (textViewUserName != null) textViewUserName.setText("Khách");
        if (textViewUserClass != null) textViewUserClass.setText("Vui lòng đăng nhập");

        if (userProjectList != null && userProjectsAdapter != null) {
            userProjectList.clear();
            userProjectsAdapter.notifyDataSetChanged();
        }
        if (getContext() != null) {
            Toast.makeText(getContext(), "Lỗi: Cần đăng nhập để xem trang này.", Toast.LENGTH_LONG).show();
        }
    }


    private void setupListeners() {
        if (profileLayout != null) {
            profileLayout.setOnClickListener(v -> {
                if (currentUserId == null) { // Kiểm tra lại đề phòng
                    if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (getContext() != null)
                    Toast.makeText(getContext(), "Chức năng xem/sửa profile chi tiết (chưa code)", Toast.LENGTH_SHORT).show();
            });
        }

        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> {
                if (currentUserId == null) { // Kiểm tra lại đề phòng
                    if (getContext() != null) Toast.makeText(getContext(), "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (getContext() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Đăng xuất")
                            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                                mAuth.signOut();
                                Toast.makeText(getContext(), "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
                                firebaseCurrentUser = null; // Cập nhật trạng thái
                                currentUserId = null;
                                showNotLoggedInUI(); // Hiển thị UI cho trạng thái chưa đăng nhập

                                // Quan trọng: Điều hướng người dùng ra khỏi Fragment này
                                // Ví dụ: nếu bạn có LoginActivity
                                // Intent intent = new Intent(getActivity(), LoginActivity.class);
                                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                // startActivity(intent);
                                // if (getActivity() != null) getActivity().finish();

                                // Hoặc nếu đây là 1 tab trong ViewPager, Activity có thể chuyển về tab Home
                                // Hoặc pop back stack nếu Fragment này được thêm vào back stack
                                if (getActivity() != null && getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                                    // getActivity().getSupportFragmentManager().popBackStack();
                                } else {
                                    Log.w(TAG, "User logged out. App needs to navigate away from ProfileFragment or handle UI change.");
                                }
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
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
                        loadUserProjects(s.toString().trim());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadUserProfile() {
        if (currentUserId == null || !isAdded() || getContext() == null) {
            Log.w(TAG, "Cannot load user profile: currentUserId is null or fragment not ready.");
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            return;
        }
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && avatarImageView != null && textViewUserName != null && textViewUserClass != null) {
                            textViewUserName.setText(user.getFullName());
                            textViewUserClass.setText(user.getUserClass()); // Đảm bảo model User có getUserClass()
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(this) // 'this' là Fragment, an toàn khi Fragment còn attached
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.mipmap.ic_launcher_round)
                                        .error(R.mipmap.ic_launcher_round)
                                        .into(avatarImageView);
                            } else {
                                avatarImageView.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        }
                    } else {
                        Log.d(TAG, "User document does not exist for ID: " + currentUserId);
                        if(textViewUserName != null) textViewUserName.setText("Không tìm thấy người dùng");
                        if(textViewUserClass != null) textViewUserClass.setText("N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(getContext(), "Lỗi tải thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProjects(String searchQuery) {
        if (currentUserId == null || !isAdded() || getContext() == null) {
            Log.w(TAG, "Cannot load user projects: currentUserId is null or fragment not ready.");
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            return;
        }
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        CollectionReference projectsRef = db.collection("Projects");
        Query query;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            // Query này yêu cầu index: CreatorUserId (ASC), Title (ASC)
            query = projectsRef.whereEqualTo("CreatorUserId", currentUserId)
                    .orderBy("Title")
                    .whereGreaterThanOrEqualTo("Title", searchQuery)
                    .whereLessThanOrEqualTo("Title", searchQuery + "\uf8ff");
        } else {
            // Query này yêu cầu index: CreatorUserId (ASC), CreatedAt (DESC)
            query = projectsRef.whereEqualTo("CreatorUserId", currentUserId)
                    .orderBy("CreatedAt", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null || userProjectsAdapter == null || userProjectList == null) {
                        Log.w(TAG, "Fragment not ready or adapter/list is null in loadUserProjects success callback.");
                        if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                        return;
                    }
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);

                    userProjectList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Project project = document.toObject(Project.class);
                            project.setProjectId(document.getId());
                            userProjectList.add(project);
                        }
                    }
                    userProjectsAdapter.notifyDataSetChanged();

                    if (userProjectList.isEmpty()) {
                        if (searchQuery == null || searchQuery.isEmpty()) {
                            // Toast.makeText(getContext(), "Bạn chưa có dự án nào.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Không tìm thấy dự án nào khớp với tìm kiếm.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user projects", e);
                    Toast.makeText(getContext(), "Lỗi tải danh sách dự án.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEditClick(Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null)
            Toast.makeText(getContext(), "Sửa dự án: " + project.getTitle() + " (chưa code)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(final Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Xóa dự án")
                    .setMessage("Bạn có chắc chắn muốn xóa dự án '" + project.getTitle() + "' không?")
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
        if (!isAdded() || getContext() == null || userProjectsAdapter == null) {
            Log.w(TAG, "Fragment not ready or adapter is null in deleteProjectFromFirestore.");
            return;
        }
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        db.collection("Projects").document(project.getProjectId()).delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null || userProjectsAdapter == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Đã xóa dự án: " + project.getTitle(), Toast.LENGTH_SHORT).show();
                    userProjectsAdapter.removeProject(project);
                    // QUAN TRỌNG: Cần xóa các dữ liệu liên quan ở các collection khác
                    // (ProjectMembers, ProjectCategories, Comments, Votes)
                    // Cách tốt nhất là dùng Firebase Cloud Functions.
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Error deleting project", e);
                    Toast.makeText(getContext(), "Lỗi xóa dự án.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(Project project) {
        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null)
            Toast.makeText(getContext(), "Xem chi tiết: " + project.getTitle() + " (chưa code)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật trạng thái người dùng hiện tại
        firebaseCurrentUser = mAuth.getCurrentUser();
        if (firebaseCurrentUser != null) {
            currentUserId = firebaseCurrentUser.getUid();
            // Tải lại dữ liệu vì có thể có thay đổi từ nơi khác hoặc cần làm mới
            // (ví dụ: sau khi sửa profile hoặc project từ activity khác)
            loadUserProfile();
            if (searchEditText != null) {
                loadUserProjects(searchEditText.getText().toString().trim());
            } else {
                loadUserProjects(null); // Hoặc không làm gì nếu searchEditText null
            }
        } else {
            // Người dùng đã đăng xuất hoặc phiên hết hạn trong khi fragment đang paused
            currentUserId = null;
            Log.w(TAG, "User became unauthenticated while ProfileFragment was paused. Showing not logged in UI.");
            showNotLoggedInUI();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng các tham chiếu view để giúp garbage collector
        avatarImageView = null;
        textViewUserName = null;
        textViewUserClass = null;
        profileLayout = null;
        logoutLayout = null;
        searchEditText = null;
        if (projectsRecyclerView != null) {
            projectsRecyclerView.setAdapter(null); // Quan trọng để giải phóng adapter
        }
        projectsRecyclerView = null;
        userProjectsAdapter = null; // Adapter sẽ được giải phóng khi RecyclerView không còn tham chiếu
        userProjectList = null;
        progressBarProfile = null;
    }
}