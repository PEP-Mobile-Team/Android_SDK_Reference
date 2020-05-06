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
package com.foxit.uiextensions.modules.signature;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.LayoutConfig;


public class SignatureMixListPopup {
    public interface ISignatureListEvent {
        void onSignatureListDismiss();
    }

    private SignatureListPicker mListPicker;
    private PopupWindow mPopupWindow;
    private ViewGroup mParent;
    private Context mContext;
    private ISignatureListEvent mListEvent;
    private PDFViewCtrl mPDFViewCtrl;

    public SignatureMixListPopup(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl, SignatureFragment.SignatureInkCallback inkCallback, boolean isFromSignatureField) {
        this.mParent = parent;
        this.mContext = context;
        mPDFViewCtrl = pdfViewCtrl;

        mListPicker = new SignatureListPicker(context, parent, pdfViewCtrl, inkCallback, isFromSignatureField);
        mListPicker.init(new SignatureListPicker.ISignListPickerDismissCallback() {
            @Override
            public void onDismiss(boolean isShowAnnotMenu) {
                mPopupWindow.dismiss();
            }
        });

        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(mListPicker.getRootView(), RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT, true);
        }
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
        mPopupWindow.setAnimationStyle(R.style.View_Animation_RtoL);
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mListPicker.getBaseItemsSize() == 0) {
                    ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_NORMAL);
                }
                mListPicker.dismiss();
                ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getMainFrame().hideMaskView();
                if (mListEvent != null) {
                    mListEvent.onSignatureListDismiss();
                }

                showToolbars();
            }
        });
    }

    public void show() {
        mListPicker.loadData();

        showSystemUI();
        Rect rect = new Rect();
        mParent.getGlobalVisibleRect(rect);
        int top = rect.top;
        int right = rect.right;
        int screenWidth = AppDisplay.getInstance(mContext).getRawScreenWidth();

        int width = mParent.getWidth();
        int height = mParent.getHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPopupWindow.setWidth(width);
        mPopupWindow.setHeight(height);
        mPopupWindow.showAtLocation(mParent, Gravity.RIGHT | Gravity.TOP, screenWidth - right, top);
    }

    public void update(int width, int height) {
        showSystemUI();
        Rect rect = new Rect();
        mParent.getGlobalVisibleRect(rect);
        int top = rect.top;
        int right = rect.right;
        int screenWidth = AppDisplay.getInstance(mContext).getRawScreenWidth();

        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPopupWindow.update(screenWidth - right, top, width, height);
    }

    private void showToolbars(){
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(true);
        mainFrame.showToolbars();
    }

    private void showSystemUI(){
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame)uiExtensionsManager.getMainFrame();
        if (mainFrame.isToolbarsVisible()){
            mainFrame.setHideSystemUI(false);
        } else {
            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
        }
    }

    public PopupWindow getPopWindow() {
        return mPopupWindow;
    }

    public void setSignatureListEvent(ISignatureListEvent event) {
        mListEvent = event;
    }

}
