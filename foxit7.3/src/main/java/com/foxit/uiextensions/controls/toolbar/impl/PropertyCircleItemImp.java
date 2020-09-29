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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.utils.AppDisplay;

public class PropertyCircleItemImp extends CircleItemImpl implements PropertyCircleItem {
    private PropertyCircle mPropertyCircle;
    private RelativeLayout.LayoutParams mPropertyCircleLayoutParams;

    public PropertyCircleItemImp(Context context) {
        super(context);
        mPropertyCircle = new PropertyCircle(context);
        mPropertyCircleLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPropertyCircleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mCircleLayout.addView(mPropertyCircle, mCircleLayout.getChildCount() - 1, mPropertyCircleLayoutParams);
        mContentImageView.setImageResource(R.drawable.annot_propertycircleitem_selector);
        mBackgroundImageView.setVisibility(View.GONE);
    }

    @Override
    public void setCentreCircleColor(int color) {
        mPropertyCircle.setColor(color);
    }

    @Override
    public boolean setImageResource(int res) {
        return false;
    }


    private class PropertyCircle extends View {
        private float mRadius;
        private int mColor;
        private Paint mPaint;

        public PropertyCircle(Context context) {
            super(context);
            AppDisplay mDisplay = new AppDisplay(context);
            mRadius = mDisplay.dp2px(30);
            mColor = Color.RED;
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(1);
        }

        public void setColor(int color) {
            mColor = color;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mContentImageView.measure(0, 0);
            this.getLayoutParams().width = mContentImageView.getMeasuredWidth();
            this.getLayoutParams().height = mContentImageView.getMeasuredHeight();

        }

        @Override
        protected void onDraw(Canvas canvas) {
            mPaint.setColor(mColor);
            mRadius = (int) (getWidth() / 2f - 0.1f);//fix margin
            canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f, mRadius, mPaint);
            super.onDraw(canvas);
        }
    }
}
