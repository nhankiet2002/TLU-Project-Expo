package com.cse441.tluprojectexpo.admin;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.NavigationUtil;
import com.cse441.tluprojectexpo.auth.LoginActivity;
import com.cse441.tluprojectexpo.fragment.HomeFragment;

public class Dashboard extends AppCompatActivity {

    private static final String TAG = "Dashboard";
    private LinearLayout btnLogout, btnUser, btnCensor, btnFeatured, btnCategory, btnBackHome, btnGoAdminProfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnLogout = (LinearLayout) findViewById(R.id.btn_logout);
        btnUser = (LinearLayout) findViewById(R.id.btn_user);
        btnCensor = (LinearLayout) findViewById(R.id.btn_censor);
        btnFeatured = (LinearLayout) findViewById(R.id.btn_featured);
        btnCategory = (LinearLayout) findViewById(R.id.btn_category);
        btnBackHome = (LinearLayout) findViewById(R.id.btn_back_home);
        btnGoAdminProfile = (LinearLayout) findViewById(R.id.btn_admin_profile);

        btnLogout.setOnClickListener(v -> NavigationUtil.navigateTo(this, LoginActivity.class));
        btnUser.setOnClickListener(v -> NavigationUtil.navigateTo(this, UserManagementPage.class));
        btnCensor.setOnClickListener(v -> NavigationUtil.navigateTo(this, CensorManagementPage.class));
        btnFeatured.setOnClickListener(v -> NavigationUtil.navigateTo(this, FeaturedManagementPage.class));
        btnCategory.setOnClickListener(v -> NavigationUtil.navigateTo(this, CatalogManagementPage.class));
        btnBackHome.setOnClickListener(v -> NavigationUtil.navigateTo(this, HomeFragment.class));
        //btnGoAdminProfile.setOnClickListener(v -> NavigationUtil.navigateTo(this, AdminProfilePage.class));
    }
}