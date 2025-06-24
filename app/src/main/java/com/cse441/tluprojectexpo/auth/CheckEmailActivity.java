package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class CheckEmailActivity extends AppCompatActivity {

    private TextView txtYourEmail;
    private MaterialButton btnResendLink; // Đổi tên để phản ánh chức năng mới
    private ImageView imgBack;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_email);

        txtYourEmail = findViewById(R.id.txtYourEmail);
        btnResendLink = findViewById(R.id.btnVerifyEmail); // Sử dụng lại ID này, thay đổi text trong XML
        imgBack = findViewById(R.id.imageView2); // Nút back

        // Lấy email từ Intent
        if (getIntent() != null && getIntent().hasExtra("user_email")) {
            userEmail = getIntent().getStringExtra("user_email");
            txtYourEmail.setText(userEmail);
        } else {
            // Trường hợp không có email, có thể hiển thị thông báo lỗi hoặc quay lại màn hình trước
            AppToast.show(this, "Không tìm thấy email.", Toast.LENGTH_SHORT);
            finish();
        }


        // Logic cho nút "Gửi lại đường dẫn" (trước đây là "Xác minh")
        btnResendLink.setOnClickListener(v -> {

        });

        // Logic cho nút quay lại
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Quay lại màn hình ForgotPasswordActivity
            }
        });
    }

}