package com.cse441.tluprojectexpo.ui.common.uimanager;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.utils.UiHelper;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.User;
import com.google.android.material.textfield.TextInputLayout; // Thêm nếu chưa có
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;
import java.util.Map;

public class SelectedMembersUiManager {

    private Context context;
    private LayoutInflater inflater;
    private LinearLayout container;
    private List<User> selectedUsers;
    private Map<String, String> userRolesInProject; // UserId -> Role
    private String[] roleItems; // Danh sách vai trò từ resources
    private OnMemberInteractionListener listener;

    public interface OnMemberInteractionListener {
        void onMemberRemoved(User user, int index);
        void onMemberRoleChanged(User user, String newRole, int index);
    }

    public SelectedMembersUiManager(Context context, LinearLayout container,
                                    List<User> selectedUsers, Map<String, String> userRolesInProject,
                                    OnMemberInteractionListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.container = container;
        this.selectedUsers = selectedUsers;
        this.userRolesInProject = userRolesInProject;
        this.roleItems = context.getResources().getStringArray(R.array.member_roles);
        this.listener = listener;
    }

    public void updateUI() {
        if (container == null) return;
        container.removeAllViews();
        if (selectedUsers.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);

        for (int i = 0; i < selectedUsers.size(); i++) {
            User user = selectedUsers.get(i);
            if (user == null || user.getUserId() == null) continue;

            View memberView = inflater.inflate(R.layout.item_selected_member, container, false);
            ImageView ivAvatar = memberView.findViewById(R.id.iv_selected_member_avatar);
            TextView tvName = memberView.findViewById(R.id.tv_selected_member_name);
            TextView tvClass = memberView.findViewById(R.id.tv_selected_member_class);
            AutoCompleteTextView actvRole = memberView.findViewById(R.id.actv_member_role);
            ImageView ivRemove = memberView.findViewById(R.id.iv_remove_member);
            TextInputLayout tilRole = memberView.findViewById(R.id.til_member_role);

            tvName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
            tvClass.setText(user.getClassName() != null ? user.getClassName() : "N/A");
            Glide.with(context).load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop().into(ivAvatar);

            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_dropdown_item_1line, roleItems);
            actvRole.setAdapter(roleAdapter);

            String currentRole = userRolesInProject.get(user.getUserId());
            if (currentRole != null && !currentRole.isEmpty()) {
                actvRole.setText(currentRole, false);
            } else {
                String defaultRole = "Thành viên";
                FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser(); // Cẩn thận với việc gọi static này
                if (fbUser != null && user.getUserId().equals(fbUser.getUid()) && i == 0) {
                    defaultRole = "Trưởng nhóm";
                }
                actvRole.setText(defaultRole, false);
                if (listener != null) listener.onMemberRoleChanged(user, defaultRole, i); // Thông báo vai trò mặc định
            }

            final int userIndex = i;
            actvRole.setOnItemClickListener((parent, MView, position, id) -> {
                if (userIndex < selectedUsers.size() && listener != null) { // Kiểm tra index
                    String selectedRole = parent.getItemAtPosition(position).toString();
                    listener.onMemberRoleChanged(selectedUsers.get(userIndex), selectedRole, userIndex);
                }
            });

            if (tilRole != null) {
                UiHelper.setupDropdownToggle(tilRole, actvRole);
            }

            FirebaseUser fbCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean isCurrentUserAndOnlyLeader = selectedUsers.size() == 1 &&
                    fbCurrentUser != null &&
                    user.getUserId().equals(fbCurrentUser.getUid()) &&
                    "Trưởng nhóm".equals(userRolesInProject.get(user.getUserId()));

            if (isCurrentUserAndOnlyLeader) {
                ivRemove.setVisibility(View.GONE);
            } else {
                ivRemove.setVisibility(View.VISIBLE);
                ivRemove.setOnClickListener(v_remove -> {
                    if (userIndex < selectedUsers.size() && listener != null) { // Kiểm tra index
                        listener.onMemberRemoved(selectedUsers.get(userIndex), userIndex);
                    }
                });
            }
            container.addView(memberView);
        }
    }
} 