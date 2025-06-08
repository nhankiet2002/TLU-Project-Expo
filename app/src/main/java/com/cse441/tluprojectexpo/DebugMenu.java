package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    }
}