package com.cse441.tluprojectexpo.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.auth.LoginActivity;
import com.cse441.tluprojectexpo.auth.SettingProfileActivity;
import com.google.firebase.auth.FirebaseAuth;

public class Dashboard extends AppCompatActivity {

    private static final String TAG = "Dashboard";
    private FirebaseAuth mAuth; // Khai báo FirebaseAuth
    private LinearLayout btnLogout, btnUser, btnCensor, btnFeatured, btnCategory, btnBackHome, btnGoAdminProfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth

        btnLogout = (LinearLayout) findViewById(R.id.btn_logout);
        btnUser = (LinearLayout) findViewById(R.id.btn_user);
        btnCensor = (LinearLayout) findViewById(R.id.btn_censor);
        btnFeatured = (LinearLayout) findViewById(R.id.btn_featured);
        btnCategory = (LinearLayout) findViewById(R.id.btn_category);
        btnBackHome = (LinearLayout) findViewById(R.id.btn_back_home);
        btnGoAdminProfile = (LinearLayout) findViewById(R.id.btn_admin_profile);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Đăng xuất khỏi Firebase
            LoginActivity.clearRememberMePreferences(this); // Xóa thông tin "nhớ tài khoản"
            // Chuyển hướng đến LoginActivity và xóa toàn bộ back stack
            Intent intent = new Intent(Dashboard.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Kết thúc Dashboard activity
        });
        btnUser.setOnClickListener(v -> NavigationUtil.navigateTo(this, UserManagementPage.class));
        btnCensor.setOnClickListener(v -> NavigationUtil.navigateTo(this, CensorManagementPage.class));
        btnFeatured.setOnClickListener(v -> NavigationUtil.navigateTo(this, FeaturedManagementPage.class));
        btnCategory.setOnClickListener(v -> NavigationUtil.navigateTo(this, CatalogManagementPage.class));
        btnBackHome.setOnClickListener(v -> NavigationUtil.navigateTo(this, AdminHomePage.class));
        btnGoAdminProfile.setOnClickListener(v -> NavigationUtil.navigateTo(this, SettingProfileActivity.class));
    }
}