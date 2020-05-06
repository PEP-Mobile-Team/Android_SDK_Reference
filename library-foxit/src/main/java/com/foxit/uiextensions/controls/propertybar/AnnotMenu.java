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
package com.foxit.uiextensions.controls.propertybar;

import android.graphics.RectF;
import android.view.View;
import android.widget.PopupWindow;

import java.util.ArrayList;

public interface AnnotMenu {
    public static interface ClickListener {
        public void onAMClick(int btType);
    }

    public static final int AM_BT_COPY = 1;
    public static final int AM_BT_DELETE = 2;
    public static final int AM_BT_COMMENT = 3;
    public static final int AM_BT_REPLY = 4;
    public static final int AM_BT_EDIT = 5;
    public static final int AM_BT_STYLE = 6;

    public static final int AM_BT_HIGHLIGHT = 7;
    public static final int AM_BT_UNDERLINE = 8;
    public static final int AM_BT_STRIKEOUT = 9;
    public static final int AM_BT_SQUIGGLY = 10;
    public static final int AM_BT_NOTE = 11;
    public static final int AM_BT_SIGNATURE = 12;

    public static final int AM_BT_SIGN_LIST = 13;
    public static final int AM_BT_SIGN = 14;
    public static final int AM_BT_CANCEL = 15;
    public static final int AM_BT_VERIFY_SIGNATURE = 16;

    public static final int AM_BT_PALY = 17;
    public static final int AM_BT_FLATTEN = 18;
    public static final int AM_BT_REDACT = 19;
    public static final int AM_BT_APPLY = 20;

    public PopupWindow getPopupWindow();

    public void setMenuItems(ArrayList<Integer> menuItems);

    public void setShowAlways(boolean showAlways);

    public void show(RectF rectF);

    public void show(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss);

    public void show(RectF rectF, View view);

    public void update(RectF rectF);

    public void update(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss);

    public void dismiss();

    public boolean isShowing();

    public void setListener(ClickListener listener);

}
