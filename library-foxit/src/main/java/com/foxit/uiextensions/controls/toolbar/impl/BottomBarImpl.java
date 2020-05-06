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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


import com.foxit.uiextensions.R;

public class BottomBarImpl extends BaseBarImpl {
    private LinearLayout mBottomBarRealRootLayout = null;
    protected int mSolidLineHeight = 1;
    protected int mShadowLineHeight = 3;
    protected int mSidesInterval = 16;

    public BottomBarImpl(Context context) {
        super(context, HORIZONTAL);
        initDimens();
    }

    @Override
    public View getContentView() {
        if (mOrientation == HORIZONTAL && mBottomBarRealRootLayout == null) {
            mBottomBarRealRootLayout = new LinearLayout(mRootLayout.getContext());
            mBottomBarRealRootLayout.setOrientation(LinearLayout.VERTICAL);

            View shadowLine = new View(mRootLayout.getContext());
            shadowLine.setBackgroundResource(R.drawable.toolbar_shadow_bottom);
            mBottomBarRealRootLayout.addView(shadowLine);
            shadowLine.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            shadowLine.getLayoutParams().height = dip2px_fromDimens(mShadowLineHeight);

            View solidLine = new View(mRootLayout.getContext());
            solidLine.setBackgroundColor(mRootLayout.getContext().getResources().getColor(R.color.ux_color_shadow_solid_line));
            mBottomBarRealRootLayout.addView(solidLine);
            solidLine.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            solidLine.getLayoutParams().height = dip2px_fromDimens(mSolidLineHeight);

            mBottomBarRealRootLayout.addView(mRootLayout);
        }

        if (!mInterval) {
            if (mOrientation == HORIZONTAL) {
                mRootLayout.setPadding(dip2px_fromDimens(mSidesInterval), 0, dip2px_fromDimens(mSidesInterval), 0);
            } else {
                mRootLayout.setPadding(0, dip2px_fromDimens(mSidesInterval), 0, dip2px_fromDimens(mSidesInterval));
            }
        }
        if (mOrientation == HORIZONTAL && mBottomBarRealRootLayout != null) {
            return mBottomBarRealRootLayout;
        } else {
            return mRootLayout;
        }
    }

    @Override
    public void setBarVisible(boolean visible) {
        int visibility = 0;
        if (visible) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.INVISIBLE;
        }
        if (mOrientation == HORIZONTAL && mBottomBarRealRootLayout != null)
            mBottomBarRealRootLayout.setVisibility(visibility);
        else
            mRootLayout.setVisibility(visibility);
    }

    private void initDimens() {
        try {
            mSolidLineHeight = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_solidLine_height);//used dp2px
        } catch (Exception e) {
            mSolidLineHeight = dip2px(mSolidLineHeight);
        }
        try {
            mShadowLineHeight = (int) mContext.getResources().getDimension(R.dimen.ux_shadow_height);//used dp2px
        } catch (Exception e) {
            mShadowLineHeight = dip2px(mShadowLineHeight);
        }
        try {
            mSidesInterval = (int) mContext.getResources().getDimension(R.dimen.ux_text_icon_distance_phone);
        } catch (Exception e) {
            mSidesInterval = dip2px(mSidesInterval);
        }
    }
}
