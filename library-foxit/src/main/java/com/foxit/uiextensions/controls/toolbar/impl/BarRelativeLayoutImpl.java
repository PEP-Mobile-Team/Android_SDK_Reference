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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class BarRelativeLayoutImpl extends RelativeLayout {
    private BaseBarImpl mBar;
    private boolean mInterceptTouch = true;

    public BarRelativeLayoutImpl(Context context, BaseBarImpl bar) {
        super(context);
        mBar = bar;
    }

    public BarRelativeLayoutImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarRelativeLayoutImpl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mBar.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mBar.layout(l, t, r, b);
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mInterceptTouch) {
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    public void setInterceptTouch(boolean isInterceptTouch) {
        mInterceptTouch = isInterceptTouch;
    }
}
