// AddMemberDialogFragment.java
package com.cse441.tluprojectexpo.ui.createproject; // Hoặc package đúng của bạn

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.ui.createproject.adapter.UserSearchAdapter;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.Constants; // **ĐẢM BẢO IMPORT ĐÚNG**

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import com.google.android.gms.tasks.Tasks; // Thêm Tasks để xử lý nhiều query

public class AddMemberDialogFragment extends DialogFragment implements UserSearchAdapter.OnUserClickListener {

    private static final String TAG = "AddMemberDialog";
    // ... (các biến khác giữ nguyên) ...
    private TextInputEditText etSearchMember;
    private RecyclerView rvMembersList;
    private TextInputLayout tilSearchMember;
    private ProgressBar pbLoadingMembers;
    private UserSearchAdapter adapter;
    private List<User> allUsersMasterList = new ArrayList<>();
    private FirebaseFirestore db;
    public interface AddUserDialogListener { void onUserSelected(User user); }
    private AddUserDialogListener dialogListener;

    public static AddMemberDialogFragment newInstance() { return new AddMemberDialogFragment(); }
    public void setDialogListener(AddUserDialogListener listener) { this.dialogListener = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ... (code onCreateView giữ nguyên, đảm bảo dialogListener được set) ...
        View view = inflater.inflate(R.layout.dialog_add_member, container, false);
        if (this.dialogListener == null) {
            if (getParentFragment() instanceof AddUserDialogListener) {
                this.dialogListener = (AddUserDialogListener) getParentFragment();
            } else if (getActivity() instanceof AddUserDialogListener && getParentFragment() == null) {
                this.dialogListener = (AddUserDialogListener) getActivity();
            }
        }
        db = FirebaseFirestore.getInstance();
        etSearchMember = view.findViewById(R.id.et_search_member_dialog);
        rvMembersList = view.findViewById(R.id.rv_members_list_dialog);
        tilSearchMember = view.findViewById(R.id.til_search_member);
        pbLoadingMembers = view.findViewById(R.id.pb_loading_members);
        setupRecyclerView();
        fetchValidUsers(); // Đổi tên hàm
        if (tilSearchMember != null) {
            tilSearchMember.setEndIconOnClickListener(v -> dismiss());
        }
        etSearchMember.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        return view;
    }

