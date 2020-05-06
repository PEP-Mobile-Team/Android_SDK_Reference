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
package com.foxit.uiextensions.modules.panel;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.LayoutConfig;

public class PanelWindow {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;

    public PanelWindow(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context.getApplicationContext();
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;

        init();
    }

    public void init() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelHost = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanel();
            mPanelPopupWindow = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanelWindow();
        }

        if (mPanelPopupWindow == null) {
            mPanelPopupWindow = new PopupWindow(mPanelHost.getContentView(), RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
            mPanelPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                }
            });
        }

        if (mPanelHost == null) {
            mPanelHost = new PanelHostImpl(mContext, new PanelHost.ICloseDefaultPanelCallback() {
                @Override
                public void closeDefaultPanel(View v) {
                    if (mPanelPopupWindow != null && mPanelPopupWindow.isShowing()) {
                        mPanelPopupWindow.dismiss();
                    }
                }
            });
        }
    }

    public void show(PanelSpec.PanelType panelType) {
        ViewGroup rootView = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int width = rootView.getWidth();
        int height = rootView.getHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPanelPopupWindow.setWidth(width);
        mPanelPopupWindow.setHeight(height);
        mPanelPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mPanelHost.setCurrentSpec(panelType);
        mPanelPopupWindow.showAtLocation(rootView, Gravity.LEFT | Gravity.TOP, x, y);
    }

    public void update() {
        int rootWidth = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().getWidth();
        int rootHeight = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().getHeight();

        boolean bVertical = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int width, height;
        if (bVertical) {
            height = Math.max(rootWidth, rootHeight);
            width = Math.min(rootWidth, rootHeight);
        } else {
            height = Math.min(rootWidth, rootHeight);
            width = Math.max(rootWidth, rootHeight);
        }
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
        }
        mPanelPopupWindow.update(width, height);
    }

    public void update(int width, int height) {
        int[] location = new int[2];
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPanelPopupWindow.update(x, y, width, height);
    }


    public PopupWindow getPanelWindow() {
        return mPanelPopupWindow;
    }

    public PanelHost getPanelHost() {
        return mPanelHost;
    }

    public PanelSpec getCurrentPanelSpec() {
        return mPanelHost.getCurrentSpec();
    }

    public boolean isShowing() {
        return mPanelPopupWindow.isShowing();
    }

    public void dismiss() {
        mPanelPopupWindow.dismiss();
    }

    public void addPanelSpec(PanelSpec panelSpec) {
        mPanelHost.addSpec(panelSpec);
    }

    public void removePanelSpec(PanelSpec panelSpec) {
        mPanelHost.removeSpec(panelSpec);
    }
}
