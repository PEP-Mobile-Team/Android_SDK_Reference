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

import android.view.View;

import com.foxit.uiextensions.controls.toolbar.BaseBar;

public interface MatchDialog {
    public interface DialogListener {
        public void onResult(long btType);

        public void onBackClick();
    }

    public interface DismissListener {
        public void onDismiss();
    }

    public static final long DIALOG_NO_BUTTON = 0x00000000;
    public static final long DIALOG_CANCEL = 0x00000001;
    public static final long DIALOG_SKIP = 0x00000002;
    public static final long DIALOG_OK = 0x00000004;
    public static final long DIALOG_OPEN_ONLY = 0x00000008;
    public static final long DIALOG_REPLACE = 0x00000010;
    public static final long DIALOG_COPY = 0x00000020;
    public static final long DIALOG_MOVE = 0x00000040;
    public static final long DIALOG_UPLOAD = 0x00000080;

    public static final int DLG_TITLE_STYLE_BG_BLUE = 1;
    public static final int DLG_TITLE_STYLE_BG_WHITE = 2;

    public View getRootView();

    public void setTitle(String title);

    public void setContentView(View contentView);

    public void setStyle(int style);

    public void setButton(long buttons);

    public void setBackButtonVisible(int visibility);

    public void setButtonEnable(boolean enable, long buttons);

    public void setTitleBlueLineVisible(boolean visible);

    public void setTitlePosition(BaseBar.TB_Position position);

    public void setWidth(int width);

    public void setHeight(int height);

    public void setFullScreenWithStatusBar();

    public boolean isShowing();

    public void showDialog();

    public void showDialog(boolean showMask);

    public void dismiss();

    public void showDialogNoManage();

    public void setListener(DialogListener dialogListener);

    public void setOnDLDismissListener(DismissListener dismissListener);

    public void setBackgroundColor(int color);
}
