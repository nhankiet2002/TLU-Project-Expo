package com.cse441.tluprojectexpo; // Đảm bảo đúng package của bạn

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView; // Import cho Spinner
import android.widget.ArrayAdapter; // Import cho ArrayAdapter
import android.widget.CheckBox; // Import cho CheckBox
import android.widget.ProgressBar;
import android.widget.Spinner; // Import cho Spinner
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cse441.tluprojectexpo.utils.Constants;

import com.cse441.tluprojectexpo.model.User;

import com.cse441.tluprojectexpo.utils.InputValidationUtils;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText edFullName, edEmailRegister, edPasswordRegister, edVarifyPW; // Đổi tên biến cho khớp
    private Spinner spinnerRole; // Spinner cho Vai trò người dùng
    private CheckBox checkBoxAgreeTerms; // CheckBox đồng ý điều khoản
    private MaterialButton btnRegister;
    private TextView txtLoginFromRegister; // Nút "Đã có tài khoản? Đăng nhập"
    private ProgressBar progressBar; // ProgressBar của bạn

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole = Constants.ROLE_STUDENT; // Mặc định là Student

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupSpinner();
        setListeners();
    }

    private void initViews() {
        edFullName = findViewById(R.id.edEmailLogin); // Đây là "Tên người dùng" trong layout của bạn
        edEmailRegister = findViewById(R.id.edEmailRegister);
        edPasswordRegister = findViewById(R.id.edPasswordRegister);
        edVarifyPW = findViewById(R.id.edVarifyPW); // Xác nhận mật khẩu
        spinnerRole = findViewById(R.id.spinner); // Spinner
        checkBoxAgreeTerms = findViewById(R.id.checkBox); // CheckBox
        btnRegister = findViewById(R.id.btnRegister);
        txtLoginFromRegister = findViewById(R.id.txtLoginFromRegister); // "Đã có tài khoản? Đăng nhập"
        progressBar = findViewById(R.id.progressBar); // ProgressBar
    }
    private void setupSpinner() {
        String[] roles = {Constants.ROLE_STUDENT, Constants.ROLE_FACULTY, Constants.ROLE_ADMIN};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected role: " + selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRole = Constants.ROLE_STUDENT;
            }
        });
    }

    private void setListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        txtLoginFromRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fullName = edFullName.getText().toString().trim();
        String email = edEmailRegister.getText().toString().trim();
        String password = edPasswordRegister.getText().toString().trim();
        String confirmPassword = edVarifyPW.getText().toString().trim();

        // Validate inputs
        if (!InputValidationUtils.isNotEmpty(fullName)) {
            edFullName.setError("Tên người dùng không được để trống!");
            edFullName.requestFocus();
            return;
        }
        if (!InputValidationUtils.isValidEmail(email)) {
            edEmailRegister.setError("Email không hợp lệ!");
            edEmailRegister.requestFocus();
            return;
        }
        if (!InputValidationUtils.isValidPassword(password)) { // Kiểm tra độ dài >= 6 ký tự
            edPasswordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự!");
            edPasswordRegister.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edVarifyPW.setError("Mật khẩu xác nhận không khớp!");
            edVarifyPW.requestFocus();
            return;
        }
        if (!checkBoxAgreeTerms.isChecked()) {
            Toast.makeText(this, "Bạn phải chấp nhận các điều khoản để đăng ký.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();

        // Tạo tài khoản Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Tạo tài khoản Firebase Auth thành công: " + firebaseUser.getUid());

                            // Cập nhật tên hiển thị trong Firebase Authentication (displayName)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "Cập nhật display name thành công.");
                                        } else {
                                            Log.w(TAG, "Lỗi khi cập nhật display name.", profileTask.getException());
                                        }
                                    });

                            // Gửi email xác thực (tùy chọn nhưng rất nên có)
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(emailVerificationTask -> {
                                        if (emailVerificationTask.isSuccessful()) {
                                            Log.d(TAG, "Email xác thực đã được gửi.");
                                            Toast.makeText(this, "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.", Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.e(TAG, "Lỗi khi gửi email xác thực.", emailVerificationTask.getException());
                                            Toast.makeText(this, "Đăng ký thành công nhưng không gửi được email xác thực. Vui lòng kiểm tra lại email.", Toast.LENGTH_LONG).show();
                                        }
                                    });

                            // Tạo đối tượng User để lưu vào Firestore (sử dụng User model bạn đã tạo)
                            User newUser = new User();
                            newUser.setUserId(firebaseUser.getUid());
                            newUser.setEmail(email);
                            newUser.setFullName(fullName);
                            newUser.setAvatarUrl("https://i.pravatar.cc/150?u=" + firebaseUser.getUid());
                            newUser.setRole(selectedRole); // Vai trò người dùng từ Spinner

                            // Gán permissions mặc định dựa trên vai trò
                            List<String> permissions = new ArrayList<>();
                            if (Constants.ROLE_STUDENT.equals(selectedRole)) {
                                permissions.add(Constants.PERMISSION_CREATE_PROJECT);
                                permissions.add(Constants.PERMISSION_VIEW_OWN_PROJECTS);
                                permissions.add(Constants.PERMISSION_COMMENT);
                                permissions.add(Constants.PERMISSION_VOTE);
                                permissions.add(Constants.PERMISSION_DELETE_OWN_PROJECT);
                                permissions.add(Constants.PERMISSION_EDIT_OWN_PROJECT);
                                permissions.add(Constants.PERMISSION_VIEW_ALL_PROJECTS);
                            } else if (Constants.ROLE_FACULTY.equals(selectedRole)) {
                                // Gán quyền cho Faculty
                                permissions.add(Constants.PERMISSION_VIEW_ALL_PROJECTS);
                                permissions.add(Constants.PERMISSION_COMMENT);
                                permissions.add(Constants.PERMISSION_APPROVE_CONTENT); // Ví dụ quyền bảo trợ
                            }
                            newUser.setPermissions(permissions);
                            newUser.setCreatedAt(new Date());
                            newUser.setIsEmailVerified(false); // Ban đầu chưa xác thực

                            // Lưu thông tin người dùng vào Firestore
                            db.collection(Constants.COLLECTION_USERS)
                                    .document(firebaseUser.getUid()) // Dùng UID làm document ID
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Thông tin người dùng đã được lưu vào Firestore.");
                                        hideProgressBar();
                                        // Chuyển sang màn hình đăng ký thành công của bạn
                                        Intent successIntent = new Intent(RegisterActivity.this, RegisterSuccessfulActivity.class);
                                        startActivity(successIntent);
                                        finish(); // Đóng RegisterActivity
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi khi lưu thông tin người dùng vào Firestore.", e);
                                        Toast.makeText(this, "Lỗi khi lưu thông tin người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // Nếu lưu Firestore thất bại, bạn có thể cân nhắc xóa tài khoản Auth vừa tạo
                                        firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                                            if (deleteTask.isSuccessful()) {
                                                Log.d(TAG, "Tài khoản Auth đã được xóa do lỗi Firestore.");
                                            }
                                        });
                                        hideProgressBar();
                                    });
                        }
                    } else {
                        hideProgressBar();
                        Log.w(TAG, "Đăng ký thất bại.", task.getException());
                        Toast.makeText(this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
        edVarifyPW.setEnabled(false);
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
        edVarifyPW.setEnabled(true);
        spinnerRole.setEnabled(true);
        checkBoxAgreeTerms.setEnabled(true);
        txtLoginFromRegister.setEnabled(true);
    }
}