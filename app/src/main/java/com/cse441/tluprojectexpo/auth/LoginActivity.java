// PATH: com/cse441/tluprojectexpo/auth/LoginActivity.java
package com.cse441.tluprojectexpo.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cse441.tluprojectexpo.MainActivity; // Màn hình chính cho người dùng thường
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.Dashboard; // Màn hình Dashboard cho Admin
import com.cse441.tluprojectexpo.model.UserRole;
import com.cse441.tluprojectexpo.utils.GuestModeHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edEmailLogin, edPasswordLogin;
    private Button btnLogin;
    private TextView txtRegister, txtForgotPassword, txtGuestMode;
    private CheckBox cbRememberMe;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "LoginActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edEmailLogin = findViewById(R.id.edEmailLogin);
        edPasswordLogin = findViewById(R.id.edPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        progressBar = findViewById(R.id.progressBar);
        txtGuestMode = findViewById(R.id.txtGuestMode);

        loadRememberMePreferences();

        btnLogin.setOnClickListener(v -> loginUser());

        txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        if (txtGuestMode != null) {
            txtGuestMode.setOnClickListener(v -> {
                GuestModeHandler.enterGuestMode(LoginActivity.this, mAuth);
                finish();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadRememberMePreferences() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMe = preferences.getBoolean(KEY_REMEMBER_ME, false);
        cbRememberMe.setChecked(rememberMe);

        if (rememberMe) {
            String savedEmail = preferences.getString(KEY_EMAIL, "");
            String savedPassword = preferences.getString(KEY_PASSWORD, "");
            edEmailLogin.setText(savedEmail);
            edPasswordLogin.setText(savedPassword);
        }
    }

    private void saveRememberMePreferences(String email, String password) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
        } else {
            editor.putBoolean(KEY_REMEMBER_ME, false);
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
        }
        editor.apply();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loginUser() {
        hideKeyboard();
        String email = edEmailLogin.getText().toString().trim();
        String password = edPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Bắt đầu kiểm tra trạng thái khóa tài khoản
                                db.collection("Users").document(user.getUid()).get()
                                        .addOnCompleteListener(userDocTask -> {
                                            if (userDocTask.isSuccessful()) {
                                                DocumentSnapshot document = userDocTask.getResult();
                                                if (document.exists()) {
                                                    Boolean isLocked = document.getBoolean("IsLocked");
                                                    // Mặc định không khóa nếu trường IsLocked không tồn tại hoặc null
                                                    if (isLocked != null && isLocked) {
                                                        // Tài khoản bị khóa
                                                        mAuth.signOut(); // Đăng xuất người dùng ngay lập tức
                                                        hideProgressBar();
                                                        Toast.makeText(LoginActivity.this, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_LONG).show();
                                                        Log.d(TAG, "Tài khoản bị khóa: " + user.getUid());
                                                    } else {

                                                        // Tài khoản không bị khóa, tiếp tục logic thông thường
                                                        Log.d(TAG, "Đăng nhập thành công.");
                                                        saveRememberMePreferences(email, password);

                                                        if (!user.isEmailVerified()) {
                                                            Toast.makeText(LoginActivity.this, "Email của bạn chưa được xác thực. Một số tính năng có thể bị hạn chế. Vui lòng kiểm tra email để xác thực tài khoản.", Toast.LENGTH_LONG).show();
                                                        }
                                                        checkUserRoleAndNavigate(user.getUid());
                                                    }
                                                } else {
                                                    // Document người dùng không tồn tại trong Firestore
                                                    // Điều này có thể xảy ra nếu người dùng được tạo qua Authentication nhưng chưa có document trong Users collection
                                                    Log.e(TAG, "Không tìm thấy thông tin người dùng trong Firestore cho UID: " + user.getUid());
                                                    mAuth.signOut(); // Đăng xuất để tránh trạng thái không nhất quán
                                                    hideProgressBar();
                                                    Toast.makeText(LoginActivity.this, "Lỗi: Không thể truy xuất thông tin tài khoản. Vui lòng thử lại hoặc liên hệ hỗ trợ.", Toast.LENGTH_LONG).show();
                                                }
                                            } else {
                                                // Lỗi khi truy vấn Firestore
                                                Log.e(TAG, "Lỗi khi lấy trạng thái khóa từ Firestore: " + userDocTask.getException().getMessage());
                                                mAuth.signOut(); // Đăng xuất để tránh trạng thái không nhất quán
                                                hideProgressBar();
                                                Toast.makeText(LoginActivity.this, "Lỗi: Không thể kiểm tra trạng thái tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                                            }
                                        });

                            } else {
                                hideProgressBar();
                                Toast.makeText(LoginActivity.this, "Người dùng không tồn tại.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            hideProgressBar();
                            String errorMessage;
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Mật khẩu không đúng.";
                            } else {
                                errorMessage = "Đăng nhập thất bại: " + task.getException().getMessage();
                            }
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkUserRoleAndNavigate(String userId) {
        db.collection("UserRoles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    hideProgressBar(); // Ẩn ProgressBar ngay sau khi task hoàn thành

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String userRoleName = null;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserRole userRole = document.toObject(UserRole.class);
                            userRoleName = userRole.getRole();
                            Log.d(TAG, "Fetched Role Name from UserRoles: " + userRoleName);
                            break;
                        }

                        if (userRoleName != null) {
                            if ("role_admin".equalsIgnoreCase(userRoleName)) {
                                Log.d(TAG, "Navigating to Admin Dashboard.");
                                navigateToAdminDashboard();
                            } else {
                                Log.d(TAG, "Navigating to Main Activity.");
                                navigateToMainActivity();
                            }
                        } else {
                            Log.e(TAG, "Lỗi: Không tìm thấy trường 'role' trong UserRole cho userId: " + userId);
                            Toast.makeText(LoginActivity.this, "Lỗi: Không tìm thấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG).show();
                            navigateToMainActivity();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy UserRole từ Firestore hoặc không tìm thấy UserRole cho userId: " + userId, task.getException());
                        Toast.makeText(LoginActivity.this, "Lỗi: Không thể lấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG).show();
                        navigateToMainActivity();
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToAdminDashboard() {
        Intent intent = new Intent(LoginActivity.this, Dashboard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnLogin.setEnabled(false);
        edEmailLogin.setEnabled(false);
        edPasswordLogin.setEnabled(false);
        txtRegister.setEnabled(false);
        txtForgotPassword.setEnabled(false);
        cbRememberMe.setEnabled(false);
        if (txtGuestMode != null) txtGuestMode.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnLogin.setEnabled(true);
        edEmailLogin.setEnabled(true);
        edPasswordLogin.setEnabled(true);
        txtRegister.setEnabled(true);
        txtForgotPassword.setEnabled(true);
        cbRememberMe.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true);
    }
}