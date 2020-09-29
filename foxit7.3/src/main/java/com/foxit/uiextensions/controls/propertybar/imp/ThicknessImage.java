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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.foxit.uiextensions.R;

public class ThicknessImage extends View {
    private float MAX_THICKNESS = 60.0f;
    private float mMax_Thickness_px;// max borderThickness in px
    private float mBorderThickness;// in px
    private int mColor;
    private Paint mPaint;

    public ThicknessImage(Context context) {
        this(context, null);
    }

    public ThicknessImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThicknessImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mMax_Thickness_px = MAX_THICKNESS * (context.getResources().getDisplayMetrics().densityDpi / 160.0f);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThicknessImage);
        mColor = typedArray.getColor(R.styleable.ThicknessImage_borderColor, Color.RED);
        float borderThickness = typedArray.getDimension(R.styleable.ThicknessImage_borderThickness, 1.0f);
        if (borderThickness * 2 > mMax_Thickness_px) {
            mBorderThickness = mMax_Thickness_px / 2;
        } else if (borderThickness < 1.0f) {
            mBorderThickness = 1.0f;
        } else {
            mBorderThickness = borderThickness;
        }
        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mColor);
        canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f, mBorderThickness, mPaint);
        super.onDraw(canvas);
    }

    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }

    public void setBorderThickness(float borderThickness) {
        if (borderThickness * 2 > mMax_Thickness_px) {
            this.mBorderThickness = mMax_Thickness_px / 2;
        } else if (borderThickness < 1.0f) {
            this.mBorderThickness = 1.0f;
        } else {
            this.mBorderThickness = borderThickness;
        }
        invalidate();
    }
}
