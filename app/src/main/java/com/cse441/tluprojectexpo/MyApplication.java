// MyApplication.java (Đặt trong package gốc của bạn)
package com.cse441.tluprojectexpo;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import com.cse441.tluprojectexpo.utils.NotificationHelper;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Cấu hình Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "devrh3pid");
        config.put("api_key", "716836116263789");
        config.put("api_secret", "ugFoenLx6va0q5IyKRmPzibFXKA");
        // config.put("secure", "true"); // Tùy chọn: Luôn sử dụng HTTPS
        MediaManager.init(this, config);
        NotificationHelper.createNotificationChannel(getApplicationContext());
    }
}