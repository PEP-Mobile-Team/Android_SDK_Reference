/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;


import com.foxit.uiextensions.R;

import java.lang.reflect.Field;

@TargetApi(Build.VERSION_CODES.DONUT)
public class AppTheme {
    public static int getDialogTheme() {
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 11) {
            theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
        } else {
            theme = R.style.rv_dialog_style;
        }
        return theme;
    }

    public static void setThemeNoTitle(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static void setThemeNeedMenuKey(Activity activity) {
        if (Build.VERSION.SDK_INT >= 22) {
            try {
                WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
                Field field = WindowManager.LayoutParams.class.getField("needsMenuKey");
                field.setInt(layoutParams, WindowManager.LayoutParams.class.getField("NEEDS_MENU_SET_TRUE").getInt(null));
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        } else {
            try {
                activity.getWindow().addFlags(WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null));
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static void setThemeFullScreen(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @SuppressLint("NewApi")
    public static int getThemeFullScreen() {
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 11) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
        }
        return theme;
    }

    public static int getThemeNoTitle() {
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar;
        } else if (Build.VERSION.SDK_INT >= 11) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar;
        }
        return theme;
    }


}
