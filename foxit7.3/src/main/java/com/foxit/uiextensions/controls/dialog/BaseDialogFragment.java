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
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppUtil;

public abstract class BaseDialogFragment extends DialogFragment {

    protected Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 13) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
        }
        setStyle(STYLE_NO_TITLE, theme);

        ((UIExtensionsManager) getPDFViewCtrl().getUIExtensionsManager()).registerLayoutChangeListener(mLayoutChangeListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = onCreateView(inflater, container);
        if (contentView != null)
            return contentView;
        else
            return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected abstract View onCreateView(LayoutInflater inflater, ViewGroup container);

    @NonNull
    protected abstract PDFViewCtrl getPDFViewCtrl();

    protected abstract void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight);

    @Override
    public void onStart() {
        super.onStart();
        refreshWindowLayout();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((UIExtensionsManager) getPDFViewCtrl().getUIExtensionsManager()).unregisterLayoutChangeListener(mLayoutChangeListener);
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (oldWidth != newWidth || oldHeight != newHeight) {
                refreshWindowLayout();
                BaseDialogFragment.this.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
            }
        }
    };

    private void refreshWindowLayout() {
        showSystemUI();

        View rootView = ((UIExtensionsManager) getPDFViewCtrl().getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = rootView.getWidth();
        int height = rootView.getHeight();

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowParams.dimAmount = 0.0f;
        windowParams.x = x;
        windowParams.y = y;
        windowParams.width = width;
        windowParams.height = height;
        window.setAttributes(windowParams);
        getDialog().setCanceledOnTouchOutside(true);
    }

    private void showSystemUI() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) getPDFViewCtrl().getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame) uiExtensionsManager.getMainFrame();
        if (mainFrame.isToolbarsVisible()) {
            mainFrame.setHideSystemUI(false);
        } else {
            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mDismissListener != null)
            mDismissListener.onDismiss();
    }

    private DismissListener mDismissListener;

    public void setDismissListener(DismissListener dismissListener) {
        mDismissListener = dismissListener;
    }

    public interface DismissListener {
        public void onDismiss();
    }

}
