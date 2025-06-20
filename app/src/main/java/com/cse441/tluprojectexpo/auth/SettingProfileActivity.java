package com.cse441.tluprojectexpo.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.fragment.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingProfileActivity extends AppCompatActivity {

    private ImageView imageView;
    private EditText khoaEditText, lopEditText, edUserName;
    private Button saveButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private ImageView imgBack;
    private FirebaseAuth auth;

    private String currentUserId;
    private Button btnChangePW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        imageView = findViewById(R.id.imageView4);
        edUserName = findViewById(R.id.edUserName);
        khoaEditText = findViewById(R.id.editTextText2);
        lopEditText = findViewById(R.id.editTextText3);
        saveButton = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar2);
        imgBack = findViewById(R.id.imgBackToFragmentProfile);
        btnChangePW = findViewById(R.id.btnChangePW);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setClickListener();

        currentUserId = auth.getCurrentUser().getUid();

        loadUserProfile();

        saveButton.setOnClickListener(v -> confirmSave());
    }

    private void setClickListener() {
        btnChangePW.setOnClickListener(v -> {
            Intent intent = new Intent(SettingProfileActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
        imgBack.setOnClickListener(v -> {
           finish();
        });
    }

    private void loadUserProfile() {
        DocumentReference userRef = db.collection("Users").document(currentUserId);
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String fullName = snapshot.getString("FullName");
                String avatarUrl = snapshot.getString("AvatarUrl");
                String userClass = snapshot.getString("Class");

                if (fullName != null) edUserName.setText(fullName);
                if (avatarUrl != null) {
                    Glide.with(this).load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .into(imageView);
                } else {
                    Glide.with(this).load(R.drawable.default_avatar).into(imageView);
                }
                if (userClass != null && userClass.length() > 2) {
                    khoaEditText.setText(userClass.substring(0, userClass.length() - 1));
                    lopEditText.setText(userClass.substring(userClass.length() - 1));
                }
            } else {
                Glide.with(this).load(R.drawable.default_avatar).into(imageView);
            }
        });
    }

    private void confirmSave() {
        String fullName = edUserName.getText().toString().trim(); // Lấy FullName
        String khoa = khoaEditText.getText().toString().trim();
        String lop = lopEditText.getText().toString().trim();

        if (fullName.isEmpty() || khoa.isEmpty() || lop.isEmpty()) { // Kiểm tra cả FullName
            Toast.makeText(this, "Vui lòng điền đầy đủ tên, khoa và lớp", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận lưu")
                .setMessage("Bạn có chắc chắn muốn lưu thông tin này?")
                .setPositiveButton("Lưu", (dialog, which) -> saveProfile())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveProfile() {
        String fullName = edUserName.getText().toString().trim(); // Lấy FullName để lưu
        String khoa = khoaEditText.getText().toString().trim();
        String lop = lopEditText.getText().toString().trim();
        String userClass = khoa + lop;

        progressBar.setVisibility(View.VISIBLE);
        DocumentReference userRef = db.collection("Users").document(currentUserId);

        // Gọi saveToFirestore với FullName và userClass
        saveToFirestore(userRef, fullName, userClass);
    }

    private void saveToFirestore(DocumentReference userRef, String fullName, String userClass) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("FullName", fullName); // Thêm FullName vào danh sách cập nhật
        updates.put("Class", userClass);

        userRef.update(updates)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Intent intent = new Intent(SettingProfileActivity.this, SaveProfileSuccessfulActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                });
    }
}