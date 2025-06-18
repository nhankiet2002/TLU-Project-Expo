// ProfileFragment.java
package com.cse441.tluprojectexpo.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager; // Vẫn cần import này nếu bạn muốn dùng PreferenceManager cho các trường hợp khác
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
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.Project.adapter.UserProjectsAdapter;
import com.cse441.tluprojectexpo.model.Project;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.auth.LoginActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Constants cho SharedPreferences (giống LoginActivity và OpenActivity)
    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_LAST_EMAIL = "last_email"; // Để xóa nếu có
    private static final String KEY_IS_GUEST_MODE = "isGuestMode"; // Khai báo KEY_IS_GUEST_MODE

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
            if (projectsRecyclerView != null) {
                projectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                projectsRecyclerView.setAdapter(userProjectsAdapter);
            } else {
                Log.e(TAG, "projectsRecyclerView is null in onCreateView");
            }
        } else {
            Log.e(TAG, "Context is null in onCreateView for ProfileFragment, cannot initialize adapter.");
        }

        setupListeners();

        // Kiểm tra trạng thái khách hoặc người dùng đăng nhập để hiển thị UI phù hợp
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);

        if (isGuestMode) {
            Log.d(TAG, "Entering ProfileFragment in Guest Mode.");
            showGuestModeUI();
        } else if (currentUserId != null) {
            Log.d(TAG, "Entering ProfileFragment in Authenticated Mode.");
            loadUserProfile();
            loadUserProjects(null);
        } else {
            Log.w(TAG, "currentUserId is null and not in Guest Mode, showing not logged in UI.");
            showNotLoggedInUI();
        }

        return view;
    }

    // Phương thức mới để hiển thị UI cho chế độ khách
    private void showGuestModeUI() {
        if (profileLayout != null) profileLayout.setVisibility(View.GONE); // Ẩn phần thông tin profile
        if (searchEditText != null) searchEditText.setVisibility(View.GONE); // Ẩn tìm kiếm dự án
        if (projectsRecyclerView != null) projectsRecyclerView.setVisibility(View.GONE); // Ẩn danh sách dự án
        if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);

        if (avatarImageView != null) {
            // Có thể dùng ảnh mặc định cho khách hoặc ảnh trống
            avatarImageView.setImageResource(R.drawable.default_avatar); // Hoặc một icon khách
        }
        if (textViewUserName != null) textViewUserName.setText("Khách");
        if (textViewUserClass != null) textViewUserClass.setText("Chế độ khách"); // Hoặc một text phù hợp

        // Nút đăng xuất nên đổi thành "Đăng nhập/Thoát chế độ khách"
        // Để đơn giản, hiện tại vẫn giữ nút logout, nhưng có thể điều chỉnh text/onclick
        if (logoutLayout != null) {
            // Bạn có thể tìm Button bên trong logoutLayout và đổi text, ví dụ:
            // Button logoutButton = logoutLayout.findViewById(R.id.btnLogout);
            // if (logoutButton != null) logoutButton.setText("Đăng nhập");
            logoutLayout.setVisibility(View.VISIBLE); // Đảm bảo nút logout (hoặc đổi tên) hiển thị
        }

        if (userProjectList != null && userProjectsAdapter != null) {
            userProjectList.clear();
            userProjectsAdapter.notifyDataSetChanged();
        }
        if (getContext() != null) {
            Toast.makeText(getContext(), "Bạn đang ở chế độ khách. Một số chức năng có thể bị hạn chế.", Toast.LENGTH_LONG).show();
        }
    }


    private void showNotLoggedInUI() {
        if (profileLayout != null) profileLayout.setVisibility(View.GONE);
        if (logoutLayout != null) logoutLayout.setVisibility(View.GONE); // Hoặc đổi thành nút "Đăng nhập"
        if (searchEditText != null) searchEditText.setVisibility(View.GONE);
        if (projectsRecyclerView != null) projectsRecyclerView.setVisibility(View.GONE);
        if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);

        if (avatarImageView != null) avatarImageView.setImageResource(R.mipmap.ic_launcher_round); // Ảnh mặc định
        if (textViewUserName != null) textViewUserName.setText("Chưa đăng nhập");
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
                // Kiểm tra nếu đang ở chế độ khách thì không cho vào sửa profile
                SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);
                if (isGuestMode) {
                    if (getContext() != null) Toast.makeText(getContext(), "Bạn đang ở chế độ khách, không thể sửa thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentUserId == null) {
                    if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (getContext() != null)
                    Toast.makeText(getContext(), "Chức năng xem/sửa profile chi tiết (chưa code)", Toast.LENGTH_SHORT).show();
            });
        }

        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> {
                if (getContext() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Thoát") // Đổi title thành "Thoát"
                            .setMessage("Bạn có chắc chắn muốn thoát chế độ khách hoặc đăng xuất không?") // Đổi message
                            .setPositiveButton("Thoát", (dialog, which) -> {
                                // Xóa trạng thái isGuestMode và remember_login
                                SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(KEY_IS_GUEST_MODE, false); // Đặt trạng thái không phải khách
                                editor.putBoolean(KEY_REMEMBER_LOGIN, false); // Đặt không nhớ tài khoản
                                editor.remove(KEY_LAST_EMAIL); // Xóa email cuối cùng đã lưu
                                editor.apply();

                                // Đăng xuất khỏi Firebase Auth nếu đang có phiên đăng nhập
                                if (mAuth.getCurrentUser() != null) {
                                    mAuth.signOut();
                                    Log.d(TAG, "Đã đăng xuất người dùng Firebase.");
                                }

                                Toast.makeText(getContext(), "Đã thoát.", Toast.LENGTH_SHORT).show();
                                firebaseCurrentUser = null;
                                currentUserId = null;
                                // Không showNotLoggedInUI() nữa vì chúng ta sẽ chuyển Activity

                                // Điều hướng về LoginActivity
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                if (getActivity() != null) getActivity().finish();
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
                    // Chỉ cho phép tìm kiếm nếu không phải chế độ khách và có user
                    SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);
                    if (currentUserId != null && !isGuestMode) {
                        loadUserProjects(s.toString().trim());
                    } else if (isGuestMode) {
                        // Nếu là khách, không cho phép tìm kiếm và có thể thông báo
                        if (getContext() != null) Toast.makeText(getContext(), "Chức năng tìm kiếm không khả dụng ở chế độ khách.", Toast.LENGTH_SHORT).show();
                        searchEditText.setText(""); // Xóa nội dung nhập
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
                        // User user = documentSnapshot.toObject(User.class); // Bạn không dùng User.class để map
                        Map<String, Object> userData = documentSnapshot.getData(); // Đọc dưới dạng Map

                        if (userData != null && avatarImageView != null && textViewUserName != null && textViewUserClass != null) {
                            // Lấy dữ liệu từ Map
                            String userName = (String) userData.get("FullName");
                            String userClass = (String) userData.get("Class"); // Lấy trường "Class"
                            String avatarUrl = (String) userData.get("AvatarUrl");

                            textViewUserName.setText(userName != null && !userName.isEmpty() ? userName : "Người dùng");
                            textViewUserClass.setText(userClass != null && !userClass.isEmpty() ? userClass : "N/A"); // Hiển thị "Class"

                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar) // Đảm bảo bạn có default_avatar.xml
                                        .error(R.drawable.default_avatar)
                                        .into(avatarImageView);
                            } else {
                                // Nếu không có avatarUrl, dùng ảnh mặc định và email từ Auth để tạo URL tạm thời
                                String emailFromAuth = firebaseCurrentUser != null ? firebaseCurrentUser.getEmail() : "default";
                                Glide.with(this)
                                        .load("https://i.pravatar.cc/150?u=" + emailFromAuth)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(avatarImageView);
                            }
                        } else {
                            Log.d(TAG, "User data is null or UI components are null after fetching.");
                            if(textViewUserName != null) textViewUserName.setText("Thông tin trống");
                            if(textViewUserClass != null) textViewUserClass.setText("N/A");
                            if (avatarImageView != null) avatarImageView.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        Log.d(TAG, "User document does not exist for ID: " + currentUserId);
                        if(textViewUserName != null) textViewUserName.setText("Không tìm thấy người dùng");
                        if(textViewUserClass != null) textViewUserClass.setText("N/A");
                        if (avatarImageView != null) avatarImageView.setImageResource(R.drawable.default_avatar);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    if(progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(getContext(), "Lỗi tải thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                    // Fallback to default UI in case of error
                    if(textViewUserName != null) textViewUserName.setText("Lỗi tải");
                    if(textViewUserClass != null) textViewUserClass.setText("N/A");
                    if (avatarImageView != null) avatarImageView.setImageResource(R.drawable.default_avatar);
                });
    }

    private void loadUserProjects(String searchQuery) {
        // Trong chế độ khách, không tải dự án
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);
        if (isGuestMode || currentUserId == null || !isAdded() || getContext() == null) {
            Log.w(TAG, "Cannot load user projects: In Guest Mode or user not authenticated or fragment not ready.");
            if (progressBarProfile != null) progressBarProfile.setVisibility(View.GONE);
            userProjectList.clear(); // Xóa dữ liệu cũ
            if (userProjectsAdapter != null) userProjectsAdapter.notifyDataSetChanged();
            return;
        }
        if(progressBarProfile != null) progressBarProfile.setVisibility(View.VISIBLE);

        CollectionReference projectsRef = db.collection("Projects");
        Query query;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = projectsRef.whereEqualTo("CreatorUserId", currentUserId)
                    .orderBy("Title")
                    .whereGreaterThanOrEqualTo("Title", searchQuery)
                    .whereLessThanOrEqualTo("Title", searchQuery + "\uf8ff");
        } else {
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
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);
        if (isGuestMode) {
            if (getContext() != null) Toast.makeText(getContext(), "Chức năng chỉnh sửa không khả dụng ở chế độ khách.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() != null)
            Toast.makeText(getContext(), "Sửa dự án: " + project.getTitle() + " (chưa code)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(final Project project) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);
        if (isGuestMode) {
            if (getContext() != null) Toast.makeText(getContext(), "Chức năng xóa không khả dụng ở chế độ khách.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        // Tùy chọn: Quyết định xem chế độ khách có được xem chi tiết dự án không
        // Hiện tại, tôi sẽ cho phép xem, nhưng bạn có thể thêm kiểm tra như onDeleteClick nếu muốn hạn chế
        if (currentUserId == null) { // Vẫn kiểm tra currentUserId để phân biệt người dùng thật với khách (nếu cần phân biệt)
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
        } else {
            currentUserId = null;
        }

        // Kiểm tra lại chế độ khách khi Fragment hoạt động lại
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);

        if (isGuestMode) {
            showGuestModeUI(); // Hiển thị UI khách
        } else if (currentUserId != null) {
            loadUserProfile();
            if (searchEditText != null) {
                loadUserProjects(searchEditText.getText().toString().trim());
            } else {
                loadUserProjects(null);
            }
        } else {
            showNotLoggedInUI(); // Hiển thị UI chưa đăng nhập
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
        userProjectList = null;
        progressBarProfile = null;
    }
}