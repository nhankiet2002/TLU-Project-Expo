// PATH: com/cse441/tluprojectexpo/auth/ResetPasswordActivity.java
package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar; // Thêm ProgressBar nếu bạn muốn hiển thị trạng thái tải
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private Button btnUpdatePW;
    private ImageView imgBackToForgotPWCode;
    private ProgressBar progressBar; // Thêm ProgressBar

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password); // Đảm bảo sử dụng layout đúng

        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các View
        // txtTitle và txtContentResetPW đã được đặt trong XML, không cần ánh xạ nếu không thay đổi text động
        btnUpdatePW = findViewById(R.id.btnUpdatePW);
        imgBackToForgotPWCode = findViewById(R.id.imgBackToForgotPWCode);
        // Giả định bạn có ProgressBar trong reset_password.xml với id progessBar1 hoặc khác
        // Nếu không có, bạn có thể bỏ dòng này hoặc thêm ProgressBar vào XML
        progressBar = findViewById(R.id.progessBar1); // Đảm bảo ID này khớp với XML của bạn

        // Logic cho nút "Xác nhận đổi mật khẩu" (btnUpdatePW)
        btnUpdatePW.setOnClickListener(v -> sendPasswordResetEmail());

        // Logic cho nút quay lại
        imgBackToForgotPWCode.setOnClickListener(v -> finish());
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            // Có thể chuyển hướng về màn hình đăng nhập nếu không có người dùng
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            // finish();
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy email của bạn để gửi link đặt lại mật khẩu.", Toast.LENGTH_LONG).show();
            return;
        }

        showProgressBar(); // Hiển thị ProgressBar

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgressBar(); // Ẩn ProgressBar
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        Toast.makeText(ResetPasswordActivity.this, "Đã gửi link đổi mật khẩu đến email của bạn (" + email + "). Vui lòng kiểm tra email!", Toast.LENGTH_LONG).show();
                        // Chuyển sang màn hình thông báo thành công
                        Intent successIntent = new Intent(ResetPasswordActivity.this, ResetPasswordSuccessfullActivity.class);
                        startActivity(successIntent);
                        finish(); // Đóng ResetPasswordActivity
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                        Log.e(TAG, "Failed to send password reset email: " + errorMessage);
                        Toast.makeText(ResetPasswordActivity.this, "Không thể gửi link đổi mật khẩu: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
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