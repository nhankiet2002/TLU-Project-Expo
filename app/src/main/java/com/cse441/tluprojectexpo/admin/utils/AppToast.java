package com.cse441.tluprojectexpo.admin.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import com.cse441.tluprojectexpo.R;

// Lớp này không cần được khởi tạo, chỉ chứa các phương thức tĩnh
public final class AppToast {

    // Constructor private để ngăn việc tạo đối tượng từ bên ngoài
    private AppToast() {}

    /**
     * Phương thức chính để hiển thị Toast tùy chỉnh.
     * @param context Context của ứng dụng.
     * @param message Tin nhắn cần hiển thị.
     */
    public static void show(Context context, String message) {
        // Cảnh báo: Custom Toast không hoạt động tốt trên Android 11+ nếu app ở dưới nền.
        // Tuy nhiên, nếu bạn chỉ gọi nó khi app đang mở thì vẫn ổn.

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);

        // Lấy TextView và thiết lập nội dung
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        // Icon đã được thiết lập sẵn trong file XML, bạn không cần thay đổi ở đây.
        // ImageView icon = layout.findViewById(R.id.toast_icon);
        // icon.setImageResource(R.drawable.your_icon); // Chỉ làm vậy nếu bạn muốn icon thay đổi động

        // Tạo và hiển thị Toast
        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Phiên bản quá tải (overload) cho phép truyền thời gian hiển thị.
     */
    public static void show(Context context, String message, int duration) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(duration); // duration là Toast.LENGTH_SHORT hoặc Toast.LENGTH_LONG
        toast.setView(layout);
        toast.show();
    }
}