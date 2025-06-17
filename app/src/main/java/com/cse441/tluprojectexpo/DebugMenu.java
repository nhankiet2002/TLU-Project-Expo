package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.model.FirestoreUtils;
import com.google.firebase.firestore.FirebaseFirestore;

public class DebugMenu extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_menu);

        Button btnLaunchCatalog = findViewById(R.id.btn_launch_catalog_management);
        btnLaunchCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo Intent để mở CatalogManagementPage
                Intent intent = new Intent(DebugMenu.this, CatalogManagementPage.class);
                startActivity(intent);
            }
        });

        Button addUsers = findViewById(R.id.btn_launch_add_user);
        addUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                try {
                    FirestoreUtils.importUsersFromJson(DebugMenu.this, db, "Users.json");
                    Toast.makeText(DebugMenu.this, "Thêm dữ liệu thành công", Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    Toast.makeText(DebugMenu.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });

        //Thêm mới một field vào tất cả các document trong collection
        Button addField = findViewById(R.id.btn_add_field_to_collection);
        addField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFieldDialog();
            }
        });

        Button deleteField = findViewById(R.id.btn_delete_field_from_collection);
        deleteField.setOnClickListener(v -> {
            showDeleteFieldDialog();
        });

    }

    private void showAddFieldDialog() {
        // Tạo một đối tượng AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate layout tùy chỉnh cho dialog
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_field, null);
        builder.setView(dialogView);

        // Ánh xạ các EditText từ layout của dialog
        final EditText etCollectionName = dialogView.findViewById(R.id.et_collection_name);
        final EditText etFieldName = dialogView.findViewById(R.id.et_field_name);
        final EditText etDefaultValue = dialogView.findViewById(R.id.et_default_value);

        // Thiết lập các nút cho dialog
        builder.setPositiveButton("Thêm", (dialog, which) -> {
            // Logic xử lý khi người dùng nhấn nút "Thêm"
            // Được set là null ở đây để chúng ta có thể override và ngăn dialog tự đóng khi nhập liệu sai
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            // Người dùng nhấn "Hủy", dialog sẽ tự động đóng
            dialog.cancel();
        });

        // Tạo và hiển thị dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Override nút Positive để kiểm tra dữ liệu trước khi đóng dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Lấy dữ liệu từ EditTexts
            String collectionName = etCollectionName.getText().toString().trim();
            String fieldName = etFieldName.getText().toString().trim();
            String defaultValue = etDefaultValue.getText().toString().trim(); // Hiện tại chỉ lấy giá trị là String

            // Kiểm tra xem người dùng đã nhập đủ thông tin chưa
            if (TextUtils.isEmpty(collectionName)) {
                etCollectionName.setError("Tên collection không được để trống!");
                return;
            }
            if (TextUtils.isEmpty(fieldName)) {
                etFieldName.setError("Tên field không được để trống!");
                return;
            }
            if (TextUtils.isEmpty(defaultValue)) {
                etDefaultValue.setError("Giá trị mặc định không được để trống!");
                return;
            }

            // Nếu mọi thứ hợp lệ, gọi hàm của FirestoreUtils
            performAddField(collectionName, fieldName, defaultValue);

            // Đóng dialog sau khi đã xử lý
            dialog.dismiss();
        });
    }

    /**
     * Thực hiện việc gọi hàm trong FirestoreUtils và hiển thị Toast.
     * @param collectionName Tên collection
     * @param fieldName Tên field
     * @param defaultValue Giá trị mặc định
     */
    private void performAddField(String collectionName, String fieldName, Object defaultValue) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirestoreUtils.addFieldToAllDocuments(db, collectionName, fieldName, defaultValue);

        String message = "Đang xử lý thêm field '" + fieldName + "' vào collection '" + collectionName + "'...";
        Toast.makeText(DebugMenu.this, message, Toast.LENGTH_LONG).show();
    }


    private void showDeleteFieldDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate layout tùy chỉnh
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_delete_field, null);
        builder.setView(dialogView);

        // Ánh xạ các EditText
        final EditText etCollectionName = dialogView.findViewById(R.id.et_collection_name_delete);
        final EditText etFieldName = dialogView.findViewById(R.id.et_field_name_delete);

        // Thiết lập các nút
        builder.setPositiveButton("Xóa", null); // Set null để override
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override nút Positive để kiểm tra dữ liệu
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String collectionName = etCollectionName.getText().toString().trim();
            String fieldToDelete = etFieldName.getText().toString().trim();

            if (TextUtils.isEmpty(collectionName)) {
                etCollectionName.setError("Tên collection không được để trống!");
                return;
            }
            if (TextUtils.isEmpty(fieldToDelete)) {
                etFieldName.setError("Tên field không được để trống!");
                return;
            }

            // Gọi hàm thực thi
            performDeleteField(collectionName, fieldToDelete);

            // Đóng dialog
            dialog.dismiss();
        });
    }

    private void performDeleteField(String collectionName, String fieldToDelete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Gọi hàm tiện ích bạn đã có
        FirestoreUtils.deleteFieldFromAllDocuments(db, collectionName, fieldToDelete);

        String message = "Đang xử lý xóa field '" + fieldToDelete + "' khỏi collection '" + collectionName + "'...";
        Toast.makeText(DebugMenu.this, message, Toast.LENGTH_LONG).show();
    }
}

