package com.cse441.tluprojectexpo.Project.responsibility.form;


import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout; // Cần để set/clear lỗi

public class ProjectFormDelegate {

    /**
     * Kiểm tra xem tên dự án có hợp lệ không.
     *
     * @param projectName    Tên dự án cần kiểm tra.
     * @param tilProjectName TextInputLayout tương ứng để hiển thị lỗi (có thể null).
     * @return true nếu hợp lệ, false nếu không.
     */
    public static boolean isProjectNameValid(String projectName, @Nullable TextInputLayout tilProjectName) {
        if (TextUtils.isEmpty(projectName)) {
            if (tilProjectName != null) {
                tilProjectName.setError("Tên dự án không được để trống.");
            }
            return false;
        }
        // Thêm các quy tắc khác nếu cần, ví dụ: độ dài tối thiểu/tối đa
        // if (projectName.length() < 5) {
        //     if (tilProjectName != null) tilProjectName.setError("Tên dự án phải có ít nhất 5 ký tự.");
        //     return false;
        // }
        if (tilProjectName != null) {
            tilProjectName.setError(null); // Xóa lỗi nếu hợp lệ
        }
        return true;
    }
}
