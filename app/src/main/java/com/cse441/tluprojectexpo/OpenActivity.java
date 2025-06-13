package com.cse441.tluprojectexpo; // Đảm bảo đúng package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Sử dụng Button cho MaterialButton
import android.widget.Toast; // Chỉ để test

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OpenActivity extends AppCompatActivity {

    private Button btnLogIn, btnSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);

        mAuth = FirebaseAuth.getInstance();

        btnLogIn = findViewById(R.id.btnLogIn);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra xem người dùng đã đăng nhập chưa
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Nếu đã đăng nhập, chuyển hướng đến MainActivity và đóng OpenActivity
            Intent intent = new Intent(OpenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa hết các Activity cũ trên stack
            startActivity(intent);
            finish(); // Đóng OpenActivity
        }
        // Nếu chưa đăng nhập, sẽ ở lại màn hình open.xml
    }
}