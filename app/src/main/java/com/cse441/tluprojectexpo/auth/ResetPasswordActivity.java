// PATH: com/cse441/tluprojectexpo/auth/ResetPasswordActivity.java
package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.net.Uri; // Cần thiết để tạo Intent mở ứng dụng email
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private Button btnUpdatePW;
    private ImageView imgBackToForgotPWCode;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password); // Đảm bảo sử dụng layout đúng

        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các View
        btnUpdatePW = findViewById(R.id.btnUpdatePW);
        imgBackToForgotPWCode = findViewById(R.id.imgBackToForgotPWCode);
        progressBar = findViewById(R.id.progessBar1); // Đảm bảo ID này khớp với XML của bạn

        // Logic cho nút "Xác nhận đổi mật khẩu" (btnUpdatePW)
        btnUpdatePW.setOnClickListener(v -> sendPasswordResetEmail());

        // Logic cho nút quay lại
        imgBackToForgotPWCode.setOnClickListener(v -> finish());
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            AppToast.show(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT);
            // Có thể chuyển hướng về màn hình đăng nhập nếu không có người dùng
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            // finish();
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            AppToast.show(this, "Không tìm thấy email của bạn để gửi link đặt lại mật khẩu.", Toast.LENGTH_LONG);
            return;
        }

        showProgressBar(); // Hiển thị ProgressBar

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgressBar(); // Ẩn ProgressBar
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        AppToast.show(ResetPasswordActivity.this, "Đã gửi link đổi mật khẩu đến email của bạn (" + email + "). Vui lòng kiểm tra email!", Toast.LENGTH_LONG);
                        // Thay vì chuyển sang ResetPasswordSuccessfullActivity, chúng ta sẽ mở ứng dụng email
                        openEmailApp();
                        // Bạn có thể chọn finish() Activity này hoặc để nó chờ người dùng quay lại
                        // finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                        Log.e(TAG, "Failed to send password reset email: " + errorMessage);
                        AppToast.show(ResetPasswordActivity.this, "Không thể gửi link đổi mật khẩu: " + errorMessage, Toast.LENGTH_LONG);
                    }
                });
    }

    // --- Phương thức mới để mở ứng dụng email ---
    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        // Đảm bảo ứng dụng email mở trong một tác vụ mới, ngăn chặn việc quay lại ngay lập tức
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Kiểm tra xem có ứng dụng email nào có thể xử lý intent này không
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            AppToast.show(this, "Không tìm thấy ứng dụng email nào trên thiết bị của bạn.", Toast.LENGTH_SHORT);
        }
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnUpdatePW.setEnabled(false);
        imgBackToForgotPWCode.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnUpdatePW.setEnabled(true);
        imgBackToForgotPWCode.setEnabled(true);
    }
}