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

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.AdminHomePage;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
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
import com.google.firebase.firestore.DocumentSnapshot;
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
        initializeUIAndListeners(); // Vẫn giữ nguyên khởi tạo UI

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load các thông tin đã lưu vào các trường nhập liệu
        loadRememberMePreferences();

        // KHÔNG CÒN LOGIC TỰ ĐỘNG ĐĂNG NHẬP Ở ĐÂY NỮA
        // Logic này đã được chuyển sang OpenActivity
    }

    private void initializeUIAndListeners() {
        edEmailLogin = findViewById(R.id.edFullName);
        edPasswordLogin = findViewById(R.id.edPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        progressBar = findViewById(R.id.progressBar);
        txtGuestMode = findViewById(R.id.txtGuestMode);

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
        } else {
            // Đảm bảo trường email và mật khẩu trống nếu "nhớ tài khoản" không được chọn
            edEmailLogin.setText("");
            edPasswordLogin.setText("");
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

    // Phương thức tĩnh để clear SharedPreferences, cần gọi từ OpenActivity hoặc Dashboard
    public static void clearRememberMePreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.apply();
        // Không cập nhật UI ở đây vì đây là static method, không có quyền truy cập trực tiếp vào CheckBox
        // CheckBox sẽ được cập nhật khi LoginActivity được load lại
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
            AppToast.show(this, "Vui lòng nhập email và mật khẩu.", Toast.LENGTH_SHORT);
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
                                db.collection("Users").document(user.getUid()).get()
                                        .addOnCompleteListener(userDocTask -> {
                                            if (userDocTask.isSuccessful()) {
                                                DocumentSnapshot document = userDocTask.getResult();
                                                if (document.exists()) {
                                                    Boolean isLocked = document.getBoolean("IsLocked");
                                                    if (isLocked != null && isLocked) {
                                                        mAuth.signOut();
                                                        hideProgressBar();
                                                        AppToast.show(LoginActivity.this, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_LONG);
                                                        Log.d(TAG, "Tài khoản bị khóa: " + user.getUid());
                                                        clearRememberMePreferences(LoginActivity.this);
                                                    } else {
                                                        Log.d(TAG, "Đăng nhập thành công.");
                                                        saveRememberMePreferences(email, password); // Lưu thông tin sau khi đăng nhập thành công

                                                        if (!user.isEmailVerified()) {
                                                            AppToast.show(LoginActivity.this, "Email của bạn chưa được xác thực. Một số tính năng có thể bị hạn chế. Vui lòng kiểm tra email để xác thực tài khoản.", Toast.LENGTH_LONG);
                                                        }
                                                        checkUserRoleAndNavigate(user.getUid());
                                                    }
                                                } else {
                                                    Log.e(TAG, "Không tìm thấy thông tin người dùng trong Firestore cho UID: " + user.getUid());
                                                    mAuth.signOut();
                                                    hideProgressBar();
                                                    AppToast.show(LoginActivity.this, "Lỗi: Không thể truy xuất thông tin tài khoản. Vui lòng thử lại hoặc liên hệ hỗ trợ.", Toast.LENGTH_LONG);
                                                    clearRememberMePreferences(LoginActivity.this);
                                                }
                                            } else {
                                                Log.e(TAG, "Lỗi khi lấy trạng thái khóa từ Firestore: " + userDocTask.getException().getMessage());
                                                mAuth.signOut();
                                                hideProgressBar();
                                                AppToast.show(LoginActivity.this, "Lỗi: Không thể kiểm tra trạng thái tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG);
                                                clearRememberMePreferences(LoginActivity.this);
                                            }
                                        });

                            } else {
                                hideProgressBar();
                                AppToast.show(LoginActivity.this, "Người dùng không tồn tại.", Toast.LENGTH_SHORT);
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
                            AppToast.show(LoginActivity.this, errorMessage, Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    private void checkUserRoleAndNavigate(String userId) {
        db.collection("UserRoles")
                .whereEqualTo("UserId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    hideProgressBar();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String userRoleName = null;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserRole userRole = document.toObject(UserRole.class);
                            userRoleName = userRole.getRoleId();
                            Log.d(TAG, "Fetched Role Name from UserRoles: " + userRoleName);
                            break;
                        }

                        if (userRoleName != null) {
                            if ("role_admin".equalsIgnoreCase(userRoleName)) {
                                Log.d(TAG, "Navigating to Admin Dashboard.");
                                navigateToAdminHomePage();
                            } else {
                                Log.d(TAG, "Navigating to Main Activity.");
                                navigateToMainActivity();
                            }
                        } else {
                            Log.e(TAG, "Lỗi: Không tìm thấy trường 'role' trong UserRole cho userId: " + userId);
                            AppToast.show(LoginActivity.this, "Lỗi: Không tìm thấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG);
                            navigateToMainActivity();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy UserRole từ Firestore hoặc không tìm thấy UserRole cho userId: " + userId, task.getException());
                        AppToast.show(LoginActivity.this, "Lỗi: Không thể lấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG);
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

    private void navigateToAdminHomePage() {
        Intent intent = new Intent(LoginActivity.this, AdminHomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnLogin != null) btnLogin.setEnabled(false);
        if (edEmailLogin != null) edEmailLogin.setEnabled(false);
        if (edPasswordLogin != null) edPasswordLogin.setEnabled(false);
        if (txtRegister != null) txtRegister.setEnabled(false);
        if (txtForgotPassword != null) txtForgotPassword.setEnabled(false);
        if (cbRememberMe != null) cbRememberMe.setEnabled(false);
        if (txtGuestMode != null) txtGuestMode.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (btnLogin != null) btnLogin.setEnabled(true);
        if (edEmailLogin != null) edEmailLogin.setEnabled(true);
        if (edPasswordLogin != null) edPasswordLogin.setEnabled(true);
        if (txtRegister != null) txtRegister.setEnabled(true);
        if (txtForgotPassword != null) txtForgotPassword.setEnabled(true);
        if (cbRememberMe != null) cbRememberMe.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true);
    }
}