package com.lazamelezi.soundrecorder.Helper;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

public class util {

    public static void toggleStatusBarColor(Activity activity, int mColor) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(mColor);
    }

}
