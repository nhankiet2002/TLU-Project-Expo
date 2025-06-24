package com.cse441.tluprojectexpo.admin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.repository.RoleRepository;
import com.cse441.tluprojectexpo.admin.repository.UserManagementRepository;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.Role;
import com.cse441.tluprojectexpo.model.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDetailManagementPage extends AppCompatActivity {

    private TextView txtUserName, txtClassName, txtEmail;
    private Spinner spinnerRole, spinnerStatus;
    private ShapeableImageView imgAvatar;
    private Button btnSaveChanges;
    private ImageButton back;

    // --- Dữ liệu và Repositories ---
    private User currentUser;
    private List<Role> allRolesList = new ArrayList<>();
    private RoleRepository roleRepository;
    private UserManagementRepository userManagementRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng đúng tên file layout của bạn
        setContentView(R.layout.activity_user_detail_management_page);

        // Khởi tạo các repository
        roleRepository = new RoleRepository();
        userManagementRepository = new UserManagementRepository();

        bindViews();
        retrieveUserData();

        // Nếu không có dữ liệu user, đóng activity
        if (currentUser == null) {
            AppToast.show(this, "Không thể tải dữ liệu người dùng.", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        // Bắt đầu quá trình hiển thị dữ liệu
        populateDataToViews();
        loadAndObserveRoles(); // Tải dữ liệu động cho Spinner Role
        setupClickListeners();
    }

    private void bindViews() {
        // TextViews
        txtUserName = findViewById(R.id.user_name2);
        txtClassName = findViewById(R.id.class_name2);
        txtEmail = findViewById(R.id.email2);

        // Spinners
        spinnerRole = findViewById(R.id.role_input);
        spinnerStatus = findViewById(R.id.status_input);

        // ImageView
        imgAvatar = findViewById(R.id.avatar);

        // Buttons and Navigation
        btnSaveChanges = findViewById(R.id.btn_save_infor);
        back = findViewById(R.id.back_from_censor);
    }

    private void retrieveUserData() {
        if (getIntent() != null && getIntent().hasExtra("USER_DETAIL")) {
            // Giả sử bạn dùng Serializable, nếu là Parcelable thì dùng getParcelableExtra
            currentUser = (User) getIntent().getSerializableExtra("USER_DETAIL");
        }
    }

    /**
     * Điền các dữ liệu tĩnh hoặc có sẵn ngay lập tức từ đối tượng User.
     */
    private void populateDataToViews() {
        // 1. Điền thông tin vào các TextView
        txtUserName.setText(orDefault(currentUser.getFullName(), "Chưa có thông tin"));
        txtEmail.setText(orDefault(currentUser.getEmail(), "Chưa có thông tin"));

        // Giả sử User có các phương thức này, nếu không, bạn cần thêm chúng vào
        txtClassName.setText(orDefault(currentUser.getClassName(), "Chưa có thông tin"));

        // 2. Tải ảnh đại diện bằng Glide
        Glide.with(this)
                .load(currentUser.getAvatarUrl()) // URL ảnh của user
                .placeholder(R.drawable.image7)   // Ảnh hiển thị khi đang tải
                .error(R.drawable.image7)         // Ảnh hiển thị khi có lỗi
                .into(imgAvatar);

        // 3. Cài đặt cho Spinner Trạng thái (dữ liệu tĩnh)
        setupStatusSpinner();
    }

    /**
     * Tải danh sách vai trò từ Firestore và cài đặt cho Spinner Role.
     */
    private void loadAndObserveRoles() {
        roleRepository.getAllRoles().observe(this, roles -> {
            if (roles != null && !roles.isEmpty()) {
                allRolesList.clear();
                allRolesList.addAll(roles);
                setupRoleSpinner(); // Cài đặt Spinner sau khi có dữ liệu
            } else {
                AppToast.show(this, "Không thể tải danh sách vai trò", Toast.LENGTH_SHORT);
            }
        });
    }

    private void setupRoleSpinner() {
        // Tạo danh sách tên vai trò để hiển thị
        List<String> roleNames = new ArrayList<>();
        for (Role role : allRolesList) {
            roleNames.add(role.getRoleName());
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleNames);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // TỰ ĐỘNG CHỌN ĐÚNG VAI TRÒ HIỆN TẠI CỦA USER
        if (currentUser.getRole() != null) {
            String userRoleName = currentUser.getRole().getRoleName();
            int position = roleNames.indexOf(userRoleName);
            if (position >= 0) {
                spinnerRole.setSelection(position);
            }
        }
    }

    private void setupStatusSpinner() {
        // Dùng từ trong resource để dễ dàng dịch thuật sau này
        String active = getString(R.string.active); // "Mở khóa" hoặc "Active"
        String locked = getString(R.string.locked); // "Khóa" hoặc "Locked"
        List<String> statuses = Arrays.asList(active, locked);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // TỰ ĐỘNG CHỌN ĐÚNG TRẠNG THÁI HIỆN TẠI CỦA USER
        if (currentUser.isLocked()) {
            spinnerStatus.setSelection(1); // "Khóa" ở vị trí 1
        } else {
            spinnerStatus.setSelection(0); // "Mở khóa" ở vị trí 0
        }
    }

    private void setupClickListeners() {
        // Sự kiện cho nút quay lại
        back.setOnClickListener(v -> finish());

        // Sự kiện cho nút Lưu thay đổi
        btnSaveChanges.setOnClickListener(v -> saveUserInformation());
    }

    private void saveUserInformation() {
        // Lấy dữ liệu đã chọn từ Spinner
        int selectedRolePosition = spinnerRole.getSelectedItemPosition();
        if (selectedRolePosition < 0 || selectedRolePosition >= allRolesList.size()) {
            AppToast.show(this, "Vui lòng chọn vai trò hợp lệ.", Toast.LENGTH_SHORT);
            return;
        }
        Role selectedRole = allRolesList.get(selectedRolePosition);

        // Xác định trạng thái isLocked từ vị trí của Spinner Status
        boolean isLocked = spinnerStatus.getSelectedItemPosition() == 1; // 1 là vị trí của "Khóa"
        String userId = currentUser.getUserId();
        // --- THÊM CÁC DÒNG LOG NÀY ĐỂ KIỂM TRA ---
        Log.d("DEBUG_SAVE", "Attempting to save for User ID: " + userId);
        Log.d("DEBUG_SAVE", "Selected Role Name: " + selectedRole.getRoleName());
        Log.d("DEBUG_SAVE", "Selected Role ID: " + selectedRole.getRoleId());
        Log.d("DEBUG_SAVE", "Selected Status (isLocked): " + isLocked);
        // ---------------------------------------------


        // Gọi repository để cập nhật
        userManagementRepository.updateUserRoleAndStatus(userId, selectedRole, isLocked, new UserManagementRepository.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                AppToast.show(UserDetailManagementPage.this, "Đã cập nhật thành công!", Toast.LENGTH_SHORT);
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(UserDetailManagementPage.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * Một hàm tiện ích nhỏ để trả về giá trị mặc định nếu chuỗi gốc là null hoặc rỗng.
     */
    private String orDefault(String original, String defaultValue) {
        return (original != null && !original.isEmpty()) ? original : defaultValue;
    }
}