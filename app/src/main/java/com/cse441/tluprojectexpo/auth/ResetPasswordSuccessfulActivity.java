package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Hoặc MaterialButton

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton; // Import MaterialButton nếu bạn dùng

public class ResetPasswordSuccessfulActivity extends AppCompatActivity {

    private MaterialButton btnReturnToLogin; // Tương ứng với id btnReturnToLogin trong reset_password_successfull.xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password_successfull); // Hoặc R.layout.password_changed

        btnReturnToLogin = findViewById(R.id.btnReturnToLogin);

        btnReturnToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay về màn hình LoginActivity và xóa tất cả các Activity khác trên stack
                Intent intent = new Intent(ResetPasswordSuccessfulActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}