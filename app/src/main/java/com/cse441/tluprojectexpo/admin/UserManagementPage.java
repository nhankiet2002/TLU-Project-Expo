package com.cse441.tluprojectexpo.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.adapter.UserManagementAdapter;
import com.cse441.tluprojectexpo.admin.repository.UserManagementRepository;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.model.User;

import java.util.ArrayList;
import java.util.List;

// Bước 1: Cho Activity implements interface từ Adapter
public class UserManagementPage extends AppCompatActivity implements UserManagementAdapter.OnUserSwitchListener, UserManagementAdapter.OnUserClickListener {

    private static final String TAG = "UserManagementPage";
    private static final int UPDATE_USER_REQUEST_CODE = 101;

    // Khai báo các thành phần UI và logic
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private UserManagementAdapter userAdapter;
    private List<User> userList;
    private UserManagementRepository userRepository;
    private ImageButton btnBackToDashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đảm bảo tên layout của bạn là chính xác
        setContentView(R.layout.activity_user_management_page);

        // Bước 2: Khởi tạo các thành phần theo đúng trình tự

        // 2.1. Ánh xạ các View từ layout
        // Đảm bảo các ID này tồn tại trong file activity_user_management_page.xml của bạn
        recyclerView = findViewById(R.id.recycler_view_user_management);
        progressBar = findViewById(R.id.progress_bar_loading); // Thêm một ProgressBar vào layout của bạn

        // 2.2. Khởi tạo các đối tượng logic và dữ liệu
        userRepository = new UserManagementRepository();
        userList = new ArrayList<>();
        userAdapter = new UserManagementAdapter(this, userList, this, this); // 'this' vì Activity implement listener

        // 2.3. Thiết lập cho RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter); // Gắn adapter vào RecyclerView

        // Bước 3: Bắt đầu tải dữ liệu
        Log.d(TAG, "onCreate: Bắt đầu tải dữ liệu người dùng.");
        loadUsers();

        btnBackToDashboard = (ImageButton) findViewById(R.id.back_to_dashboard);
        btnBackToDashboard.setOnClickListener(v -> NavigationUtil.navigateToDashboard(this));
    }

    /**
     * Yêu cầu UserManagementRepository lấy dữ liệu và xử lý kết quả trả về.
     */
    private void loadUsers() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // Hiển thị vòng xoay loading
        }

        userRepository.getAllUsersWithRoles(new UserManagementRepository.OnUsersDataChangedListener() {
            @Override
            public void onUsersLoaded(List<User> loadedUserList) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE); // Ẩn vòng xoay loading
                }
                Log.d(TAG, "onUsersLoaded: Đã nhận được " + loadedUserList.size() + " người dùng.");

                // Cập nhật dữ liệu cho Adapter
                userList.clear();
                userList.addAll(loadedUserList);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE); // Ẩn vòng xoay loading
                }
                Log.e(TAG, "onError: Lỗi khi tải dữ liệu", e);
                Toast.makeText(UserManagementPage.this, "Không thể tải danh sách người dùng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Đây là phương thức được gọi từ Adapter khi người dùng nhấn vào switch.
     * @param user Đối tượng User của item được nhấn.
     * @param newLockState Trạng thái isLocked MỚI cần được cập nhật.
     */
    @Override
    public void onUserLockStateChanged(User user, boolean newLockState) {
        Log.d(TAG, "onUserLockStateChanged: Yêu cầu thay đổi trạng thái của " + user.getFullName() + " thành isLocked = " + newLockState);
        Toast.makeText(this, "Đang cập nhật...", Toast.LENGTH_SHORT).show();

        // Yêu cầu UserManagementRepository cập nhật trạng thái lên Firestore
        userRepository.updateUserLockState(user.getUserId(), newLockState, new UserManagementRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(UserManagementPage.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                // Cập nhật trạng thái trong danh sách local để UI khớp ngay lập tức
                user.setLocked(newLockState);
                userAdapter.notifyDataSetChanged();
                // Không cần gọi notifyDataSetChanged() vì trạng thái của Switch đã tự thay đổi rồi.
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UserManagementPage.this, "Cập nhật thất bại!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "onFailure: Lỗi khi cập nhật trạng thái khóa", e);
                // Vì cập nhật thất bại, tải lại toàn bộ danh sách để đảm bảo
                // switch trên UI quay về đúng trạng thái trên server.
                loadUsers();
            }
        });
    }

    @Override
    public void onUserItemClicked(User user) {
        NavigationUtil.navigateWithObjectForResult(
                this,
                UserDetailManagementPage.class,
                "USER_DETAIL",
                user,
                UPDATE_USER_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_USER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Làm mới danh sách
                loadUsers();
            }
        }
    }
}