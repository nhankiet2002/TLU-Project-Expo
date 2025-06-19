package com.cse441.tluprojectexpo.admin.utils;

import android.content.Context;
import android.content.Intent;

import com.cse441.tluprojectexpo.admin.Dashboard;

import java.io.Serializable;

public class NavigationUtil {

    public static void navigateTo(Context context, Class<?> destinationActivity) {
        Intent intent = new Intent(context, destinationActivity);
        context.startActivity(intent);
    }

    public static void navigateToDashboard(Context context) {
        Intent intent = new Intent(context, Dashboard.class);
        context.startActivity(intent);
    }

    public static void navigateWithObject(Context context, Class<?> destinationActivity, String key, Serializable object) {
        Intent intent = new Intent(context, destinationActivity);
        intent.putExtra(key, object);
        context.startActivity(intent);
    }
}
