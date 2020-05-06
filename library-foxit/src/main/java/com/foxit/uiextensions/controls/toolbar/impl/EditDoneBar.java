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
package com.foxit.uiextensions.controls.toolbar.impl;

import android.content.Context;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;


public class EditDoneBar extends BaseBarImpl {
    public EditDoneBar(Context context) {
        super(context);
        initDimens(context);
    }

    private void initDimens(Context context) {
        try {
            if (AppDisplay.getInstance(context).isPad()) {
                mDefaultSpace = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_button_interval_small_pad);
            } else {
                mDefaultSpace = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_button_interval_small_phone);
            }
        } catch (Exception e) {
            mDefaultSpace = dip2px(4);
        }
    }
}