package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
                    FirestoreUtils.importUsersFromJson(DebugMenu.this, db, "User.json");
                    Toast.makeText(DebugMenu.this, "Thêm dữ liệu thành công", Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    Toast.makeText(DebugMenu.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}