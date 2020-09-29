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
package com.foxit.uiextensions.modules.panel;

import android.content.Context;
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
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.LayoutConfig;

/** The panel management, which manage all kinds of panels including bookmark, outline, annotations, file attachment... */
public class PanelManager implements IPanelManager {

    private Context mContext;
    private ViewGroup mRootView;
    private PanelHost mPanel;
    private PopupWindow mPanelPopupWindow;
    private IPanelManager.OnShowPanelListener mShowListener = null;

    public PanelManager(Context context, PDFViewCtrl.UIExtensionsManager uiExtensionsManager, ViewGroup parent, PopupWindow.OnDismissListener dismissListener) {
        mContext = context;
        mRootView = parent;
        ((UIExtensionsManager)uiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);

        mPanel = new PanelHostImpl(mContext, new PanelHost.ICloseDefaultPanelCallback() {
            @Override
            public void closeDefaultPanel(View v) {
                if (mPanelPopupWindow != null && mPanelPopupWindow.isShowing()) {
                    mPanelPopupWindow.dismiss();
                }
            }
        });
        setPanelView(mPanel.getContentView(), dismissListener);
    }

    private void setPanelView(final View view, PopupWindow.OnDismissListener dismissListener) {
        mPanelPopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
        mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
        if (dismissListener != null) {
            mPanelPopupWindow.setOnDismissListener(dismissListener);
        }
    }

    @Override
    public PanelHost getPanel() {
        return mPanel;
    }

    @Override
    public PopupWindow getPanelWindow() {
        return mPanelPopupWindow;
    }

    @Override
    public void setOnShowPanelListener(OnShowPanelListener listener) {
        mShowListener = listener;
    }

    @Override
    public void showPanel() {
        if (mPanel.getCurrentSpec() == null) {
            showPanel(PanelSpec.PanelType.Outline);
        } else {
            showPanel(mPanel.getCurrentSpec().getPanelType());
        }
    }

    @Override
    public void showPanel(PanelSpec.PanelType panelType) {
        int[] location = new int[2];
        mRootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int width = mRootView.getWidth();
        int height = mRootView.getHeight();
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
        resetPanelFocus(panelType);
        mPanelPopupWindow.showAtLocation(mRootView, Gravity.LEFT | Gravity.TOP, x, y);
        if (mShowListener != null) {
            mShowListener.onShow();
        }
    }

    private void updatePanel(int width, int height) {
        int[] location = new int[2];
        mRootView.getLocationOnScreen(location);
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
        if (mShowListener != null) {
            mShowListener.onShow();
        }
    }

    private void resetPanelFocus(PanelSpec.PanelType panelType) {
        if (panelType != null) {
            mPanel.setCurrentSpec(panelType);
        }
    }

    @Override
    public void hidePanel() {
        if (mPanelPopupWindow.isShowing()) {
            mPanelPopupWindow.dismiss();
        }
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mPanelPopupWindow != null && mPanelPopupWindow.isShowing()) {
                if (oldWidth != newWidth || oldHeight != newHeight) {
                    updatePanel(newWidth, newHeight);
                }
            }
        }
    };

}
