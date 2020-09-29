/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.panzoom.floatwindow;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppDisplay;


public class FloatWindowManager {

    private WindowManager mWidowManager;
    private WindowManager.LayoutParams mParams;

    private View mView;
    private boolean mIsAdded = false;

    public FloatWindowManager(Context context) {
        mWidowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= 24 && Build.VERSION.SDK_INT < 26) {
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        } else if (Build.VERSION.SDK_INT >= 26) {//Android O
            mParams.type = 2038;//WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            boolean hasPermission = (PackageManager.PERMISSION_GRANTED == packageManager.checkPermission("android.permission.SYSTEM_ALERT_WINDOW", packageName));
            if (hasPermission) {
                mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            }
        }

        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.gravity = Gravity.START | Gravity.TOP;

        View rootView = ((UIExtensionsManager) FloatWindowUtil.getInstance().getPdfViewCtrl().getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int rootWidth = rootView.getWidth();
        int rootHeight = rootView.getHeight();
        if (AppDisplay.getInstance(context).isLandscape()) {
            mParams.x = x + rootWidth / 2;
            mParams.y = y + rootHeight / 4;
        } else {
            mParams.x = x + rootWidth / 4;
            mParams.y = y + rootHeight / 2;
        }

        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }


    public void addFloatWindow(View view) {
        if (view == null) return;
        mView = view;
        mView.setLayoutParams(mParams);
        mWidowManager.addView(mView, mParams);
        mIsAdded = true;
    }

    public void removeFloatWindow() {
        boolean isAttached = true;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            isAttached = mView.isAttachedToWindow();
        }

        if (mIsAdded && isAttached && mWidowManager != null) {
            mWidowManager.removeView(mView);
            mIsAdded = false;
        }
    }
}
