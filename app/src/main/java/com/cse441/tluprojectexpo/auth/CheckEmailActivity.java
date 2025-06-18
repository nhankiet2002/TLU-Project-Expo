package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Hoặc MaterialButton nếu bạn đổi trong XML
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton; // Import MaterialButton nếu bạn dùng
import com.google.firebase.auth.FirebaseAuth;

public class CheckEmailActivity extends AppCompatActivity {

    private TextView txtYourEmail; // Để hiển thị email đã nhập
    private MaterialButton btnVerifyEmail; // Nút "Xác minh"
    private TextView tvResendCode; // TextView "Gửi lại mã xác minh"
    private ImageView imgBack; // Nút quay lại

    private String userEmail; // Để lưu trữ email được truyền từ ForgotPasswordActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_email);

        txtYourEmail = findViewById(R.id.txtYourEmail); // ID của TextView trong check_email.xml
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail); // ID của Button "Xác minh"
        tvResendCode = findViewById(R.id.textView12); // ID của TextView "Gửi lại mã xác minh"
        imgBack = findViewById(R.id.imageView2); // ID của ImageView mũi tên quay lại

        // Lấy email được truyền từ Intent
        if (getIntent().hasExtra("email")) {
            userEmail = getIntent().getStringExtra("email");
            txtYourEmail.setText(userEmail);
        } else {
            txtYourEmail.setText("Email của bạn"); // Fallback nếu không nhận được email
        }

        // Logic cho nút "Xác minh" - Có thể chuyển sang màn hình thành công hoặc quay lại login
        btnVerifyEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Giả định người dùng đã kiểm tra email và đặt lại mật khẩu bên ngoài app
                // Bây giờ cho phép họ quay lại màn hình đăng nhập hoặc thông báo thành công
                Toast.makeText(CheckEmailActivity.this, "Hãy kiểm tra email để hoàn tất việc đặt lại mật khẩu.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(CheckEmailActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Xóa các activity trên stack và đưa LoginActivity lên đầu
                startActivity(intent);
                finish();
            }
        });

        // Logic cho "Gửi lại mã xác minh"
        tvResendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userEmail != null && !userEmail.isEmpty()) {
                    // Gọi lại Firebase Auth để gửi email đặt lại
                     FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(CheckEmailActivity.this, "Email đặt lại mật khẩu đã được gửi lại.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(CheckEmailActivity.this, "Không thể gửi lại email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(CheckEmailActivity.this, "Không có email để gửi lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Logic cho nút quay lại
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Quay lại màn hình trước (ForgotPasswordActivity)
            }
        });
    }
}