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
package com.foxit.uiextensions.controls.dialog;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

public interface IPopupDialog {
    static final int POPUP_FROM_LEFT = 1;
    static final int POPUP_FROM_RIGHT = 2;

    View getRootView();

    void setOnDismissListener(PopupWindow.OnDismissListener onDismissListener);

    void setWidth(int width);

    void setHeight(int height);

    void update(int width, int height);

    void update(int x, int y, int width, int height);

    void showAtLocation(View parent, int gravity, int x, int y);

    void dismiss();

    boolean isShowing();

    void setAnimationStyle(int animationStyle);

    int getShadowLength();

    public static class DefaultMatric {

        static Rect getDefaultRightRect(Context context, PDFViewCtrl pdfViewCtrl, IPopupDialog popupDialog) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
            int width = uiExtensionsManager.getRootView().getWidth();
            int height = uiExtensionsManager.getRootView().getHeight();
            Rect rect = new Rect(0, 0, width, height);

            if (AppDisplay.getInstance(context).isPad()) {
                int shadowWidth = popupDialog.getShadowLength();
                rect.left = AppResource.getDimensionPixelSize(context, R.dimen.ux_screen_margin_icon) * 2 - shadowWidth;
                rect.top = uiExtensionsManager.getMainFrame().getTopToolbar().getContentView().getHeight() - shadowWidth;

                if (width > height) {
                    rect.right = (int) (width * 0.35f) + shadowWidth * 2;
                    rect.bottom = (int) (height * 0.6f) + shadowWidth * 2;
                } else {
                    rect.right = (int) (height * 0.35) + shadowWidth * 2;
                    rect.bottom = (int) (width * 0.6f) + shadowWidth * 2;
                }
            } else {
                if (width > height) {
                    width = (int) (width * 0.56f);
                    rect.left = (uiExtensionsManager.getRootView().getWidth() - width) / 2;
                }
            }
            return rect;
        }

        public static int getDefaultWidth(Context context, PDFViewCtrl pdfViewCtrl, IPopupDialog popupDialog) {
            Rect rect = getDefaultRightRect(context, pdfViewCtrl, popupDialog);
            return rect.right;
        }

        public static Point getDefaultRightTopForPad(Context context, PDFViewCtrl pdfViewCtrl, IPopupDialog popupDialog) {
            Rect rect = getDefaultRightRect(context, pdfViewCtrl, popupDialog);
            return new Point(rect.left, rect.top);
        }
    }
}