    // onStart và setupRecyclerView giữ nguyên
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int dialogWidth = (int)(displayMetrics.widthPixels * 0.90);
                window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }
    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(getContext(), new ArrayList<>(), this);
        rvMembersList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMembersList.setAdapter(adapter);
        if (getContext() != null) {
            rvMembersList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        }
    }


    private void fetchValidUsers() {
        pbLoadingMembers.setVisibility(View.VISIBLE);
        rvMembersList.setVisibility(View.GONE);

        // ---- LỰA CHỌN 1: Nếu vai trò và isLocked nằm trực tiếp trong Users collection ----
        // (Bỏ comment đoạn này và comment đoạn LỰA CHỌN 2 nếu dùng cách này)
        /*
        db.collection(Constants.COLLECTION_USERS)
                // Giả sử bạn có trường "Role" và "isLocked" trong Users
                // .whereEqualTo("Role", Constants.ROLE_ID_USER) // Hoặc tên vai trò user, ví dụ "user"
                .whereEqualTo(Constants.FIELD_IS_LOCKED, false) // Chỉ lấy user không bị khóa
                .orderBy(Constants.FIELD_FULL_NAME)
                .get()
                .addOnCompleteListener(task -> {
                    pbLoadingMembers.setVisibility(View.GONE);
                    rvMembersList.setVisibility(View.VISIBLE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        allUsersMasterList.clear();
                        List<User> tempUserList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                User user = document.toObject(User.class);
                                user.setUserId(document.getId());
                                if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                                    tempUserList.add(user);
                                } else {
                                    Log.w(TAG, "Skipped user with null or empty FullName. ID: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user document: " + document.getId(), e);
                            }
                        }
                        allUsersMasterList.addAll(tempUserList);
                        if (adapter != null) {
                            adapter.updateData(new ArrayList<>(allUsersMasterList));
                            if (etSearchMember != null && etSearchMember.getText() != null) {
                                adapter.filter(etSearchMember.getText().toString());
                            }
                        }
                    } else {
                        Log.w(TAG, "Error fetching valid users: ", task.getException());
                        if(getContext() != null) AppToast.show(getContext(), "Lỗi tải danh sách người dùng.", Toast.LENGTH_SHORT);
                    }
                });
        return; // Dừng ở đây nếu dùng Lựa chọn 1
        */


        // ---- LỰA CHỌN 2: Nếu vai trò nằm trong UserRoles và isLocked trong Users (Phức tạp hơn) ----
        // Bước 1: Lấy danh sách UserId của Admin
        Task<QuerySnapshot> getAdminUserIdsTask = db.collection(Constants.COLLECTION_USER_ROLES)
                .whereEqualTo(Constants.FIELD_ROLE_ID, Constants.ROLE_ID_ADMIN)
                .get();

        // Bước 2: Lấy danh sách UserId của những người bị khóa
        Task<QuerySnapshot> getLockedUserIdsTask = db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_IS_LOCKED, true)
                .get();

        // Bước 3: Lấy danh sách UserId của những người có vai trò "user" (nếu bạn quản lý vai trò user riêng)
        // Nếu không có vai trò user cụ thể mà chỉ cần loại admin, bạn có thể bỏ qua task này
        // và logic lọc ở dưới sẽ chỉ cần không phải admin.
        Task<QuerySnapshot> getUserRoleUserIdsTask = db.collection(Constants.COLLECTION_USER_ROLES)
                .whereEqualTo(Constants.FIELD_ROLE_ID, Constants.ROLE_ID_USER) // Giả sử có ROLE_ID_USER
                .get();


        // Kết hợp các task
        Tasks.whenAllSuccess(getAdminUserIdsTask, getLockedUserIdsTask, getUserRoleUserIdsTask)
                .addOnSuccessListener(results -> {
                    Set<String> adminUserIds = new HashSet<>();
                    if (results.get(0) instanceof QuerySnapshot) {
                        QuerySnapshot adminResult = (QuerySnapshot) results.get(0);
                        for (QueryDocumentSnapshot document : adminResult) {
                            String userId = document.getString(Constants.FIELD_USER_ID);
                            if (userId != null) adminUserIds.add(userId);
                        }
                    }
                    Log.d(TAG, "Admin UserIds: " + adminUserIds);

                    Set<String> lockedUserIds = new HashSet<>();
                    if (results.get(1) instanceof QuerySnapshot) {
                        QuerySnapshot lockedResult = (QuerySnapshot) results.get(1);
                        for (QueryDocumentSnapshot document : lockedResult) {
                            lockedUserIds.add(document.getId()); // ID của document trong Users collection
                        }
                    }
                    Log.d(TAG, "Locked UserIds: " + lockedUserIds);

                    Set<String> userRoleUserIds = new HashSet<>();
                    // Nếu bạn có task lấy user có vai trò "user"
                    if (results.size() > 2 && results.get(2) instanceof QuerySnapshot) {
                        QuerySnapshot userRoleResult = (QuerySnapshot) results.get(2);
                        for (QueryDocumentSnapshot document : userRoleResult) {
                            String userId = document.getString(Constants.FIELD_USER_ID);
                            if (userId != null) userRoleUserIds.add(userId);
                        }
                    }
                    Log.d(TAG, "User role UserIds: " + userRoleUserIds);


                    // Bước 4: Lấy tất cả user và lọc
                    db.collection(Constants.COLLECTION_USERS)
                            .orderBy(Constants.FIELD_FULL_NAME)
                            .get()
                            .addOnCompleteListener(allUsersTask -> {
                                pbLoadingMembers.setVisibility(View.GONE);
                                rvMembersList.setVisibility(View.VISIBLE);
                                if (allUsersTask.isSuccessful() && allUsersTask.getResult() != null) {
                                    allUsersMasterList.clear();
                                    List<User> tempUserList = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : allUsersTask.getResult()) {
                                        String currentUserId = document.getId();

                                        // Kiểm tra điều kiện:
                                        // 1. Không phải Admin
                                        // 2. Không bị khóa
                                        // 3. (Tùy chọn) Phải có vai trò "user" nếu bạn query UserRoles cho vai trò user
                                        boolean isUserRole = userRoleUserIds.isEmpty() || userRoleUserIds.contains(currentUserId); // Nếu userRoleUserIds rỗng, coi như mọi non-admin đều là user

                                        if (!adminUserIds.contains(currentUserId) &&
                                                !lockedUserIds.contains(currentUserId) &&
                                                isUserRole) { // Thêm điều kiện isUserRole
                                            try {
                                                User user = document.toObject(User.class);
                                                user.setUserId(currentUserId);
                                                if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                                                    tempUserList.add(user);
                                                } else {
                                                    Log.w(TAG, "Skipped user with null or empty FullName. ID: " + document.getId());
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error processing user document: " + document.getId(), e);
                                            }
                                        } else {
                                            Log.d(TAG, "Skipping user: " + document.getString(Constants.FIELD_FULL_NAME) + " (ID: " + currentUserId + ") due to admin/locked/role mismatch.");
                                        }
                                    }
                                    allUsersMasterList.addAll(tempUserList);
                                    if (adapter != null) {
                                        adapter.updateData(new ArrayList<>(allUsersMasterList));
                                        if (etSearchMember != null && etSearchMember.getText() != null) {
                                            adapter.filter(etSearchMember.getText().toString());
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Error fetching all users: ", allUsersTask.getException());
                                    if(getContext() != null) AppToast.show(getContext(), "Lỗi tải danh sách người dùng.", Toast.LENGTH_SHORT);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    pbLoadingMembers.setVisibility(View.GONE);
                    rvMembersList.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error fetching initial role/lock data: ", e);
                    if(getContext() != null) AppToast.show(getContext(), "Lỗi tải dữ liệu người dùng.", Toast.LENGTH_SHORT);
                });
    }


    @Override
    public void onUserClick(User user) {
        // ... (code trong onUserClick giữ nguyên) ...
        if (dialogListener == null) {
            Log.e(TAG, "AddUserDialogListener is null in onUserClick.");
            if (getContext() != null) AppToast.show(getContext(), "Lỗi: Không thể xử lý lựa chọn.", Toast.LENGTH_SHORT);
            dismiss(); return;
        }
        if (user != null && user.getUserId() != null) {
            Log.d(TAG, "User selected: " + user.getFullName() + " with ID: " + user.getUserId());
            dialogListener.onUserSelected(user);
            if (getContext() != null) AppToast.show(getContext(), "Đã chọn: " + user.getFullName(), Toast.LENGTH_SHORT);
            dismiss();
        } else {
            Log.e(TAG, "User or UserID is null onUserClick.");
            if (getContext() != null) AppToast.show(getContext(), "Lỗi: Không thể chọn người dùng này.", Toast.LENGTH_SHORT);
        }
    }
}