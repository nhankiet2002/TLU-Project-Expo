package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button; // Hoặc MaterialButton
import android.widget.EditText; // Hoặc TextInputEditText
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import com.cse441.tluprojectexpo.utils.InputValidationUtils; // Đảm bảo bạn có lớp này

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edEmailForgot; // Tương ứng với id edEmail trong forgot_password.xml
    private Button btnSentCode; // Tương ứng với id btnSentCode trong forgot_password.xml
    private TextView txtLogin; // Tương ứng với id txtLogin trong forgot_password.xml

    private ProgressBar progressBar; // Tương ứng với id progressBar trong forgot_password.xml

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các thành phần UI
        edEmailForgot = findViewById(R.id.edEmail); // ID của EditText trong forgot_password.xml
        btnSentCode = findViewById(R.id.btnSentCode); // ID của Button trong forgot_password.xml
        txtLogin = findViewById(R.id.txtLogin); // ID của TextView "Đăng nhập"
        progressBar = findViewById(R.id.progessBar1); // ID của ProgressBar

        btnSentCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmail();
            }
        });

        txtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay về màn hình LoginActivity
                startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                finish();
            }
        });

    }

    private void sendPasswordResetEmail() {
        String email = edEmailForgot.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edEmailForgot.setError("Vui lòng nhập địa chỉ email");
            edEmailForgot.requestFocus();
            return;
        }

        if (!InputValidationUtils.isValidEmail(email)) { // Sử dụng hàm kiểm tra email của bạn
            edEmailForgot.setError("Email không hợp lệ");
            edEmailForgot.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        hideProgressBar();
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Vui lòng kiểm tra email của bạn để đặt lại mật khẩu.", Toast.LENGTH_LONG).show();
                            // Chuyển sang màn hình CheckEmailActivity
                            Intent intent = new Intent(ForgotPasswordActivity.this, CheckEmailActivity.class);
                            intent.putExtra("email", email); // Truyền email sang màn hình CheckEmail
                            startActivity(intent);
                            finish(); // Đóng ForgotPasswordActivity
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đã xảy ra lỗi.";
                            Toast.makeText(ForgotPasswordActivity.this, "Gửi email thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnSentCode.setEnabled(false);
        edEmailForgot.setEnabled(false);
        txtLogin.setEnabled(false);
    }
    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnSentCode.setEnabled(true);
        edEmailForgot.setEnabled(true);
        txtLogin.setEnabled(true);
    }
}