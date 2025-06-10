package com.cse441.tluprojectexpo.fragment;

// Trong package fragment hoặc dialog của bạn
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.cse441.tluprojectexpo.R; // Thay thế bằng package của bạn

public class SuccessNotificationDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "success_message";
    private String message;

    public interface OnDismissListener {
        void onDialogDismissed();
    }
    private OnDismissListener dismissListener;

    public static SuccessNotificationDialogFragment newInstance(String message) {
        SuccessNotificationDialogFragment fragment = new SuccessNotificationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE, "Thao tác thành công");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_success_notification, container, false);
        TextView tvSuccessMessage = view.findViewById(R.id.tv_success_message);
        ImageView ivClose = view.findViewById(R.id.iv_close_success_dialog);

        tvSuccessMessage.setText(message);
        ivClose.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL); // Hiển thị ở trên cùng, giữa màn hình

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int dialogWidth = (int)(displayMetrics.widthPixels * 0.90); // 90% chiều rộng
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDialogDismissed();
        }
    }
}
