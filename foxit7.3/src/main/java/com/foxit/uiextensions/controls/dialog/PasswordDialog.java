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
import android.content.DialogInterface;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;

import com.foxit.uiextensions.utils.AppUtil;

public class PasswordDialog {
    public interface IPasswordDialogListener {

        /**
         * Called when this password dialog is confirmed.
         * @param password The input password.
         */
        void onConfirm(byte[] password);
        /**
         * Called when this password dialog is dismissed.
         */
        void onDismiss();
        /**
         * Called when the back key is pressed.
         */
        void onKeyBack();
    }
    private UITextEditDialog mDialog;
    public PasswordDialog(Context context, final IPasswordDialogListener listener) {
        mDialog = new UITextEditDialog(context);
        mDialog.getDialog().setCanceledOnTouchOutside(false);
        mDialog.getInputEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onConfirm(mDialog.getInputEditText().getText().toString().getBytes());
                }
            }
        });

        mDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onDismiss();
                }
            }
        });

        mDialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mDialog.getDialog().cancel();
                    if (listener != null) {
                        listener.onKeyBack();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public PasswordDialog setTitle(String title) {
        mDialog.setTitle(title);
        return this;
    }

    public PasswordDialog setPromptTips(String tips) {
        mDialog.getPromptTextView().setText(tips);
        return this;
    }

    public void show() {
        mDialog.show();
        AppUtil.showSoftInput(mDialog.getInputEditText());
    }

    private void dismiss() {
        mDialog.dismiss();
        AppUtil.dismissInputSoft(mDialog.getInputEditText());
    }
}
