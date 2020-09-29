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
package com.foxit.uiextensions.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.Nullable;

public class AppResource {
    public static String getString(Context context, int id) {
        return context.getApplicationContext().getString(id);
    }

    public static String getString(Context context, int resId, Object... formatArgs) {
        return context.getApplicationContext().getString(resId, formatArgs);
    }

    public static int getDimensionPixelSize(Context context, int id) {
        return context.getResources().getDimensionPixelSize(id);
    }

    public static float getDimension(Context context, int id) {
        return context.getResources().getDimension(id);
    }

    public static Drawable getDrawable(Context context,int id){
        return context.getResources().getDrawable(id);
    }

    public static Drawable getDrawable(Context context, int id, @Nullable Resources.Theme theme){
        if (Build.VERSION.SDK_INT >= 21) {
            //Build.VERSION_CODES.LOLLIPOP
            return context.getResources().getDrawable(id, theme);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    public static int getColor(Context context, int id, @Nullable Resources.Theme theme) {
        if (Build.VERSION.SDK_INT >= 23) {
            // Build.VERSION_CODES.M
            return context.getResources().getColor(id, theme);
        } else {
            return context.getResources().getColor(id);
        }
    }
}