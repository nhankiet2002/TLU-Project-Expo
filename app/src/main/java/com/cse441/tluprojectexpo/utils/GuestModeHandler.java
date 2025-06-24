package com.cse441.tluprojectexpo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.cse441.tluprojectexpo.MainActivity; // Đảm bảo đúng package của MainActivity
import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GuestModeHandler {

    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_LAST_EMAIL = "last_email"; // Dùng để lưu email cuối cùng đăng nhập
    private static final String KEY_IS_GUEST_MODE = "isGuestMode";
    private static final String TAG = "GuestModeHandler";

    /**
     * Xử lý logic để vào chế độ khách: đăng xuất người dùng hiện tại (nếu có),
     * lưu trạng thái khách vào SharedPreferences và chuyển hướng đến MainActivity.
     *
     * @param context Context của Activity gọi.
     * @param mAuth Đối tượng FirebaseAuth để đăng xuất người dùng hiện tại.
     */
    public static void enterGuestMode(Context context, FirebaseAuth mAuth) {
        // Đảm bảo không còn tài khoản Firebase Auth nào đang được đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mAuth.signOut(); // Đăng xuất nếu có người dùng đang đăng nhập
            Log.d(TAG, "Đã đăng xuất người dùng hiện tại (" + currentUser.getEmail() + ") để vào chế độ khách.");
        }

        // Lưu trạng thái là chế độ khách vào SharedPreferences
        setGuestModePreference(context, true);

        // Tắt chức năng "nhớ tài khoản" khi vào chế độ khách để không tự động đăng nhập lại bằng tài khoản thật
        saveRememberMeStatePreference(context, "", false);

        AppToast.show(context, "Đang vào chế độ khách...", Toast.LENGTH_SHORT);
        navigateToMainActivity(context); // Chuyển thẳng vào MainActivity
    }

    /**
     * Lưu trạng thái chế độ khách vào SharedPreferences.
     * Các Activity khác có thể gọi hàm này để bật/tắt chế độ khách một cách nhất quán.
     *
     * @param context Context của Activity.
     * @param isGuest Trạng thái khách (true/false).
     */
    public static void setGuestModePreference(Context context, boolean isGuest) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_GUEST_MODE, isGuest);
        editor.apply();
        Log.d(TAG, "Đặt trạng thái isGuestMode: " + isGuest);
    }

    /**
     * Lưu trạng thái "nhớ đăng nhập" vào SharedPreferences.
     *
     * @param context Context của Activity.
     * @param email Email của người dùng (nếu nhớ), hoặc rỗng nếu không nhớ.
     * @param remember True nếu muốn nhớ, false nếu không.
     */
    public static void saveRememberMeStatePreference(Context context, String email, boolean remember) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_REMEMBER_LOGIN, remember);
        if (remember) {
            editor.putString(KEY_LAST_EMAIL, email);
        } else {
            editor.remove(KEY_LAST_EMAIL); // Xóa email nếu không nhớ
        }
        editor.apply();
        Log.d(TAG, "Đặt trạng thái remember_login: " + remember + ", email: " + email);
    }

    /**
     * Chuyển hướng đến MainActivity và xóa tất cả các Activity trên stack.
     *
     * @param context Context của Activity.
     */
    private static void navigateToMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        // Lưu ý: finish() không thể gọi từ đây, Activity gọi cần tự gọi finish() nếu muốn đóng nó.
    }
}