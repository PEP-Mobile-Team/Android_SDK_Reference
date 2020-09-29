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
package com.foxit.uiextensions.controls.toolbar.impl;

import android.content.Context;
import android.view.View;

import com.foxit.uiextensions.R;


public class AnnotBar extends BaseBarImpl {
    private int mSidesInterval = 16;

    public AnnotBar(Context context) {
        super(context, VERTICAL);
        initDimens(context);
    }

    @Override
    public View getContentView() {
        if (!mInterval) {
            if (mOrientation == HORIZONTAL) {
                mRootLayout.setPadding(dip2px_fromDimens(mSidesInterval), 0, dip2px_fromDimens(mSidesInterval), 0);
            } else {
                mRootLayout.setPadding(0, dip2px_fromDimens(mSidesInterval), 0, dip2px(5));
            }
        }
        return mRootLayout;
    }

    private void initDimens(Context context) {
        try {
            mSidesInterval = (int) context.getResources().getDimension(R.dimen.ux_text_icon_distance_phone);
        } catch (Exception e) {
            mSidesInterval = dip2px(mSidesInterval);
        }
    }
}
