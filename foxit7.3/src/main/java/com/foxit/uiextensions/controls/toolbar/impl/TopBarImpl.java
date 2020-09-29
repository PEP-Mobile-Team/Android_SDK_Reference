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
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;

public class TopBarImpl extends BaseBarImpl {
    private LinearLayout mTopBarRealRootLayout = null;
    protected int mWide = 56;
    protected int mSolidLineHeight = 1;
    protected int mShadowLineHeight = 3;
    protected int mLeftSideInterval = 16;
    protected int mRightSideInterval = 9;

    public TopBarImpl(Context context) {
        super(context, HORIZONTAL);

        initDimens();
        refreshLayout();
        initOrientation(HORIZONTAL, ViewGroup.LayoutParams.MATCH_PARENT, mWide);
    }

    @Override
    public View getContentView() {
        if (mOrientation == HORIZONTAL && mTopBarRealRootLayout == null) {
            mTopBarRealRootLayout = new LinearLayout(mRootLayout.getContext());
            mTopBarRealRootLayout.setOrientation(LinearLayout.VERTICAL);

            mTopBarRealRootLayout.addView(mRootLayout);

            View solidLine = new View(mRootLayout.getContext());
            solidLine.setBackgroundColor(mRootLayout.getContext().getResources().getColor(R.color.ux_color_shadow_solid_line));
            mTopBarRealRootLayout.addView(solidLine);
            solidLine.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            solidLine.getLayoutParams().height = dip2px_fromDimens(mSolidLineHeight);

            View shadowLine = new View(mRootLayout.getContext());
            shadowLine.setBackgroundResource(R.drawable.toolbar_shadow_top);
            mTopBarRealRootLayout.addView(shadowLine);
            shadowLine.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            shadowLine.getLayoutParams().height = dip2px_fromDimens(mShadowLineHeight);
        }
        if (!mInterval) {//super getContentView
            if (mOrientation == HORIZONTAL) {
                mRootLayout.setPadding(dip2px_fromDimens(mLeftSideInterval), 0, dip2px_fromDimens(mRightSideInterval), 0);
            } else {
                mRootLayout.setPadding(0, dip2px_fromDimens(mLeftSideInterval), 0, dip2px_fromDimens(mRightSideInterval));
            }
        }
        if (mOrientation == HORIZONTAL && mTopBarRealRootLayout != null) {
            return mTopBarRealRootLayout;
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
        if (mOrientation == HORIZONTAL && mTopBarRealRootLayout != null)
            mTopBarRealRootLayout.setVisibility(visibility);
        else
            mRootLayout.setVisibility(visibility);
    }

    @Override
    public boolean addView(IBaseItem item, TB_Position position) {
        boolean isAddSuccess = super.addView(item, position);
        if (!mInterval) {
            if (mOrientation == HORIZONTAL) {
                mRootLayout.setPadding(dip2px_fromDimens(mLeftSideInterval), 0, dip2px_fromDimens(mRightSideInterval), 0);
            } else {
                mRootLayout.setPadding(0, dip2px_fromDimens(mLeftSideInterval), 0, dip2px_fromDimens(mRightSideInterval));
            }
        }
        return isAddSuccess;
    }

    private void initDimens() {
        try {
            if (mIsPad) {
                mWide = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_pad);//used dp2px
            } else {
                mWide = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_phone);//used dp2px
            }
        } catch (Exception e) {
            mWide = dip2px(mWide);
        }
        try {
            mSolidLineHeight = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_solidLine_height);//used dp2px
        } catch (Exception e) {
            mSolidLineHeight = dip2px(mSolidLineHeight);
        }
        try {
            mShadowLineHeight = mContext.getResources().getDimensionPixelSize(R.dimen.ux_shadow_height);//used dp2px
        } catch (Exception e) {
            mShadowLineHeight = dip2px(mShadowLineHeight);
        }
        try {
            if (mIsPad) {
                mLeftSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            } else {
                mLeftSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            }
        } catch (Exception e) {
            mLeftSideInterval = dip2px(mLeftSideInterval);
        }
        try {
            if (mIsPad) {
                mRightSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
            } else {
                mRightSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
            }
        } catch (Exception e) {
            mRightSideInterval = dip2px(mRightSideInterval);
        }
    }
}
