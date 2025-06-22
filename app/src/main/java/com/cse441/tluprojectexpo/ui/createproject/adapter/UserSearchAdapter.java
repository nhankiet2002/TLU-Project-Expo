// UserSearchAdapter.java
package com.cse441.tluprojectexpo.ui.createproject.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.User; // THAY ĐỔI TỪ Member sang User
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private Context context;
    private List<User> userListFull; // Danh sách đầy đủ ban đầu
    private List<User> userListFiltered; // Danh sách đã được lọc để hiển thị

    public interface OnUserClickListener { // Đổi tên interface
        void onUserClick(User user);    // Đổi tên tham số
    }
    private OnUserClickListener listener;

    public UserSearchAdapter(Context context, List<User> userList, OnUserClickListener listener) {
        this.context = context;
        this.userListFull = new ArrayList<>(userList);
        this.userListFiltered = new ArrayList<>(userList);
        this.listener = listener;
    }

    public void updateData(List<User> newUsers) {
        this.userListFull.clear();
        this.userListFull.addAll(newUsers);
        filter(""); // Hiển thị tất cả khi cập nhật
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member_search, parent, false); // Vẫn dùng layout item_member_search
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userListFiltered.get(position); // THAY ĐỔI
        holder.tvMemberName.setText(user.getFullName()); // Sử dụng getFullName()
        holder.tvMemberClass.setText(user.getClassName()); // Sử dụng getUserClass()

        Glide.with(context)
                .load(user.getAvatarUrl()) // Sử dụng getAvatarUrl()
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(holder.ivMemberAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user); // THAY ĐỔI
            }
        });
    }

    @Override
    public int getItemCount() {
        return userListFiltered.size();
    }

    public void filter(String query) {
        userListFiltered.clear();
        if (TextUtils.isEmpty(query)) {
            userListFiltered.addAll(userListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (User user : userListFull) { // THAY ĐỔI
                // Tìm kiếm theo tên hoặc lớp
                if ((user.getFullName() != null && user.getFullName().toLowerCase().contains(filterPattern)) ||
                        (user.getClassName() != null && user.getClassName().toLowerCase().contains(filterPattern))) {
                    userListFiltered.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder { // Đổi tên ViewHolder
        CircleImageView ivMemberAvatar;
        TextView tvMemberName;
        TextView tvMemberClass;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberAvatar = itemView.findViewById(R.id.iv_member_avatar_search);
            tvMemberName = itemView.findViewById(R.id.tv_member_name_search);
            tvMemberClass = itemView.findViewById(R.id.tv_member_class_search);
        }
    }
}