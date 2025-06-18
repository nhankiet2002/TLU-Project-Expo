// app/src/main/java/com/cse441.tluprojectexpo/utils/InputValidationUtils.java
package com.cse441.tluprojectexpo.utils;

import android.text.TextUtils;
import android.util.Patterns; // Import Patterns cho kiểm tra email

public class InputValidationUtils {

    /**
     * Kiểm tra xem chuỗi có rỗng hoặc chỉ chứa khoảng trắng hay không.
     *
     * @param text Chuỗi cần kiểm tra.
     * @return true nếu chuỗi không rỗng và không chỉ chứa khoảng trắng, false nếu ngược lại.
     */
    public static boolean isNotEmpty(String text) {
        return !TextUtils.isEmpty(text) && !text.trim().isEmpty();
    }

    /**
     * Kiểm tra xem email có hợp lệ hay không.
     *
     * @param email Chuỗi email cần kiểm tra.
     * @return true nếu email hợp lệ, false nếu ngược lại.
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Kiểm tra xem mật khẩu có đáp ứng độ dài tối thiểu hay không.
     * (Ví dụ: tối thiểu 6 ký tự)
     *
     * @param password Chuỗi mật khẩu cần kiểm tra.
     * @return true nếu mật khẩu hợp lệ (đáp ứng độ dài), false nếu ngược lại.
     */
    public static boolean isValidPassword(String password) {
        // Mật khẩu phải có ít nhất 6 ký tự
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    // Bạn có thể thêm các hàm validate khác ở đây
    // Ví dụ: isValidPhoneNumber, isValidUsername, v.v.
}