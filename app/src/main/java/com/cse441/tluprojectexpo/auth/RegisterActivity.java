package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern; // Import cho Regex

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText edFullName, edEmailRegister, edPasswordRegister, edVerifyPW;
    private Spinner spinnerRole;
    private CheckBox checkBoxAgreeTerms;
    private MaterialButton btnRegister;
    private TextView txtLoginFromRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedRoleDisplayName = ""; // Tên hiển thị vai trò từ Spinner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edFullName = findViewById(R.id.edEmailLogin);
        edEmailRegister = findViewById(R.id.edEmailRegister);
        edPasswordRegister = findViewById(R.id.edPasswordRegister);
        edVerifyPW = findViewById(R.id.edVarifyPW);
        spinnerRole = findViewById(R.id.spinner);
        checkBoxAgreeTerms = findViewById(R.id.checkBox);
        btnRegister = findViewById(R.id.btnRegister);
        txtLoginFromRegister = findViewById(R.id.txtLoginFromRegister);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.user_roles,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRoleDisplayName = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRoleDisplayName = "";
            }
        });

        btnRegister.setOnClickListener(v -> {
            registerUser();
        });

        txtLoginFromRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        showProgressBar();

        String fullName = edFullName.getText().toString().trim();
        String email = edEmailRegister.getText().toString().trim();
        String password = edPasswordRegister.getText().toString().trim();
        String verifyPassword = edVerifyPW.getText().toString().trim();

        // --- Kiểm tra Validation cơ bản ---
        if (TextUtils.isEmpty(fullName)) {
            edFullName.setError("Vui lòng nhập họ tên đầy đủ.");
            edFullName.requestFocus();
            hideProgressBar();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edEmailRegister.setError("Vui lòng nhập Email.");
            edEmailRegister.requestFocus();
            hideProgressBar();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edPasswordRegister.setError("Vui lòng nhập Mật khẩu.");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        if (TextUtils.isEmpty(verifyPassword)) {
            edVerifyPW.setError("Vui lòng nhập lại Mật khẩu xác nhận.");
            edVerifyPW.requestFocus();
            hideProgressBar();
            return;
        }
        if (!password.equals(verifyPassword)) {
            edVerifyPW.setError("Mật khẩu xác nhận không khớp.");
            edVerifyPW.requestFocus();
            hideProgressBar();
            return;
        }

        // --- KIỂM TRA ĐỘ PHỨC TẠP CỦA MẬT KHẨU ---
        if (password.length() < 6) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự.");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        // Kiểm tra ít nhất một chữ cái viết hoa
        Pattern upperCasePattern = Pattern.compile(".*[A-Z].*");
        if (!upperCasePattern.matcher(password).matches()) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất một chữ cái viết hoa.");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        // Kiểm tra ít nhất một chữ cái viết thường
        Pattern lowerCasePattern = Pattern.compile(".*[a-z].*");
        if (!lowerCasePattern.matcher(password).matches()) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất một chữ cái viết thường.");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        // Kiểm tra ít nhất một chữ số
        Pattern digitPattern = Pattern.compile(".*\\d.*");
        if (!digitPattern.matcher(password).matches()) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất một chữ số.");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        // Kiểm tra ít nhất một ký tự đặc biệt (ví dụ: !@#$%^&+=_.-)
        // Bạn có thể tùy chỉnh tập hợp ký tự đặc biệt này
        Pattern specialCharPattern = Pattern.compile(".*[!@#$%^&+=_.-].*");
        if (!specialCharPattern.matcher(password).matches()) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất một ký tự đặc biệt (ví dụ: !@#$%^&+=_.-).");
            edPasswordRegister.requestFocus();
            hideProgressBar();
            return;
        }
        // --- KẾT THÚC KIỂM TRA ĐỘ PHỨC TẠM ---

        if (!checkBoxAgreeTerms.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý với các điều khoản và chính sách bảo mật.", Toast.LENGTH_SHORT).show();
            hideProgressBar();
            return;
        }
        if (TextUtils.isEmpty(selectedRoleDisplayName) || selectedRoleDisplayName.equals("Chọn vai trò")) {
            Toast.makeText(this, "Vui lòng chọn vai trò của bạn.", Toast.LENGTH_SHORT).show();
            hideProgressBar();
            return;
        }

        // --- Băm mật khẩu người dùng nhập vào để lưu vào Firestore và kiểm tra trùng lặp ---
        String hashedPassword = generateSHA256Hash(password);
        if (hashedPassword == null) {
            Toast.makeText(RegisterActivity.this, "Lỗi khi xử lý mật khẩu. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            hideProgressBar();
            return;
        }

        checkDuplicateHashedPasswordAndRegister(fullName, email, password, hashedPassword, selectedRoleDisplayName);
    }

    // Phương thức để tạo hàm băm SHA-256 từ một chuỗi
    private String generateSHA256Hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            // Chuyển đổi mảng byte sang chuỗi Base64 để lưu trữ
            return Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not found.", e);
            return null;
        }
    }

    // Phương thức kiểm tra trùng lặp mật khẩu đã băm trong Firestore
    private void checkDuplicateHashedPasswordAndRegister(String fullName, String email, String plainPassword, String hashedPassword, String roleDisplayName) {
        db.collection("Users")
                .whereEqualTo("PasswordHash", hashedPassword)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            hideProgressBar();
                            Toast.makeText(RegisterActivity.this, "Mật khẩu này đã được sử dụng bởi một tài khoản khác.", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Mật khẩu đã băm trùng lặp được tìm thấy trong trường PasswordHash của Firestore.");
                        } else {
                            performFirebaseAuthRegistration(fullName, email, plainPassword, hashedPassword, roleDisplayName);
                        }
                    } else {
                        hideProgressBar();
                        Log.e(TAG, "Lỗi khi kiểm tra trùng lặp mật khẩu trong Firestore.", task.getException());
                        Toast.makeText(RegisterActivity.this, "Lỗi hệ thống khi kiểm tra mật khẩu. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Phương thức thực hiện đăng ký với Firebase Authentication
    private void performFirebaseAuthRegistration(String fullName, String email, String plainPassword, String hashedPassword, String roleDisplayName) {
        mAuth.createUserWithEmailAndPassword(email, plainPassword) // Vẫn truyền mật khẩu plaintext cho Firebase Auth
                .addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d(TAG, "Email xác minh đã được gửi.");
                                            saveUserAndRoleToFirestore(firebaseUser.getUid(), email, fullName, hashedPassword, roleDisplayName);
                                        } else {
                                            Log.e(TAG, "Gửi email xác minh thất bại.", emailTask.getException());
                                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công nhưng không gửi được email xác minh.", Toast.LENGTH_LONG).show();
                                            hideProgressBar();
                                        }
                                    });

                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", authTask.getException());
                        hideProgressBar();
                        String errorMessage = "Đăng ký thất bại.";
                        if (authTask.getException() != null) {
                            errorMessage += " Lỗi: " + authTask.getException().getMessage();
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Phương thức để chuyển đổi tên hiển thị vai trò sang RoleId
    private String getRoleIdFromDisplayName(String displayName) {
        switch (displayName) {
            case "Admin":
                return "role_admin";
            case "Sinh viên":
                return "role_user";
            default:
                return "role_user"; // Mặc định là sinh viên
        }
    }

    // Phương thức để lưu thông tin người dùng vào collection 'Users' và vai trò vào collection 'UserRoles'
    private void saveUserAndRoleToFirestore(String uid, String email, String fullName, String hashedPassword, String roleDisplayName) {
        // --- 1. Lưu thông tin cơ bản vào collection 'Users' ---
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("Email", email);
        userMap.put("FullName", fullName);
        userMap.put("PasswordHash", hashedPassword); // Lưu mật khẩu đã băm vào đây
        userMap.put("Class", ""); // Để trống nếu không có giá trị cụ thể lúc đăng ký
        userMap.put("AvatarUrl", "https://i.pravatar.cc/150?u=" + email);
        userMap.put("CreatedAt", new Date());
        userMap.put("isLocked", false);

        db.collection("Users").document(uid)
                .set(userMap)
                .addOnCompleteListener(userSaveTask -> {
                    if (userSaveTask.isSuccessful()) {
                        Log.d(TAG, "Thông tin người dùng đã được lưu vào Firestore (Users).");

                        // --- 2. Lưu vai trò vào collection 'UserRoles' ---
                        String roleId = getRoleIdFromDisplayName(roleDisplayName);

                        Map<String, Object> userRoleMap = new HashMap<>();
                        userRoleMap.put("RoleId", roleId);
                        userRoleMap.put("UserId", uid);

                        db.collection("UserRoles")
                                .add(userRoleMap)
                                .addOnCompleteListener(roleSaveTask -> {
                                    hideProgressBar();
                                    if (roleSaveTask.isSuccessful()) {
                                        Log.d(TAG, "Vai trò người dùng đã được lưu vào Firestore (UserRoles). Document ID: " + roleSaveTask.getResult().getId());
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email để xác minh.", Toast.LENGTH_LONG).show();

                                        Intent intent = new Intent(RegisterActivity.this, CheckEmailActivity.class);
                                        intent.putExtra("email", email);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e(TAG, "Lưu vai trò người dùng vào Firestore (UserRoles) thất bại.", roleSaveTask.getException());
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
    }
}