package com.cse441.tluprojectexpo; // Đảm bảo đúng package của bạn

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button; // Sử dụng Button thay vì MaterialButton nếu bạn đã dùng Button trong XML
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText; // Dùng TextInputEditText nếu có
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.cse441.tluprojectexpo.utils.InputValidationUtils;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Khai báo các thành phần UI - SẼ DÙNG CÁC ID TỪ XML CỦA BẠN
    private TextInputEditText edPasswordLogin, edEmailLogin;
    private Button btnLogin; // ID từ XML
    private TextView txtRegister, txtForgotPassword; // ID từ XML
    private ProgressBar progressBar; // ID từ XML
    private TextView txtGuestMode;

    // Firebase Auth instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this); // Giữ nguyên hoặc bỏ nếu bạn đã xử lý insets bằng fitsSystemWindows="true"

        setContentView(R.layout.activity_login); // Layout của bạn

        txtGuestMode = findViewById(R.id.txtGuestMode); // Ánh xạ ID của TextView "Chế độ khách"

        txtGuestMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGuestMode(true); // Đặt cờ là người dùng khách
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Kết thúc LoginActivity
            }
        });
        // Xử lý Insets (tùy chọn, nếu bạn dùng EdgeToEdge và muốn UI tràn màn hình)
        // Nếu layout gốc của bạn không có ID riêng, có thể thay thế bằng ID của ScrollView hoặc ConstraintLayout gốc
        // Nếu bạn đã dùng fitsSystemWindows="true" cho ScrollView, bạn có thể bỏ đoạn này
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // Hoặc ID của layout gốc nếu bạn đặt tên
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo các View và Firebase
        initViews();
        initFirebase();
        setListeners();
    }

    private void initViews() {
        // Ánh xạ các View theo ID trong activity_login.xml của BẠN
        edEmailLogin = findViewById(R.id.edEmailLogin); // ID cho Email
        edPasswordLogin = findViewById(R.id.edPasswordLogin); // ID cho Mật khẩu
        btnLogin = findViewById(R.id.btnLogin); // ID cho nút Đăng nhập
        txtRegister = findViewById(R.id.txtRegister); // ID cho TextView "Đăng ký"
        txtForgotPassword = findViewById(R.id.txtForgotPassword); // ID cho TextView "Quên Mật Khẩu?"
        progressBar = findViewById(R.id.progressBar); // ID cho ProgressBar
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        txtRegister.setOnClickListener(v -> {
            // Chuyển sang màn hình đăng ký
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        txtForgotPassword.setOnClickListener(v -> {
            // Chuyển sang màn hình quên mật khẩu (sẽ triển khai sau)
            Toast.makeText(LoginActivity.this, "Chức năng quên mật khẩu đang được phát triển.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class); startActivity(intent);
        });
    }

    private void loginUser() {
        String email = edEmailLogin.getText().toString().trim();
        String password = edPasswordLogin.getText().toString().trim();

        // Xóa lỗi cũ
        edEmailLogin.setError(null);
        edPasswordLogin.setError(null);

        // Kiểm tra hợp lệ đầu vào sử dụng InputValidationUtils
        if (!InputValidationUtils.isValidEmail(email)) {
            edEmailLogin.setError("Email không hợp lệ!");
            edEmailLogin.requestFocus();
            return;
        }
        if (!InputValidationUtils.isNotEmpty(password)) {
            edPasswordLogin.setError("Mật khẩu không được để trống!");
            edPasswordLogin.requestFocus();
            return;
        }

        // Ẩn bàn phím ảo
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edEmailLogin.getWindowToken(), 0); // Có thể ẩn từ bất kỳ EditText nào
        }

        // Hiển thị ProgressBar và vô hiệu hóa nút
        showProgressBar();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Ẩn ProgressBar và bật lại nút
                        hideProgressBar();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // // BỎ QUA KIỂM TRA XÁC MINH EMAIL NẾU BẠN KHÔNG MUỐN
                                // if (user.isEmailVerified()) { // Dòng này sẽ được bỏ hoặc comment
                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                // Chuyển đến màn hình chính (MainActivity)
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa hết các activity trước đó
                                startActivity(intent);
                                finish(); // Đóng LoginActivity
                                // } else { // Phần else này cũng được bỏ hoặc comment
                                //     Toast.makeText(LoginActivity.this, "Vui lòng xác minh email của bạn trước khi đăng nhập.", Toast.LENGTH_LONG).show();
                                //     mAuth.signOut();
                                // }
                            }
                        } else {
                            // Đăng nhập thất bại
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
    }

    // Tùy chọn: Tự động chuyển hướng nếu người dùng đã đăng nhập (sử dụng trong OpenActivity thay vì đây)
    @Override
    protected void onStart() {
        super.onStart();
//         FirebaseUser currentUser = mAuth.getCurrentUser();
//         if (currentUser != null && currentUser.isEmailVerified()) {
//            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
//         }
    }
    private void setGuestMode(boolean isGuest) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isGuestMode", isGuest); // Lưu trạng thái
        editor.apply(); // Lưu không đồng bộ
    }
}