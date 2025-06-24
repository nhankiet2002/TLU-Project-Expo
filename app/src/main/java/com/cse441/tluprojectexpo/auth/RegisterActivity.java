package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.cse441.tluprojectexpo.model.User;
import com.cse441.tluprojectexpo.utils.GuestModeHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edFullName, edEmailRegister, edPasswordRegister, edVerifyPW;

    private MaterialButton btnRegister;
    private CheckBox checkBoxAgreeTerms;
    private TextView txtLoginFromRegister;
    private TextView txtGuestMode;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";

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
        edVerifyPW = findViewById(R.id.edVarifyPW); // ĐÃ SỬA TỪ edVarifyPW SANG edVerifyPW
        btnRegister = findViewById(R.id.btnRegister);
        checkBoxAgreeTerms = findViewById(R.id.checkBox);
        txtLoginFromRegister = findViewById(R.id.txtLoginFromRegister);
        txtGuestMode = findViewById(R.id.txtGuestMode);
        progressBar = findViewById(R.id.progressBar);



        btnRegister.setOnClickListener(v -> registerUser());

        txtLoginFromRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        if (txtGuestMode != null) {
            txtGuestMode.setOnClickListener(v -> {
                GuestModeHandler.enterGuestMode(RegisterActivity.this, mAuth);
                finish();
            });
        }
    }

    private void registerUser() {
        String fullName = edFullName.getText().toString().trim();
        String email = edEmailRegister.getText().toString().trim();
        String password = edPasswordRegister.getText().toString().trim();
        String verifyPassword = edVerifyPW.getText().toString().trim();
        String selectedRole = "role_user";

        Log.d(TAG, "Dữ liệu đăng ký: FullName='" + fullName + "', Email='" + email + "', Role='" + selectedRole + "'");


        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(verifyPassword)) {
            AppToast.show(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT);
            return;
        }

        if (!isValidEmail(email)) {
            edEmailRegister.setError("Email không hợp lệ.");
            return;
        }

        // Cập nhật kiểm tra mật khẩu: yêu cầu chữ hoa, chữ thường, số, và ký tự đặc biệt
        if (!isValidPassword(password)) {
            edPasswordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return;
        }

        if (!password.equals(verifyPassword)) {
            edVerifyPW.setError("Mật khẩu xác nhận không khớp.");
            return;
        }
        if (!checkBoxAgreeTerms.isChecked()) {
            AppToast.show(this, "Vui lòng chấp nhận các điều khoản.", Toast.LENGTH_SHORT);
            return;
        }

        showProgressBar();

        // Kiểm tra xem email đã tồn tại chưa trước khi đăng ký
        db.collection("Users")
                .whereEqualTo("Email", email) // Nếu bạn lưu email trong Firestore document
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            hideProgressBar();
                            AppToast.show(RegisterActivity.this, "Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.", Toast.LENGTH_LONG);
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, authTask -> {
                                        if (authTask.isSuccessful()) {
                                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                            if (firebaseUser != null) {
                                                // Gửi link xác thực email
                                                firebaseUser.sendEmailVerification()
                                                        .addOnCompleteListener(verificationTask -> {
                                                            if (verificationTask.isSuccessful()) {
                                                                Log.d(TAG, "Email xác thực đã được gửi.");
                                                                AppToast.show(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email của bạn để xác thực tài khoản.", Toast.LENGTH_LONG);

                                                                // Hash mật khẩu trước khi lưu vào Firestore
                                                                String hashedPassword = hashPassword(password);
                                                                saveUserToFirestore(firebaseUser.getUid(), fullName, email, selectedRole, hashedPassword);
                                                            } else {
                                                                Log.e(TAG, "Không thể gửi email xác thực.", verificationTask.getException());
                                                                AppToast.show(RegisterActivity.this, "Đăng ký thành công nhưng không thể gửi email xác thực. Vui lòng thử lại sau.", Toast.LENGTH_LONG);
                                                                // Nếu không gửi được email xác thực, có thể ẩn progressBar và cho phép người dùng thử lại
                                                                hideProgressBar();
                                                            }
                                                        });
                                            }
                                        } else {
                                            hideProgressBar();
                                            AppToast.show(RegisterActivity.this, "Đăng ký thất bại: " + authTask.getException().getMessage(), Toast.LENGTH_LONG);
                                            Log.e(TAG, "Đăng ký thất bại", authTask.getException());
                                        }
                                    });
                        }
                    } else {
                        hideProgressBar();
                        Log.e(TAG, "Lỗi khi kiểm tra email tồn tại.", task.getException());
                        AppToast.show(RegisterActivity.this, "Lỗi kiểm tra email. Vui lòng thử lại.", Toast.LENGTH_SHORT);
                    }
                });
    }

    // Cập nhật signature để nhận hashedPassword
    private void saveUserToFirestore(String uid, String fullName, String email, String role, String hashedPassword) {
        User user = new User();
        user.setUserId(uid);
        user.setFullName(fullName);
        user.setClassName(""); // Giữ nguyên role cho UserClass
        user.setEmail(email); // ĐẶT EMAIL TỪ THAM S
        user.setLocked(false); // ĐẶT TRẠNG THÁI KHÔNG KHÓA MẶC ĐỊNH
        user.setPasswordHash(hashedPassword); // ĐẶT HASHED PASSWORD
        user.setAvatarUrl("https://i.pravatar.cc/150?u=" + email); // DÙNG EMAIL TỪ THAM SỐ CHO AVATAR URL

        db.collection("Users").document(uid).set(user)
                .addOnCompleteListener(userSaveTask -> {
                    if (userSaveTask.isSuccessful()) {
                        Map<String, Object> roleData = new HashMap<>();
                        roleData.put("UserId", uid);
                        roleData.put("RoleId", role);

                        db.collection("UserRoles").add(roleData)
                                .addOnCompleteListener(roleSaveTask -> {
                                    hideProgressBar();
                                    if (roleSaveTask.isSuccessful()) {
                                        // Đã chuyển Toast "Đăng ký thành công!" lên sau sendEmailVerification
                                        GuestModeHandler.setGuestModePreference(RegisterActivity.this, false);
                                        // Chuyển về màn hình OpenActivity
                                        Intent intent = new Intent(RegisterActivity.this, OpenActivity.class); // <-- THAY ĐỔI Ở ĐÂY
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e(TAG, "Lưu vai trò vào Firestore (UserRoles) thất bại.", roleSaveTask.getException());
                                        AppToast.show(RegisterActivity.this, "Đăng ký thành công nhưng lỗi khi lưu vai trò.", Toast.LENGTH_LONG);
                                    }
                                });

                    } else {
                        hideProgressBar();
                        Log.e(TAG, "Lưu thông tin người dùng vào Firestore (Users) thất bại.", userSaveTask.getException());
                        AppToast.show(RegisterActivity.this, "Lỗi khi lưu thông tin người dùng.", Toast.LENGTH_LONG);
                    }
                });
    }

    // Phương thức băm mật khẩu bằng SHA-256 và Base64
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            // Chuyển byte array sang chuỗi Base64
            return Base64.encodeToString(hash, Base64.NO_WRAP); // NO_WRAP để không thêm ký tự xuống dòng
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Lỗi thuật toán băm SHA-256: " + e.getMessage());
            // Xử lý lỗi, ví dụ: trả về null hoặc một chuỗi lỗi
            return null;
        }
    }

    /**
     * Kiểm tra xem mật khẩu có đáp ứng các yêu cầu về độ mạnh hay không.
     * Yêu cầu: ít nhất 6 ký tự, có chữ hoa, chữ thường, số, và ký tự đặc biệt.
     * Ký tự đặc biệt bao gồm: !@#$%^&*()_+-=[]{};':"\|,<>.?
     * @param password Mật khẩu cần kiểm tra.
     * @return true nếu mật khẩu hợp lệ, ngược lại là false.
     */
    private boolean isValidPassword(String password) {
        // Regex giải thích:
        // (?=.*[0-9]): Phải chứa ít nhất một chữ số
        // (?=.*[a-z]): Phải chứa ít nhất một chữ cái viết thường
        // (?=.*[A-Z]): Phải chứa ít nhất một chữ cái viết hoa
        // (?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]): Phải chứa ít nhất một ký tự đặc biệt
        // .{6,}: Phải có ít nhất 6 ký tự (hoặc nhiều hơn)
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
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
        checkBoxAgreeTerms.setEnabled(true);
        txtLoginFromRegister.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true);
    }
}