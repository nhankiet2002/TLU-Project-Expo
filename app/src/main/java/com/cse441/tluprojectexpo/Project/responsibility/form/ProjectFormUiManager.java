package com.cse441.tluprojectexpo.Project.responsibility.form;


import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;

import com.cse441.tluprojectexpo.R; // Cần R để lấy string array
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Nếu cần trả về Map

/**
 * Quản lý các trường input tĩnh của form tạo dự án.
 * Bao gồm lấy giá trị, đặt giá trị, reset và có thể cả validation cơ bản.
 */
public class ProjectFormUiManager {

    private TextInputEditText etProjectName, etProjectDescription, etTechnology;
    private AutoCompleteTextView actvCategory, actvStatus;
    private TextInputLayout tilProjectName, tilProjectDescription, tilCategory, tilTechnology, tilStatus;

    private Context context; // Cần context để lấy string array cho status

    private List<String> categoryNameListRef; // Tham chiếu đến list category từ CreateFragment
    private Map<String, String> categoryNameToIdMapRef; // Tham chiếu đến map category từ CreateFragment
    private ArrayAdapter<String> categoryAdapter;

    private List<String> statusNameList; // List status nội bộ
    private ArrayAdapter<String> statusAdapter;

    /**
     * Constructor.
     * @param rootView View gốc chứa các input field.
     * @param context Context.
     * @param categoryNameListRef Tham chiếu đến danh sách tên category từ CreateFragment.
     * @param categoryNameToIdMapRef Tham chiếu đến map category name-id từ CreateFragment.
     */
    public ProjectFormUiManager(View rootView, Context context,
                                List<String> categoryNameListRef,
                                Map<String, String> categoryNameToIdMapRef) {
        this.context = context;
        this.categoryNameListRef = categoryNameListRef;
        this.categoryNameToIdMapRef = categoryNameToIdMapRef;

        // Ánh xạ views
        etProjectName = rootView.findViewById(R.id.et_project_name);
        etProjectDescription = rootView.findViewById(R.id.et_project_description);
        etTechnology = rootView.findViewById(R.id.et_technology);
        actvCategory = rootView.findViewById(R.id.actv_category);
        actvStatus = rootView.findViewById(R.id.actv_status);

        tilProjectName = rootView.findViewById(R.id.til_project_name);
        tilProjectDescription = rootView.findViewById(R.id.til_project_description);
        tilCategory = rootView.findViewById(R.id.til_category);
        tilTechnology = rootView.findViewById(R.id.til_technology);
        tilStatus = rootView.findViewById(R.id.til_status);

        setupAdapters();
    }

    private void setupAdapters() {
        // Category Adapter
        if (categoryNameListRef != null && actvCategory != null) {
            categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, categoryNameListRef);
            actvCategory.setAdapter(categoryAdapter);
        }

        // Status Adapter
        statusNameList = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.project_statuses)));
        if (actvStatus != null) {
            statusAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, statusNameList);
            actvStatus.setAdapter(statusAdapter);
            if (!statusNameList.isEmpty()) {
                actvStatus.setText(statusNameList.get(0), false); // Đặt giá trị mặc định
            }
        }
    }

    /**
     * Lấy dữ liệu từ các trường input của form.
     * @return Một Map chứa dữ liệu form. Keys có thể là "projectName", "description", "categoryName", "technology", "statusName".
     */
    public Map<String, String> getFormData() {
        Map<String, String> formData = new HashMap<>();
        formData.put("projectName", getTextFromInput(etProjectName));
        formData.put("description", getTextFromInput(etProjectDescription));
        formData.put("categoryName", getTextFromInput(actvCategory));
        formData.put("technology", getTextFromInput(etTechnology));
        formData.put("statusName", getTextFromInput(actvStatus));
        return formData;
    }

    public String getProjectName() { return getTextFromInput(etProjectName); }
    public String getDescription() { return getTextFromInput(etProjectDescription); }
    public String getCategoryName() { return getTextFromInput(actvCategory); }
    public String getSelectedCategoryId() { // Lấy ID của category đã chọn
        String categoryName = getCategoryName();
        if (categoryNameToIdMapRef != null && !TextUtils.isEmpty(categoryName)) {
            return categoryNameToIdMapRef.get(categoryName);
        }
        return null;
    }
    public String getTechnologyInput() { return getTextFromInput(etTechnology); }
    public String getStatusName() { return getTextFromInput(actvStatus); }


    /**
     * Validate toàn bộ form.
     * @return true nếu tất cả các trường bắt buộc hợp lệ, false nếu ngược lại.
     */
    public boolean validateForm() {
        boolean isNameValid = ProjectFormDelegate.isProjectNameValid(getProjectName(), tilProjectName);


        return isNameValid;
    }

    /**
     * Reset tất cả các trường input trong form về giá trị mặc định.
     */
    public void resetForm() {
        if (etProjectName != null) etProjectName.setText("");
        if (tilProjectName != null) tilProjectName.setError(null);

        if (etProjectDescription != null) etProjectDescription.setText("");
        if (tilProjectDescription != null) tilProjectDescription.setError(null);

        if (actvCategory != null) actvCategory.setText("", false); // false để không filter khi đặt text
        if (tilCategory != null) tilCategory.setError(null);

        if (etTechnology != null) etTechnology.setText("");
        if (tilTechnology != null) tilTechnology.setError(null);

        if (actvStatus != null && !statusNameList.isEmpty()) {
            actvStatus.setText(statusNameList.get(0), false); // Đặt lại giá trị mặc định
        } else if (actvStatus != null) {
            actvStatus.setText("", false);
        }
        if (tilStatus != null) tilStatus.setError(null);

        if (etProjectName != null) etProjectName.requestFocus(); // Focus lại trường đầu tiên
    }


    /**
     * Cập nhật lại adapter cho category dropdown.
     * Được gọi từ CreateFragment khi danh sách category từ Firestore thay đổi.
     */
    public void notifyCategoryAdapterChanged() {
        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
    }

    // Hàm tiện ích để lấy text, tránh lặp code
    private String getTextFromInput(@Nullable TextInputEditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    private String getTextFromInput(@Nullable AutoCompleteTextView autoCompleteTextView) {
        if (autoCompleteTextView != null && autoCompleteTextView.getText() != null) {
            return autoCompleteTextView.getText().toString().trim();
        }
        return "";
    }
}
