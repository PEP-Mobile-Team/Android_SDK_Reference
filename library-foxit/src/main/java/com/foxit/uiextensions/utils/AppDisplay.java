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
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.lang.reflect.Method;

@SuppressLint("NewApi")
public class AppDisplay {
    private Context mContext;
    private DisplayMetrics mMetrics;
    private int mWidthPixels;
    private int mHeightPixels;
    private boolean mPadDevice;

    private static AppDisplay mAppDisplay = null;

    public static AppDisplay getInstance(Context context) {
        if (mAppDisplay == null) {
            mAppDisplay = new AppDisplay(context);
        }
        return mAppDisplay;
    }

    /**
     * Device DPI
     * 120 === LDPI
     * 160 === MDPI
     * 213 === TVDPI
     * 240 === HDPI
     * 320 === XHDPI
     * 480 === XXHDPI
     */
    public AppDisplay(Context context) {
        mContext = context;
        mMetrics = context.getResources().getDisplayMetrics();
        Log.d("AppDisplay", "DPI:" + mMetrics.densityDpi);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (Build.VERSION.SDK_INT < 13) {
            mWidthPixels = mMetrics.widthPixels;
            mHeightPixels = mMetrics.heightPixels;
        } else if (Build.VERSION.SDK_INT == 13) {
            try {
                Method methodWidth = Display.class.getMethod("getRealWidth");
                Method methodHeight = Display.class.getMethod("getRealHeight");
                mWidthPixels = (Integer) methodWidth.invoke(display);
                mHeightPixels = (Integer) methodHeight.invoke(display);
            } catch (Exception e) {
                mWidthPixels = mMetrics.widthPixels;
                mHeightPixels = mMetrics.heightPixels;
            }
        } else if (Build.VERSION.SDK_INT > 13 && Build.VERSION.SDK_INT < 17) {
            try {
                Method methodWidth = Display.class.getMethod("getRawWidth");
                Method methodHeight = Display.class.getMethod("getRawHeight");
                mWidthPixels = (Integer) methodWidth.invoke(display);
                mHeightPixels = (Integer) methodHeight.invoke(display);
            } catch (Exception e) {
                mWidthPixels = mMetrics.widthPixels;
                mHeightPixels = mMetrics.heightPixels;
            }
        } else if (Build.VERSION.SDK_INT >= 17) {
            display.getRealMetrics(mMetrics);

            mWidthPixels = mMetrics.widthPixels;
            mHeightPixels = mMetrics.heightPixels;
        }

        float diagonalPixels = (float) Math.sqrt(Math.pow(getRawScreenWidth(), 2) + Math.pow(getRawScreenHeight(), 2));
        float screenSize = diagonalPixels / mMetrics.densityDpi;
        if (screenSize < 7) {
            mPadDevice = false;
        } else if (screenSize >= 7 && screenSize < 8 && mMetrics.densityDpi < 160) {
            mPadDevice = false;
        } else {
            mPadDevice = true;
        }
    }

    public int dp2px(float value) {
        return (int) (value * mMetrics.density + 0.5);
    }

    public float px2dp(float pxValue) {
        return pxValue / mMetrics.density;
    }

    public int getScreenWidth() {
        return mMetrics.widthPixels;
    }

    public int getScreenHeight() {
        return mMetrics.heightPixels;
    }

    public int getDialogWidth() {
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return getScreenHeight() * 4 / 5;
        }
        return getScreenWidth() * 4 / 5;
    }

    public int getUITextEditDialogWidth() {
        if (isPad()) {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return getScreenHeight() * 3 / 5;
            }
            return getScreenWidth() * 3 / 5;
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return getScreenHeight() * 4 / 5;
            }
            return getScreenWidth() * 4 / 5;
        }
    }

    public int getDialogHeight() {
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return getScreenWidth() / 2;
        }
        return getScreenHeight() / 2;
    }

    public boolean isLandscape() {
        if (getScreenWidth() > getScreenHeight()) {
            return true;
        } else {
            return false;
        }
    }

    public int getRawScreenWidth() {
        if (isLandscape()) {
            return Math.max(mWidthPixels, mHeightPixels);
        } else {
            return Math.min(mWidthPixels, mHeightPixels);
        }
    }

    public int getRawScreenHeight() {
        if (isLandscape()) {
            return Math.min(mWidthPixels, mHeightPixels);
        } else {
            return Math.max(mWidthPixels, mHeightPixels);
        }
    }

    public boolean isPad() {
        return mPadDevice;
    }

    public int getRealNavBarHeight() {
        int result = 0;

        if (hasNavigationBar()) {
            int resourceId = mContext.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return mContext.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    public int getNavBarHeight() {
        int result = 0;

        if (hasNavigationBar()) {
            if (isLandscape()) {
                return 0;
            }

            int resourceId = mContext.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return mContext.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    private boolean hasNavigationBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return false;
        }
        // Emulator
        if (isEmulator()) {
            return true;
        }
        boolean hasMenuKey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        return !hasMenuKey && !hasBackKey;
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.toLowerCase().contains("vbox")
                || Build.FINGERPRINT.toLowerCase().contains("test-keys")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

}
