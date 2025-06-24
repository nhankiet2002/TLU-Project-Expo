package com.cse441.tluprojectexpo.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.cloudinary.Cloudinary;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingProfileActivity extends AppCompatActivity {

    private static final String TAG = "SettingProfileActivity";

    // Các hằng số Cloudinary Upload Presets
    private static final String CLOUDINARY_UPLOAD_PRESET_THUMBNAIL = "TLUProjectExpo";
    private static final String CLOUDINARY_UPLOAD_PRESET_MEDIA = "TLUProjectExpo"; // Có vẻ cả hai dùng chung một preset

    private ImageView imageView;
    private EditText khoaEditText, lopEditText, edUserName;
    private Button saveButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private ImageView imgBack;
    private FirebaseAuth auth;

    private String currentUserId;
    private Button btnChangePW;

    private Cloudinary cloudinary;
    private Uri selectedImageUri; // Lưu trữ URI của ảnh đã chọn

    // Launcher để chọn ảnh từ thư viện
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Hiển thị ảnh đã chọn ngay lập tức trên ImageView
                        Glide.with(this).load(selectedImageUri)
                                .placeholder(R.drawable.default_avatar)
                                .into(imageView);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        imageView = findViewById(R.id.imgUserImage);
        edUserName = findViewById(R.id.edUserName);
        khoaEditText = findViewById(R.id.editTextText2);
        lopEditText = findViewById(R.id.editTextText3);
        saveButton = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar2);
        imgBack = findViewById(R.id.imgBackToFragmentProfile);
        btnChangePW = findViewById(R.id.btnChangePW);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "devrh3pid");
        config.put("api_key", "716836116263789");
        config.put("api_secret", "ugFoenLx6va0q5IyKRmPzibFXKA");
        cloudinary = new Cloudinary(config);

        if (auth.getCurrentUser() == null) {
            AppToast.show(this, "Chưa đăng nhập", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        setClickListener();
        loadUserProfile();

        saveButton.setOnClickListener(v -> confirmSave());
    }

    private void setClickListener() {
        btnChangePW.setOnClickListener(v -> {
            Intent intent = new Intent(SettingProfileActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
        imgBack.setOnClickListener(v -> {
            finish();
        });

        // Khi click vào ImageView, mở trình chọn ảnh
        imageView.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void loadUserProfile() {
        DocumentReference userRef = db.collection("Users").document(currentUserId);
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String fullName = snapshot.getString("FullName");
                String avatarUrl = snapshot.getString("AvatarUrl");
                String userClass = snapshot.getString("Class");

                if (fullName != null) edUserName.setText(fullName);
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this).load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .into(imageView);
                } else {
                    Glide.with(this).load(R.drawable.default_avatar).into(imageView);
                }
                if (userClass != null && !userClass.isEmpty()) {
                    String khoa = userClass;
                    String lop = "";
                    for (int i = 0; i < userClass.length(); i++) {
                        if (Character.isDigit(userClass.charAt(i))) {
                            khoa = userClass.substring(0, i);
                            lop = userClass.substring(i);
                            break;
                        }
                    }
                    khoaEditText.setText(khoa);
                    lopEditText.setText(lop);
                }
            } else {
                Glide.with(this).load(R.drawable.default_avatar).into(imageView);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading user profile: " + e.getMessage());
            AppToast.show(this, "Không thể tải thông tin hồ sơ.", Toast.LENGTH_SHORT);
            Glide.with(this).load(R.drawable.default_avatar).into(imageView);
        });
    }

    private void confirmSave() {
        String fullName = edUserName.getText().toString().trim();
        String khoa = khoaEditText.getText().toString().trim();
        String lop = lopEditText.getText().toString().trim();

        if (fullName.isEmpty() || khoa.isEmpty() || lop.isEmpty()) {
            AppToast.show(this, "Vui lòng điền đầy đủ tên, khoa và lớp", Toast.LENGTH_SHORT);
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
        showProgressBar();

        String fullName = edUserName.getText().toString().trim();
        String khoa = khoaEditText.getText().toString().trim();
        String lop = lopEditText.getText().toString().trim();
        String userClass = khoa + lop;

        DocumentReference userRef = db.collection("Users").document(currentUserId);

        if (selectedImageUri != null) {
            // Nếu có ảnh mới được chọn, tải lên Cloudinary trước
            uploadImageToCloudinary(userRef, fullName, userClass);
        } else {
            // Nếu không có ảnh mới, chỉ lưu thông tin FullName và Class
            saveToFirestore(userRef, fullName, userClass, null); // avatarUrl là null
        }
    }

    private void uploadImageToCloudinary(DocumentReference userRef, String fullName, String userClass) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                if (inputStream == null) {
                    throw new IOException("Could not open input stream for selected image URI.");
                }

                // Tạo map options để bao gồm upload_preset
                Map<String, Object> options = new HashMap<>();
                options.put("upload_preset", CLOUDINARY_UPLOAD_PRESET_MEDIA); // Sử dụng preset media

                Map uploadResult = cloudinary.uploader().upload(inputStream, options);
                String imageUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    if (imageUrl != null) {
                        Log.d(TAG, "Image uploaded to Cloudinary: " + imageUrl);
                        saveToFirestore(userRef, fullName, userClass, imageUrl);
                    } else {
                        hideProgressBar();
                        AppToast.show(SettingProfileActivity.this, "Lỗi tải ảnh lên Cloudinary.", Toast.LENGTH_SHORT);
                        Log.e(TAG, "Cloudinary upload failed: secure_url is null.");
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Cloudinary upload error: " + e.getMessage());
                runOnUiThread(() -> {
                    hideProgressBar();
                    AppToast.show(SettingProfileActivity.this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_LONG);
                });
            }
        });
    }

    private void saveToFirestore(DocumentReference userRef, String fullName, String userClass, String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("FullName", fullName);
        updates.put("Class", userClass);
        if (avatarUrl != null) {
            updates.put("AvatarUrl", avatarUrl);
        }

        userRef.update(updates)
                .addOnSuccessListener(unused -> {
                    hideProgressBar();
                    Intent intent = new Intent(SettingProfileActivity.this, SaveProfileSuccessfulActivity.class); // Thay bằng Activity thông báo thành công của bạn
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressBar();
                    AppToast.show(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT);
                    Log.e(TAG, "Firestore update failed: " + e.getMessage());
                });
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        saveButton.setEnabled(false);
        edUserName.setEnabled(false);
        khoaEditText.setEnabled(false);
        lopEditText.setEnabled(false);
        btnChangePW.setEnabled(false);
        imageView.setEnabled(false); // Vô hiệu hóa ImageView khi đang tải
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        saveButton.setEnabled(true);
        edUserName.setEnabled(true);
        khoaEditText.setEnabled(true);
        lopEditText.setEnabled(true);
        btnChangePW.setEnabled(true);
        imageView.setEnabled(true); // Bật lại ImageView
    }
}