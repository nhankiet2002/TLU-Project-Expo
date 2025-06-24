//Các hàm tiện ích liên quan đến UI.
package com.cse441.tluprojectexpo.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.cse441.tluprojectexpo.admin.utils.AppToast;
import com.google.android.material.textfield.TextInputLayout;

public class UiHelper {

    public static void showInfoDialog(Context context, String title, String message) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .show();
    }

    public static void showToast(Context context, String message, int duration) {
        if (context == null) return;
        AppToast.show(context, message, duration);
    }


    public static void setupDropdownToggle(TextInputLayout textInputLayout, AutoCompleteTextView autoCompleteTextView) {
        if (textInputLayout == null || autoCompleteTextView == null) return;
        textInputLayout.setEndIconOnClickListener(v -> {
            if (!autoCompleteTextView.isPopupShowing()) autoCompleteTextView.showDropDown();
            else autoCompleteTextView.dismissDropDown();
        });
        autoCompleteTextView.setOnClickListener(v -> {
            if (!autoCompleteTextView.isPopupShowing()) autoCompleteTextView.showDropDown();
        });
    }

    public static void synchronizeButtonWidthsAfterLayout(View rootView, final Runnable onLayoutComplete) {
        if (rootView == null) return;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                if (onLayoutComplete != null) {
                    onLayoutComplete.run();
                }
            }
        });
    }

    public static void setViewWidth(View view, int width) {
        if (view == null || width <= 0) return;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        view.setLayoutParams(params);
    }

    public static int getMaxWidth(View... views) {
        int maxWidth = 0;
        for (View view : views) {
            if (view != null && view.getVisibility() == View.VISIBLE && view.getWidth() > maxWidth) {
                maxWidth = view.getWidth();
            }
        }
        return maxWidth;
    }
}