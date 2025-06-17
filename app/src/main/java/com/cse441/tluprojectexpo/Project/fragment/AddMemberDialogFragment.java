// AddMemberDialogFragment.java
package com.cse441.tluprojectexpo.Project.fragment;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.Project.adapter.UserSearchAdapter; // THAY ĐỔI
import com.cse441.tluprojectexpo.model.User; // THAY ĐỔI
import java.util.ArrayList;
import java.util.List;

public class AddMemberDialogFragment extends DialogFragment implements UserSearchAdapter.OnUserClickListener { // THAY ĐỔI

    private static final String TAG = "AddMemberDialog";
    private TextInputEditText etSearchMember;
    private RecyclerView rvMembersList;
    private TextInputLayout tilSearchMember;
    private ProgressBar pbLoadingMembers;
    private UserSearchAdapter adapter; // THAY ĐỔI
    private List<User> allUsers = new ArrayList<>(); // THAY ĐỔI
    private FirebaseFirestore db;

    // Interface để gửi User đã chọn về CreateFragment
    public interface AddUserDialogListener { // Đổi tên interface
        void onUserSelected(User user);    // Đổi tên tham số
    }
    private AddUserDialogListener dialogListener; // Đổi tên biến

    public static AddMemberDialogFragment newInstance() {
        return new AddMemberDialogFragment();
    }

    // Đổi tên phương thức và tham số
    public void setDialogListener(AddUserDialogListener listener) {
        this.dialogListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_member, container, false);

        db = FirebaseFirestore.getInstance();

        etSearchMember = view.findViewById(R.id.et_search_member_dialog);
        rvMembersList = view.findViewById(R.id.rv_members_list_dialog);
        tilSearchMember = view.findViewById(R.id.til_search_member);
        pbLoadingMembers = view.findViewById(R.id.pb_loading_members);

        setupRecyclerView();
        fetchUsersFromFirestore(); // Đổi tên hàm

        if (tilSearchMember != null) {
            tilSearchMember.setEndIconOnClickListener(v -> dismiss());
        }

        etSearchMember.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    @Override
    public void onStart() {
        // ... (giữ nguyên code onStart)
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
        adapter = new UserSearchAdapter(getContext(), new ArrayList<>(), this); // THAY ĐỔI
        rvMembersList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMembersList.setAdapter(adapter);
        if (getContext() != null) {
            rvMembersList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        }
    }

    // Đổi tên hàm và logic để fetch Users
    private void fetchUsersFromFirestore() {
        pbLoadingMembers.setVisibility(View.VISIBLE);
        rvMembersList.setVisibility(View.GONE);

        // Giả sử collection người dùng của bạn tên là "Users" (khớp với model User.java)
        // và bạn muốn sắp xếp theo "FullName"
        db.collection("Users")
                .orderBy("FullName") // Đổi từ "fullName" thành "FullName" nếu model User.java dùng "FullName"
                .get()
                .addOnCompleteListener(task -> {
                    pbLoadingMembers.setVisibility(View.GONE);
                    rvMembersList.setVisibility(View.VISIBLE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        allUsers.clear();
                        List<User> tempUserList = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Lấy thông tin vai trò từ document User
                                // Giả sử bạn có một trường "UserRoles" (Array of Strings) trong document User
                                // Hoặc một trường "IsAdmin" (Boolean)
                                // Ví dụ: Kiểm tra nếu user có role "Admin"
                                boolean isAdmin = false;
                                if (document.contains("UserRoles")) { // Kiểm tra sự tồn tại của trường
                                    Object rolesObject = document.get("UserRoles");
                                    if (rolesObject instanceof List) {
                                        List<?> rolesRaw = (List<?>) rolesObject;
                                        List<String> roles = new ArrayList<>();
                                        for (Object roleRaw : rolesRaw) {
                                            if (roleRaw instanceof String) {
                                                roles.add((String) roleRaw);
                                            }
                                        }
                                        for (String roleId : roles) {
                                            // Bạn cần query collection "Roles" để lấy RoleName từ RoleId
                                            // Hoặc nếu "UserRoles" lưu trực tiếp tên vai trò (vd: "Admin", "User") thì:
                                            if ("role_admin".equalsIgnoreCase(roleId)) { // Giả sử "role_admin" là ID của Admin
                                                isAdmin = true;
                                                break;
                                            }
                                        }
                                    }
                                }


                                if (!isAdmin) {
                                    User user = document.toObject(User.class);
                                    // Quan trọng: Gán UserId từ ID của document
                                    user.setUserId(document.getId()); // THÊM DÒNG NÀY VÀO MODEL User.java
                                    tempUserList.add(user);
                                    Log.d(TAG, "Fetched non-admin user: " + user.getFullName());
                                } else {
                                    Log.d(TAG, "Skipped admin user: " + document.getString("FullName"));
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user document: " + document.getId(), e);
                            }
                        }
                        allUsers.addAll(tempUserList);

                        if (adapter != null) {
                            adapter.updateData(allUsers);
                            if (etSearchMember != null) {
                                adapter.filter(etSearchMember.getText().toString());
                            }
                        }
                    } else {
                        Log.w(TAG, "Lỗi khi tải danh sách người dùng.", task.getException());
                        if(getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi tải danh sách người dùng.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Đổi tên phương thức và tham số
    @Override
    public void onUserClick(User user) {
        if (dialogListener != null) {
            dialogListener.onUserSelected(user); // THAY ĐỔI
        }
        if (getContext() != null && user != null) {
            Toast.makeText(getContext(), "Đã chọn: " + user.getFullName(), Toast.LENGTH_SHORT).show(); // THAY ĐỔI
        }
        dismiss();
    }
}