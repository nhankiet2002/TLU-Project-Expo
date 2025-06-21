package com.cse441.tluprojectexpo.admin.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.Role;
import com.cse441.tluprojectexpo.model.User;

import java.util.List;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;
    private final OnUserSwitchListener switchListener;
    private final OnUserClickListener clickListener;

    // Interface để gửi sự kiện click switch về Activity/Fragment
    public interface OnUserSwitchListener {
        void onUserLockStateChanged(User user, boolean isLocked);
    }

    public interface OnUserClickListener {
        void onUserItemClicked(User user);
    }

    // Constructor
    public UserManagementAdapter(Context context, List<User> userList, OnUserSwitchListener switchListener, OnUserClickListener clickListener) {
        this.context = context;
        this.userList = userList;
        this.switchListener = switchListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng tên file layout item của bạn ở đây
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User currentUser = userList.get(position);
        holder.bind(currentUser);
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    // ViewHolder class
    class UserViewHolder extends RecyclerView.ViewHolder {

        TextView txtIsLocked, txtEmail, txtRole, txtUserName;
        SwitchCompat lockSwitch;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.user_name);
            txtEmail = itemView.findViewById(R.id.email);
            txtRole = itemView.findViewById(R.id.role);
            txtIsLocked = itemView.findViewById(R.id.is_locked);
            lockSwitch = itemView.findViewById(R.id.lock_switch);
        }

        @SuppressLint("ResourceAsColor")
        public void bind(final User user) {
            // 1. Binding dữ liệu cơ bản (Giữ nguyên)
            txtUserName.setText(user.getFullName());
            txtIsLocked.setText(user.getFullName());
            txtEmail.setText(user.getEmail());

            // 2. SỬA LẠI: Xử lý hiển thị vai trò từ đối tượng Role
            Role userRole = user.getRole(); // Lấy đối tượng Role đã được gán từ UserRepository

            // Luôn kiểm tra null để tránh crash ứng dụng
            if (userRole != null) {
                // Nếu user có role, hiển thị tên của nó
                txtRole.setText(userRole.getRoleName());
            } else {
                // Nếu user không có txtRole, hiển thị một thông báo mặc định
                txtRole.setText("Chưa có vai trò");
            }


            if (user.isLocked()) {
                txtIsLocked.setText(R.string.locked);
                // Thiết lập giao diện cho trạng thái "Locked"
                txtIsLocked.setTextColor(ContextCompat.getColor(context, R.color.locked));
                txtIsLocked.setBackgroundResource(R.drawable.cr12beac6c6);
            } else {
                txtIsLocked.setText(R.string.active);
                // Thiết lập giao diện cho trạng thái "Active"
                txtIsLocked.setTextColor(ContextCompat.getColor(context, R.color.active));
                txtIsLocked.setBackgroundResource(R.drawable.cr12bc9eac6);
            }

            // 4. SỬA LẠI: Logic của Switch để an toàn và chính xác hơn
            // Xóa listener cũ để tránh các sự kiện không mong muốn khi view được tái sử dụng
            lockSwitch.setOnCheckedChangeListener(null);

            // Đặt trạng thái cho switch. Dòng này là đủ, không cần đặt trong if/else ở trên.
            // Nếu user KHÔNG bị khóa (isLocked = false), thì switch phải được BẬT (checked = true).
            lockSwitch.setChecked(!user.isLocked());

            // Gán listener mới
            lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Thêm điều kiện `isPressed()` để đảm bảo code chỉ chạy khi người dùng
                // thực sự nhấn vào switch, không phải khi code tự động setChecked.
                if (buttonView.isPressed()) {
                    if (switchListener != null) {
                        // isChecked là trạng thái MỚI của switch.
                        // isChecked = true (Bật)  -> isLocked phải là false
                        // isChecked = false (Tắt) -> isLocked phải là true
                        switchListener.onUserLockStateChanged(user, !isChecked);
                    }
                }
            });

            // Gán sự kiện click cho toàn bộ item view
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    // Gọi phương thức trong interface và truyền vào user của item này
                    clickListener.onUserItemClicked(user);
                }
            });
        }
    }

    // (Optional) Một hàm helper để cập nhật dữ liệu cho adapter
    public void updateData(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }
}
