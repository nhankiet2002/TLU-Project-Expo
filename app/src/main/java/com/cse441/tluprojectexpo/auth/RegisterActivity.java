package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.GuestModeHandler; // Import lớp tiện ích
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edFullName, edEmailRegister, edPasswordRegister, edVerifyPW;
    private Spinner spinnerRole;
    private MaterialButton btnRegister;
    private CheckBox checkBoxAgreeTerms;
    private TextView txtLoginFromRegister;
    private TextView txtGuestMode; // Khai báo TextView cho chế độ khách
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";

    // Constants cho SharedPreferences (giống trong GuestModeHandler)
    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_IS_GUEST_MODE = "isGuestMode";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edFullName = findViewById(R.id.edFullName);
        edEmailRegister = findViewById(R.id.edEmailRegister);
        edPasswordRegister = findViewById(R.id.edPasswordRegister);
        edVerifyPW = findViewById(R.id.edVarifyPW);
        spinnerRole = findViewById(R.id.spinner);
        btnRegister = findViewById(R.id.btnRegister);
        checkBoxAgreeTerms = findViewById(R.id.checkBox);
        txtLoginFromRegister = findViewById(R.id.txtLoginFromRegister);
        txtGuestMode = findViewById(R.id.txtGuestMode); // Ánh xạ TextView từ activity_register.xml
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> registerUser());

        txtLoginFromRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Xử lý sự kiện cho TextView "Chế độ khách" (txtGuestMode) trên màn hình RegisterActivity
        if (txtGuestMode != null) { // Đảm bảo View này tồn tại trong layout
            txtGuestMode.setOnClickListener(v -> {
                GuestModeHandler.enterGuestMode(RegisterActivity.this, mAuth);
                finish(); // Đóng RegisterActivity sau khi chuyển hướng
            });
        }
    }

    private void registerUser() {
        String fullName = edFullName.getText().toString().trim();
        String email = edEmailRegister.getText().toString().trim();
        String password = edPasswordRegister.getText().toString().trim();
        String verifyPassword = edVerifyPW.getText().toString().trim();
        String selectedRole = spinnerRole.getSelectedItem().toString();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(verifyPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            edEmailRegister.setError("Email không hợp lệ.");
            return;
        }
        if (password.length() < 6) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }
        if (!password.equals(verifyPassword)) {
            edVerifyPW.setError("Mật khẩu xác nhận không khớp.");
            return;
        }
        if (!checkBoxAgreeTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng chấp nhận các điều khoản.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();

        db.collection("Users")
                .whereEqualTo("Email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            hideProgressBar();
                            Toast.makeText(RegisterActivity.this, "Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.", Toast.LENGTH_LONG).show();
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, authTask -> {
                                        if (authTask.isSuccessful()) {
                                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                            if (firebaseUser != null) {
                                                saveUserToFirestore(firebaseUser.getUid(), fullName, email, selectedRole);
                                            }
                                        } else {
                                            hideProgressBar();
                                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e(TAG, "Đăng ký thất bại", authTask.getException());
                                        }
                                    });
                        }
                    } else {
                        hideProgressBar();
                        Log.e(TAG, "Lỗi khi kiểm tra email tồn tại.", task.getException());
                        Toast.makeText(RegisterActivity.this, "Lỗi kiểm tra email. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String fullName, String email, String role) {
        User user = new User();
        user.setUserId(uid);
        user.setFullName(fullName);
        user.setUserClass(role);
        user.setAvatarUrl("");

        db.collection("Users").document(uid).set(user)
                .addOnCompleteListener(userSaveTask -> {
                    if (userSaveTask.isSuccessful()) {
                        Map<String, Object> roleData = new HashMap<>();
                        roleData.put("userId", uid);
                        roleData.put("role", role);
                        roleData.put("assignedAt", new Date());

                        db.collection("UserRoles").add(roleData)
                                .addOnCompleteListener(roleSaveTask -> {
                                    hideProgressBar();
                                    if (roleSaveTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                                        // Sau khi đăng ký thành công, xóa trạng thái khách (nếu có)
                                        GuestModeHandler.setGuestModePreference(RegisterActivity.this, false);
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e(TAG, "Lưu vai trò vào Firestore (UserRoles) thất bại.", roleSaveTask.getException());
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công nhưng lỗi khi lưu vai trò.", Toast.LENGTH_LONG).show();
                                    }
                                });

                    } else {
                        hideProgressBar();
                        Log.e(TAG, "Lưu thông tin người dùng vào Firestore (Users) thất bại.", userSaveTask.getException());
                        Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin người dùng.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnRegister.setEnabled(false);
        edFullName.setEnabled(false);
        edEmailRegister.setEnabled(false);
        edPasswordRegister.setEnabled(false);
        edVerifyPW.setEnabled(false);
        spinnerRole.setEnabled(false);
        checkBoxAgreeTerms.setEnabled(false);
        txtLoginFromRegister.setEnabled(false);
        if (txtGuestMode != null) txtGuestMode.setEnabled(false);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnRegister.setEnabled(true);
        edFullName.setEnabled(true);
        edEmailRegister.setEnabled(true);
        edPasswordRegister.setEnabled(true);
        edVerifyPW.setEnabled(true);
        spinnerRole.setEnabled(true);
        checkBoxAgreeTerms.setEnabled(true);
        txtLoginFromRegister.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true);
    }
}