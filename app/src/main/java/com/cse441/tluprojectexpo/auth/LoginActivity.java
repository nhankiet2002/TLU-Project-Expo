package com.cse441.tluprojectexpo.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.utils.GuestModeHandler; // Import lớp tiện ích
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText edEmailLogin, edPasswordLogin;
    private Button btnLogin;
    private TextView txtForgotPassword, txtRegister, txtGuestMode;
    private ProgressBar progressBar;
    private CheckBox cbRememberMe;

    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_LAST_EMAIL = "last_email";
    private static final String KEY_IS_GUEST_MODE = "isGuestMode";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edEmailLogin = findViewById(R.id.edFullName);
        edPasswordLogin = findViewById(R.id.edPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtRegister = findViewById(R.id.txtRegister);
        progressBar = findViewById(R.id.progressBar);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        txtGuestMode = findViewById(R.id.txtGuestMode); // Ánh xạ txtGuestMode

        mAuth = FirebaseAuth.getInstance();

        loadRememberMeState();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        txtRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý click cho chế độ khách - Gọi từ GuestModeHandler
        if (txtGuestMode != null) {
            txtGuestMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GuestModeHandler.enterGuestMode(LoginActivity.this, mAuth);
                    finish(); // Đóng LoginActivity sau khi chuyển hướng
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadRememberMeState() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_LOGIN, false);
        String lastEmail = prefs.getString(KEY_LAST_EMAIL, "");

        cbRememberMe.setChecked(rememberMe);
        if (rememberMe && !TextUtils.isEmpty(lastEmail)) {
            edEmailLogin.setText(lastEmail);
            edPasswordLogin.requestFocus();
        }
    }

    private void saveRememberMeState(String email, boolean remember) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_REMEMBER_LOGIN, remember);
        if (remember) {
            editor.putString(KEY_LAST_EMAIL, email);
        } else {
            editor.remove(KEY_LAST_EMAIL);
        }
        editor.apply();
    }

    private void attemptLogin() {
        String email = edEmailLogin.getText().toString().trim();
        String password = edPasswordLogin.getText().toString().trim();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edEmailLogin.getWindowToken(), 0);
        }

        if (TextUtils.isEmpty(email)) {
            edEmailLogin.setError("Email không được để trống!");
            edEmailLogin.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edPasswordLogin.setError("Mật khẩu không được để trống!");
            edPasswordLogin.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        hideProgressBar();

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveRememberMeState(email, cbRememberMe.isChecked());
                                // Đảm bảo thoát chế độ khách khi đăng nhập thành công
                                GuestModeHandler.setGuestModePreference(LoginActivity.this, false);

                                if (user.isEmailVerified()) {
                                    Log.d(TAG, "Đăng nhập thành công: Email đã xác minh.");
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                } else {
                                    Log.d(TAG, "Đăng nhập thành công: Email chưa xác minh.");
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công! Vui lòng kiểm tra email để xác minh tài khoản của bạn.", Toast.LENGTH_LONG).show();
                                    navigateToMainActivity();
                                }
                            }
                        } else {
                            Log.w(TAG, "Đăng nhập thất bại", task.getException());
                            String errorMessage;
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "Email này chưa được đăng ký. Vui lòng đăng ký tài khoản.";
                                edEmailLogin.setError(errorMessage);
                                edEmailLogin.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Sai mật khẩu hoặc email. Vui lòng thử lại.";
                                edPasswordLogin.setError(errorMessage);
                                edPasswordLogin.requestFocus();
                            } else {
                                errorMessage = "Đăng nhập thất bại: " + task.getException().getMessage();
                            }
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnLogin.setEnabled(false);
        edEmailLogin.setEnabled(false);
        edPasswordLogin.setEnabled(false);
        txtRegister.setEnabled(false);
        txtForgotPassword.setEnabled(false);
        cbRememberMe.setEnabled(false);
        if (txtGuestMode != null) txtGuestMode.setEnabled(false); // Vô hiệu hóa chế độ khách
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnLogin.setEnabled(true);
        edEmailLogin.setEnabled(true);
        edPasswordLogin.setEnabled(true);
        txtRegister.setEnabled(true);
        txtForgotPassword.setEnabled(true);
        cbRememberMe.setEnabled(true);
        if (txtGuestMode != null) txtGuestMode.setEnabled(true); // Kích hoạt lại chế độ khách
    }
}