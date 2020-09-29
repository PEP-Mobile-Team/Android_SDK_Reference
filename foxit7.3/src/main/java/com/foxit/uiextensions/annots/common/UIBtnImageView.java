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
package com.foxit.uiextensions.annots.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;

public class UIBtnImageView extends AppCompatImageView implements GestureDetector.OnGestureListener {
    private View mParentLayout;
    private OnClickListener mClickListener;
    private OnLongClickListener mLongClickListener;
    private GestureDetector mGestureDetector;

    private boolean mIsChecked;
    private Context mContext;
    protected Paint mPaint = new Paint();

    public UIBtnImageView(Context context) {
        this(context, null, 0);
    }

    public UIBtnImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UIBtnImageView(Context context, View rootLayout) {
        this(context, null, 0);
        mParentLayout = rootLayout;
    }

    public UIBtnImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mGestureDetector = new GestureDetector(context, this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setColorFilter(mContext.getResources().getColor(R.color.ux_color_btn_item_pressed));
                mGestureDetector.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.setColorFilter(null);
                break;
            default:
        }
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (isEnabled()) {
            setColorFilter(Color.TRANSPARENT);
        } else {
            setColorFilter(mContext.getResources().getColor( R.color.ux_color_btn_item_pressed));
        }
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mIsChecked) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mContext.getResources().getColor(R.color.ux_color_menu_inverse));
            canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()),
                    AppDisplay.getInstance(mContext).dp2px(2),  AppDisplay.getInstance(mContext).dp2px(2), mPaint);
        }
        super.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        mClickListener = l;
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        super.setOnLongClickListener(l);
        mLongClickListener = l;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mClickListener != null) {
            if (mParentLayout != null) {
                mClickListener.onClick(mParentLayout);
            } else {
                mClickListener.onClick(UIBtnImageView.this);
            }
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mLongClickListener != null) {
            mLongClickListener.onLongClick(this);
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}