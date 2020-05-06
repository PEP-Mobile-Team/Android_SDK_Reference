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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;

public class ColorPicker extends View implements GestureDetector.OnGestureListener {
    private Context mContext;
    private Paint mBitmapPaint;
    private Bitmap mBitmapNormal;
    private float mBitmapRadius = 30;
    private PointF mSelectPoint;
    private Bitmap mGradualChangedBitmap;
    private PropertyBar.UpdateViewListener mUpdateViewListener;
    private int mHeight;
    private int mSelfColorHeight;
    private int mWidth;
    private int mSelfColorWidth;
    private int mCurrentColor;
    private GestureDetector detector;
    private boolean mShow;

    private AppDisplay display;
    private ViewGroup mParent;

    private boolean canEdit = true;

    public ColorPicker(Context context, ViewGroup parent) {
        this(context, null, parent);
    }

    public ColorPicker(Context context, AttributeSet attrs, ViewGroup parent) {
        super(context, attrs);
        mContext = context;
        mParent = parent;

        display = AppDisplay.getInstance(context);
        init();
    }

    @SuppressWarnings("deprecation")
    private void init() {
        if (display.isPad()) {
            mSelfColorWidth = dp2px(320) - dp2px((4 + 16) * 2 + 10 + 30);
            mSelfColorHeight = display.dp2px(120.0f);
        } else {
            int tempWidth = mParent.getWidth();
            int tempHeight = mParent.getHeight();

            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                tempWidth = Math.max(tempWidth, tempHeight);
                if (tempWidth == AppDisplay.getInstance(mContext).getRawScreenWidth())
                    tempWidth -= AppDisplay.getInstance(mContext).getRealNavBarHeight();
                mSelfColorHeight = display.dp2px(80.0f);
            } else {
                tempWidth = Math.min(tempWidth, tempHeight);
                mSelfColorHeight = display.dp2px(120.0f);
            }
            mSelfColorWidth = tempWidth - dp2px(16 * 2 + 10 + 30);
        }

        mBitmapPaint = new Paint();
        mBitmapNormal = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pb_colorpicker_point_selected);
        mBitmapRadius = mBitmapNormal.getWidth() / 2f;
        mSelectPoint = new PointF(dp2px(100), dp2px(30));
        detector = new GestureDetector(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = mSelfColorWidth;
        mHeight = mSelfColorHeight;

        setMeasuredDimension(mWidth, mHeight);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(getGradual(), null, new Rect(0, 0, mWidth, mHeight), mBitmapPaint);
        if (mShow) {
            canvas.drawBitmap(mBitmapNormal, mSelectPoint.x - mBitmapRadius, mSelectPoint.y - mBitmapRadius, mBitmapPaint);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!canEdit) return false;
        float x = e.getX();
        float y = e.getY();
        mShow = true;
        proofDisk(x, y);
        mCurrentColor = getSelectColor(mSelectPoint.x, mSelectPoint.y);
        invalidate();
        if (mUpdateViewListener != null) {
            mUpdateViewListener.onUpdate(PropertyBar.PROPERTY_SELF_COLOR, mCurrentColor);
        }
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGradualChangedBitmap != null && mGradualChangedBitmap.isRecycled()) {
            mGradualChangedBitmap.recycle();
        }
        if (mBitmapNormal != null && mBitmapNormal.isRecycled()) {
            mBitmapNormal.recycle();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    private void proofDisk(float x, float y) {
        if (x < 0) {
            mSelectPoint.x = 0;
        } else if (x > mWidth) {
            mSelectPoint.x = mWidth;
        } else {
            mSelectPoint.x = x;
        }
        if (y < 0) {
            mSelectPoint.y = 0;
        } else if (y > mHeight) {
            mSelectPoint.y = mHeight;
        } else {
            mSelectPoint.y = y;
        }
    }

    private int getSelectColor(float x, float y) {
        Bitmap temp = getGradual();
        int intX = (int) x;
        int intY = (int) y;
        if (intX >= temp.getWidth()) {
            intX = temp.getWidth() - 1;
        }
        if (intY >= temp.getHeight()) {
            intY = temp.getHeight() - 1;
        }
        return temp.getPixel(intX, intY);
    }

    private Bitmap getGradual() {
        if (mGradualChangedBitmap == null) {
            Paint paint = new Paint();
            paint.setStrokeWidth(1);
            mGradualChangedBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(mGradualChangedBitmap);
            int bitmapWidth = mGradualChangedBitmap.getWidth();
            mWidth = bitmapWidth;
            int bitmapHeight = mGradualChangedBitmap.getHeight();
            int[] Colors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
            Shader mShader = new LinearGradient(0, 0, bitmapWidth, 0, Colors, null, Shader.TileMode.MIRROR);
            paint.setShader(mShader);
            canvas.drawRect(0, 0, bitmapWidth, bitmapHeight, paint);
        }
        return mGradualChangedBitmap;
    }

    public interface ColorChangedListener {
        void onColorChanged(int color);
    }

    public void setOnUpdateViewListener(PropertyBar.UpdateViewListener listener) {
        mUpdateViewListener = listener;
    }

    public void setColor(int color) {
        mShow = false;
        mCurrentColor = color;
        invalidate();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private int dp2px(int dip) {
        return display.dp2px(dip);
    }

    public void setEditable(boolean canEdit) {
        this.canEdit = canEdit;
    }
}
