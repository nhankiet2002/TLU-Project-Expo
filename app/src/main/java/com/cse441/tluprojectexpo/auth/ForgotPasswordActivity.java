package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText edEmail;
    private Button btnSentCode;
    private TextView txtLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        mAuth = FirebaseAuth.getInstance();

        edEmail = findViewById(R.id.edEmail);
        btnSentCode = findViewById(R.id.btnSentCode);
        txtLogin = findViewById(R.id.txtLogin);
        progressBar = findViewById(R.id.progessBar1); // Đảm bảo ID này khớp với XML

        btnSentCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        txtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay lại màn hình đăng nhập
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Đóng Activity hiện tại
            }
        });
    }

    private void resetPassword() {
        String email = edEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edEmail.setError("Vui lòng nhập địa chỉ email của bạn.");
            edEmail.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        hideProgressBar();
                        if (task.isSuccessful()) {
                            AppToast.show(ForgotPasswordActivity.this, "Đã gửi hướng dẫn đặt lại mật khẩu đến email của bạn.", Toast.LENGTH_LONG);
                            // Chuyển sang màn hình CheckEmailActivity và truyền email qua
                            Intent intent = new Intent(ForgotPasswordActivity.this, CheckEmailActivity.class);
                            intent.putExtra("user_email", email); // Truyền email qua CheckEmailActivity
                            startActivity(intent);
                            finish(); // Đóng ForgotPasswordActivity
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                            AppToast.show(ForgotPasswordActivity.this, "Không thể gửi email đặt lại mật khẩu: " + errorMessage, Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnSentCode.setEnabled(false);
        edEmail.setEnabled(false);
        txtLogin.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnSentCode.setEnabled(true);
        edEmail.setEnabled(true);
        txtLogin.setEnabled(true);
    }
}