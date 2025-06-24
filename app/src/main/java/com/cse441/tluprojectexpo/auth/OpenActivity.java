// PATH: com/cse441/tluprojectexpo/auth/OpenActivity.java
package com.cse441.tluprojectexpo.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Thêm ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.AdminHomePage; // Import AdminHomePage
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.UserRole; // Import UserRole
import com.cse441.tluprojectexpo.utils.GuestModeHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class OpenActivity extends AppCompatActivity {

    private static final String TAG = "OpenActivity"; // Đặt TAG riêng cho OpenActivity

    private Button btnLogIn, btnSignUp;
    private TextView txtGuestMode;
    private ProgressBar progressBar; // Khai báo ProgressBar
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Khai báo Firestore

    // Các hằng số SharedPreferences, phải khớp với LoginActivity
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_IS_GUEST_MODE = "isGuestMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ban đầu, không set content view để có thể hiển thị ProgressBar toàn màn hình nếu cần
        // hoặc đơn giản là để nó nhanh chóng kiểm tra và chuyển hướng

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore

        // --- Bắt đầu logic kiểm tra tự động đăng nhập/guest mode ngay khi onCreate ---
        checkAndRedirectUser();
    }

    // Phương thức kiểm tra và chuyển hướng người dùng
    private void checkAndRedirectUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMeFlag = prefs.getBoolean(KEY_REMEMBER_ME, false);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);

        // Hiển thị ProgressBar nếu có khả năng tự động đăng nhập/guest mode
        // Bạn cần một ProgressBar trong layout gốc của Activity này HOẶC set một layout chứa ProgressBar
        // Để đơn giản, tôi sẽ giả định OpenActivity có thể hiển thị ProgressBar.
        // Tốt nhất nên có một SplashScreen Activity riêng để xử lý.
        setContentView(R.layout.activity_open); // Luôn setContentView để các findViewById không null
        initializeUIAndListeners(); // Khởi tạo UI sau khi setContentView
        showProgressBar(); // Hiển thị ProgressBar sau khi UI được khởi tạo

        if (isGuestMode) {
            Log.d(TAG, "User is in Guest Mode. Redirecting to MainActivity.");
            navigateToMainActivity();
            finish();
        } else if (rememberMeFlag && currentUser != null) {
            Log.d(TAG, "Remember Me is ON and Firebase user exists. Checking Firestore status...");
            // Kiểm tra trạng thái khóa tài khoản trong Firestore
            db.collection("Users").document(currentUser.getUid()).get()
                    .addOnCompleteListener(userDocTask -> {
                        if (!isFinishing() && !isDestroyed()) { // Đảm bảo Activity còn sống
                            if (userDocTask.isSuccessful()) {
                                DocumentSnapshot document = userDocTask.getResult();
                                if (document.exists()) {
                                    Boolean isLocked = document.getBoolean("IsLocked");
                                    if (isLocked != null && isLocked) {
                                        // Tài khoản bị khóa
                                        mAuth.signOut(); // Đăng xuất Firebase
                                        LoginActivity.clearRememberMePreferences(OpenActivity.this); // Xóa SharedPreferences
                                        hideProgressBar(); // Ẩn ProgressBar
                                        AppToast.show(OpenActivity.this, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_LONG);
                                        Log.d(TAG, "Tài khoản bị khóa: " + currentUser.getUid());
                                        // Giữ màn hình OpenActivity và hiển thị các nút đăng nhập/đăng ký
                                    } else {
                                        // Tài khoản không bị khóa, kiểm tra vai trò và chuyển hướng
                                        Log.d(TAG, "User account is not locked. Checking role.");
                                        // Check if email is verified (optional, can be done later)
                                        if (!currentUser.isEmailVerified()) {
                                            AppToast.show(OpenActivity.this, "Email của bạn chưa được xác thực. Một số tính năng có thể bị hạn chế.", Toast.LENGTH_LONG);
                                        }
                                        checkUserRoleAndNavigate(currentUser.getUid());
                                    }
                                } else {
                                    // Document người dùng không tồn tại trong Firestore
                                    Log.e(TAG, "Không tìm thấy thông tin người dùng trong Firestore cho UID: " + currentUser.getUid());
                                    mAuth.signOut();
                                    hideProgressBar();
                                    AppToast.show(OpenActivity.this, "Lỗi: Không thể truy xuất thông tin tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG);
                                    LoginActivity.clearRememberMePreferences(OpenActivity.this);
                                }
                            } else {
                                // Lỗi khi truy vấn Firestore
                                Log.e(TAG, "Lỗi khi lấy trạng thái khóa từ Firestore: " + userDocTask.getException().getMessage());
                                mAuth.signOut();
                                hideProgressBar();
                                AppToast.show(OpenActivity.this, "Lỗi: Không thể kiểm tra trạng thái tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG);
                                LoginActivity.clearRememberMePreferences(OpenActivity.this);
                            }
                        }
                    });
        } else {
            // Không có rememberMe, hoặc currentUser là null, hoặc guest mode không hoạt động.
            // Hiển thị màn hình OpenActivity với các nút đăng nhập/đăng ký.
            hideProgressBar(); // Đảm bảo ẩn ProgressBar nếu không tự động chuyển hướng
            Log.d(TAG, "No auto-login or guest mode. Showing login/register options.");
        }
    }

    // Phương thức khởi tạo UI và các listener
    private void initializeUIAndListeners() {
        btnLogIn = findViewById(R.id.btnLogIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtGuestMode = findViewById(R.id.txtGuestMode);
        progressBar = findViewById(R.id.progressBar); // Đảm bảo ID này chính xác trong activity_open.xml

        btnLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(OpenActivity.this, LoginActivity.class);
            startActivity(intent);
            // Không finish() ở đây, để người dùng có thể back về OpenActivity từ Login/Register
            // Nếu muốn OpenActivity kết thúc, hãy thêm finish()
        });

        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(OpenActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        if (txtGuestMode != null) {
            txtGuestMode.setOnClickListener(v -> {
                GuestModeHandler.enterGuestMode(OpenActivity.this, mAuth);
                finish();
            });
        }
        // Thêm ViewCompat.setOnApplyWindowInsetsListener nếu bạn đang sử dụng nó
        // Nếu layout activity_open của bạn không có id "main" thì cần điều chỉnh.
        // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
        //     Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        //     v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        //     return insets;
        // });
    }

    // Phương thức kiểm tra vai trò và điều hướng
    private void checkUserRoleAndNavigate(String userId) {
        db.collection("UserRoles")
                .whereEqualTo("UserId", userId) // Giữ nguyên UserId hoa
                .get()
                .addOnCompleteListener(task -> {
                    hideProgressBar(); // Ẩn ProgressBar sau khi kiểm tra vai trò hoàn tất

                    if (!isFinishing() && !isDestroyed()) { // Đảm bảo Activity còn sống
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
                                    Log.d(TAG, "Navigating to Admin Home Page.");
                                    navigateToAdminHomePage();
                                } else {
                                    Log.d(TAG, "Navigating to Main Activity.");
                                    navigateToMainActivity();
                                }
                            } else {
                                Log.e(TAG, "Lỗi: Không tìm thấy trường 'role' trong UserRole cho userId: " + userId);
                                AppToast.show(OpenActivity.this, "Lỗi: Không tìm thấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG);
                                navigateToMainActivity();
                            }
                        } else {
                            Log.e(TAG, "Lỗi khi lấy UserRole từ Firestore hoặc không tìm thấy UserRole cho userId: " + userId, task.getException());
                            AppToast.show(OpenActivity.this, "Lỗi: Không thể lấy vai trò người dùng. Mặc định vào màn hình người dùng.", Toast.LENGTH_LONG);
                            navigateToMainActivity();
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(OpenActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Kết thúc OpenActivity
    }

    private void navigateToAdminHomePage() {
        Intent intent = new Intent(OpenActivity.this, AdminHomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Kết thúc OpenActivity
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        // Vô hiệu hóa các thành phần UI để tránh tương tác khi đang xử lý
        if (btnLogIn != null) btnLogIn.setEnabled(false);
        if (btnSignUp != null) btnSignUp.setEnabled(false);
        if (txtGuestMode != null) txtGuestMode.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        // Kích hoạt lại các thành phần UI
        if (btnLogIn != null) btnLogIn.setEnabled(true);
        if (btnSignUp != null) btnSignUp.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true);
    }

    // Phương thức tĩnh để clear SharedPreferences, cần gọi từ LoginActivity hoặc Dashboard
    public static void clearRememberMePreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.remove(KEY_EMAIL); // Vẫn cần KEY_EMAIL và KEY_PASSWORD
        editor.remove(KEY_PASSWORD);
        editor.apply();
        // Không cập nhật UI ở đây vì đây là static method, không có quyền truy cập trực tiếp vào CheckBox
        // CheckBox sẽ được cập nhật khi LoginActivity được load lại
    }

    // Cần thêm hằng số KEY_EMAIL và KEY_PASSWORD vào đây để clearRememberMePreferences có thể dùng được
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
}