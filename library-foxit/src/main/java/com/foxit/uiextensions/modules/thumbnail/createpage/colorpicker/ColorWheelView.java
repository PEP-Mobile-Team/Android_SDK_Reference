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
package com.foxit.uiextensions.modules.thumbnail.createpage.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.foxit.uiextensions.utils.AppDisplay;

import androidx.annotation.Nullable;

public class ColorWheelView extends FrameLayout implements ColorObserver {

    private float radius;
    private float centerX;
    private float centerY;
    private float selectorRadiusPx;

    private PointF currentPoint = new PointF();
    private int currentColor = Color.MAGENTA;

    private ColorWheelSelector selector;
    private ObservableColor observableColor = new ObservableColor(0);

    public void observeColor(ObservableColor observableColor) {
        this.observableColor = observableColor;
        observableColor.addObserver(this);
    }

    public ColorWheelView(Context context) {
        this(context, null);
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        selectorRadiusPx = AppDisplay.getInstance(context).dp2px(6);

        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ColorWheelPalette palette = new ColorWheelPalette(context);
        int padding = (int) selectorRadiusPx;
        palette.setPadding(padding, padding, padding, padding);
        addView(palette, layoutParams);

        LayoutParams selectorlayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        selector = new ColorWheelSelector(context);
        selector.setSelectorRadiusPx(selectorRadiusPx);
        addView(selector, selectorlayoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;
        width = height = Math.min(maxWidth, maxHeight);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int netWidth = w - getPaddingLeft() - getPaddingRight();
        int netHeight = h - getPaddingTop() - getPaddingBottom();
        radius = Math.min(netWidth, netHeight) * 0.5f - selectorRadiusPx;
        if (radius < 0) return;
        centerX = netWidth * 0.5f;
        centerY = netHeight * 0.5f;
        setColor(currentColor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                update(event);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void update(MotionEvent event) {
        updateSelector(event.getX(), event.getY());

        float x = event.getX() - centerX;
        float y = event.getY() - centerY;
        double r = Math.sqrt(x * x + y * y);
        float hue = (float) (Math.atan2(y, -x) / Math.PI * 180f) + 180;
        float sat = Math.max(0f, Math.min(1f, (float) (r / radius)));
        observableColor.updateHueSat(hue, sat, this);
    }

    public void setColor(int color) {
        currentColor = color;

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        float r = hsv[1] * radius;
        float radian = (float) (hsv[0] / 180f * Math.PI);
        updateSelector((float) (r * Math.cos(radian) + centerX), (float) (-r * Math.sin(radian) + centerY));
        observableColor.updateColor(color, this);
    }

    private void updateSelector(float eventX, float eventY) {
        float x = eventX - centerX;
        float y = eventY - centerY;
        double r = Math.sqrt(x * x + y * y);
        if (r > radius) {
            x *= radius / r;
            y *= radius / r;
        }
        currentPoint.x = x + centerX;
        currentPoint.y = y + centerY;
        selector.setCurrentPoint(currentPoint);
    }

    @Override
    public void updateColor(ObservableColor observableColor) {
    }

}
