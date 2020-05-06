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

import android.app.AlertDialog;
import android.content.Context;
import androidx.core.widget.TextViewCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppSystemUtil;

public class FxProgressDialog {

    private AlertDialog mDialog;
    private TextView mTextView;
    public FxProgressDialog(Context context, String tips) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_progress_dialog_layout, null);
        mTextView = view.findViewById(R.id.progress_tip);
        if (!TextUtils.isEmpty(tips)) {
            mTextView.setText(tips);
        }

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(mTextView, 8, 20, 2, TypedValue.COMPLEX_UNIT_SP);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setView(view);

        mDialog = builder.create();
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(mDialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            mDialog.getWindow().setAttributes(layoutParams);
        }
    }

    public void show() {
        // Set the dialog to not focusable (makes navigation ignore us adding the window)
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        mDialog.show();
        AppSystemUtil.hideSystemUI(mDialog.getWindow());
        //Clear the not focusable flag from the window
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public FxProgressDialog setTips(String tips) {
        if (!TextUtils.isEmpty(tips)) {
            mTextView.setText(tips);
        }

        return this;
    }
}
