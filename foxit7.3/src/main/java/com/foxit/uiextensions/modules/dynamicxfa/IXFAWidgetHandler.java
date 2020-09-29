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
package com.foxit.uiextensions.modules.dynamicxfa;

import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.xfa.XFAWidget;

public interface IXFAWidgetHandler extends PDFViewCtrl.IDrawEventListener {
    void onXFAWidgetSelected(XFAWidget widget, boolean reRender);

    void onXFAWidgetDeselected(XFAWidget widget, boolean reRender);

    boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, XFAWidget widget);

    boolean onLongPress(int pageIndex, MotionEvent motionEvent, XFAWidget widget);

    boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, XFAWidget widget);
}
