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
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.CircleItem;

public class CircleItemImpl extends BaseItemImpl implements CircleItem {
    protected RelativeLayout mCircleLayout;
    protected RelativeLayout.LayoutParams mBackgroundLayoutParams;
    protected RelativeLayout.LayoutParams mContentLayoutParams;
    protected LinearLayout.LayoutParams mCircleLayoutParams;
    protected ImageView mBackgroundImageView;
    protected ImageView mContentImageView;

    public CircleItemImpl(Context context) {
        super(context);
        mCircleLayout = new RelativeLayout(context);
        mBackgroundLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mBackgroundLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mContentLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContentLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mBackgroundImageView = new ImageView(context);
        mBackgroundImageView.setImageResource(R.drawable.tb_circle_background);
        mContentImageView = new ImageView(context);
        mCircleLayout.addView(mBackgroundImageView, mBackgroundLayoutParams);
        mCircleLayout.addView(mContentImageView, mContentLayoutParams);

        mCircleLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mCircleLayoutParams.gravity = Gravity.CENTER;
        mRootLayout.addView(mCircleLayout, mCircleLayoutParams);

        mImage.setVisibility(View.GONE);
    }

    @Override
    public View getContentView() {
        return mRootLayout;
    }

    @Override
    public boolean setImageResource(int res) {
        mContentImageView.setImageResource(res);
        return true;
    }

    @Override
    public boolean setImageDrawable(@Nullable Drawable drawable) {
        mContentImageView.setImageDrawable(drawable);
        return true;
    }

    @Override
    public void setContentView(View view) {
        if (view != null && mCircleLayout != null) {
            mCircleLayout.removeAllViews();
            mCircleLayout.addView(mBackgroundImageView);
            mCircleLayout.addView(view);
        }

    }

    @Override
    public void setBackgroundResource(int res) {
        mRootLayout.setBackgroundResource(res);
    }

    @Override
    public void setRelation(int relation) {
        mRelation = relation;
        mRootLayout.removeAllViews();
        setTextImgRelation(relation);
    }

    private void setTextImgRelation(int relation) {
        if (relation == RELATION_LEFT || relation == RELATION_RIGNT) {
            mRootLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mRootLayout.setOrientation(LinearLayout.VERTICAL);
        }
        if (relation == RELATION_LEFT || relation == RELATION_TOP) {
            if (mTextView != null) {
                if (mTextParams != null) {
                    mRootLayout.addView(mTextView, mTextParams);
                } else {
                    mRootLayout.addView(mTextView);
                }
            }
            if (mCircleLayout != null) {
                mRootLayout.addView(mCircleLayout);
            }
        } else {
            if (mCircleLayout != null) {
                mRootLayout.addView(mCircleLayout);
            }
            if (mTextView != null) {
                if (mTextParams != null) {
                    mRootLayout.addView(mTextView, mTextParams);
                } else {
                    mRootLayout.addView(mTextView);
                }
            }
        }
    }

    @Override
    public void setEnable(boolean enable) {
        super.setEnable(enable);
        mCircleLayout.setEnabled(enable);
        mBackgroundImageView.setEnabled(enable);
        mContentImageView.setEnabled(enable);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        mCircleLayout.setSelected(selected);
        mContentImageView.setSelected(selected);
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        mRootLayout.setOnClickListener(l);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        mRootLayout.setOnLongClickListener(l);
    }

    @Override
    public void setInterval(int interval) {
        super.setInterval(interval);
    }

    @Override
    public void setDisplayStyle(ItemType type) {
        if (ItemType.Item_Image.equals(type)) {
            if (mCircleLayout != null) {
                mCircleLayout.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        } else if (ItemType.Item_Text.equals(type)) {
            if (mCircleLayout != null) {
                mCircleLayout.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else if (ItemType.Item_Text_Image.equals(type)) {
            if (mCircleLayout != null) {
                mCircleLayout.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mCircleLayout != null) {
                mCircleLayout.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        }
    }

    public void setCircleRes(int res) {
        mBackgroundImageView.setImageResource(res);
    }
}
