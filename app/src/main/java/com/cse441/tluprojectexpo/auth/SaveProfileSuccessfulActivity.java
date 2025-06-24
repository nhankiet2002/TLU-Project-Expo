package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;

public class SaveProfileSuccessfulActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_profile_successfull);

        Button btnBack = findViewById(R.id.txtBackToMainActivity);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SaveProfileSuccessfulActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
