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
package com.foxit.uiextensions.controls.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.UIMarqueeTextView;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;


public class UIDialog {
    protected Dialog mDialog;
    protected View mContentView;
    protected UIMarqueeTextView mTitleView;

    public UIDialog(Context context, int layoutId, int theme, int width) {
        mDialog = new Dialog(context, theme);
        mContentView = View.inflate(context.getApplicationContext(), layoutId, null);
        mTitleView = (UIMarqueeTextView) mContentView.findViewById(R.id.fx_dialog_title);

        mDialog.setContentView(mContentView, new LayoutParams(width, LayoutParams.WRAP_CONTENT));
        mDialog.setCanceledOnTouchOutside(true);
        AppUtil.fixBackgroundRepeat(mContentView);
    }

    UIDialog(Context context, int layoutId, int theme) {
        mDialog = new Dialog(context, theme);
        mContentView = View.inflate(context.getApplicationContext(), layoutId, null);
        mTitleView = (UIMarqueeTextView) mContentView.findViewById(R.id.fx_dialog_title);

        mDialog.setContentView(mContentView, new LayoutParams(AppDisplay.getInstance(context.getApplicationContext()).getDialogWidth(), LayoutParams.WRAP_CONTENT));
        mDialog.setCanceledOnTouchOutside(true);
        AppUtil.fixBackgroundRepeat(mContentView);
    }

    public Dialog getDialog() {
        return mDialog;
    }

    public void setTitle(String title) {
        if (null != title) {
            mTitleView.setText(title);
        }
    }

    public void setTitle(int title) {
        mTitleView.setText(title);
    }

    public void show() {
        AppDialogManager.getInstance().showAllowManager(mDialog, null);
    }

    public void dismiss() {
        AppDialogManager.getInstance().dismiss(mDialog);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        mDialog.setOnDismissListener(listener);
    }

    public void setOnCancelListener(OnCancelListener listener) {
        mDialog.setOnCancelListener(listener);
    }
}
