package com.mingkem.paijo.cobakamera;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class Utils {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Size getScreenSize(@NonNull Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }
}
