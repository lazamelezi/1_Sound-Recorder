package com.lazamelezi.soundrecorder.Helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class MySharedPreferences {

    private static final String MY_PREFS = "my_prefs";
    private static final String FIRST_TIME_PERMISSION_ASK = "first_time_permissions_ask";
    private static final String PREF_HIGH_QUALITY = "pref_high_quality";

    public static boolean isFirstTimeAskingPermission(Context context) {
        return context.getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE).getBoolean(FIRST_TIME_PERMISSION_ASK, true);
    }

    public static void firstTimeAskingPermission(Context context, boolean isFirstTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(FIRST_TIME_PERMISSION_ASK, isFirstTime).apply();
    }

    public static void setPrefHighQuality(Context context, boolean isEnabled) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_HIGH_QUALITY, isEnabled);
        editor.apply();
    }

    public static boolean getPrefHighQuality(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_HIGH_QUALITY, true);
    }
}
