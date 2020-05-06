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
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.annots.common.UIBtnImageView;

import androidx.annotation.Nullable;

public class AnnotItemImpl extends BaseItemImpl {
    protected RelativeLayout mAnnotLayout;
    protected RelativeLayout.LayoutParams mBackgroundLayoutParams;
    protected RelativeLayout.LayoutParams mContentLayoutParams;
    protected LinearLayout.LayoutParams mAnnotLayoutParams;
    protected UIBtnImageView mBackgroundImageView;
    protected UIBtnImageView mContentImageView;

    public AnnotItemImpl(Context context) {
        super(context);
        mAnnotLayout = new RelativeLayout(context);
        mBackgroundLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mBackgroundLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mContentLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContentLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mBackgroundImageView = new UIBtnImageView(context);
        mContentImageView = new UIBtnImageView(context);
        mAnnotLayout.addView(mBackgroundImageView, mBackgroundLayoutParams);
        mAnnotLayout.addView(mContentImageView, mContentLayoutParams);

        mAnnotLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mAnnotLayoutParams.gravity = Gravity.CENTER;
        mRootLayout.addView(mAnnotLayout, mAnnotLayoutParams);

        mImage.setVisibility(View.GONE);
    }

    @Override
    public View getContentView() {
        return mRootLayout;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mBackgroundImageView != null) {
            mBackgroundImageView.setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return mBackgroundImageView.isChecked();
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
        if (view != null && mAnnotLayout != null) {
            mAnnotLayout.removeAllViews();
            mAnnotLayout.addView(mBackgroundImageView);
            mAnnotLayout.addView(view);
        }
    }

    @Override
    public void setBackgroundResource(int res) {
        mBackgroundImageView.setBackgroundResource(res);
    }

    @Override
    public void setRelation(int relation) {
        mRelation = relation;
        mRootLayout.removeAllViews();
        setTextImgRelation(relation);
    }

    @Override
    public void setImagePadding(int l, int t, int r, int b) {
        if (mBackgroundImageView != null) {
            mBackgroundImageView.setPadding(l, t, r, b);
        }
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
            if (mAnnotLayout != null) {
                mRootLayout.addView(mAnnotLayout);
            }
        } else {
            if (mAnnotLayout != null) {
                mRootLayout.addView(mAnnotLayout);
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
        mAnnotLayout.setEnabled(enable);
        mBackgroundImageView.setEnabled(enable);
        mContentImageView.setEnabled(enable);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        mAnnotLayout.setSelected(selected);
        mContentImageView.setSelected(selected);
    }

    @Override
    public void setInterval(int interval) {
        super.setInterval(interval);
    }

    @Override
    public void setDisplayStyle(ItemType type) {
        if (ItemType.Item_Image.equals(type)) {
            if (mAnnotLayout != null) {
                mAnnotLayout.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        } else if (ItemType.Item_Text.equals(type)) {
            if (mAnnotLayout != null) {
                mAnnotLayout.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else if (ItemType.Item_Text_Image.equals(type)) {
            if (mAnnotLayout != null) {
                mAnnotLayout.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mAnnotLayout != null) {
                mAnnotLayout.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        mClickListener = l;
        mRootLayout.setOnClickListener(mClickListenerImp);
        if (mContentImageView != null) {
            mContentImageView.setOnClickListener(mClickListenerImp);
        }
        mRootLayout.setOnClickListener(l);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        mLongClickListener = l;
        mRootLayout.setOnLongClickListener(l);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mItemClickListener = l;
        mRootLayout.setOnClickListener(mClickListenerImp);
        if (mContentImageView != null) {
            mContentImageView.setOnClickListener(mClickListenerImp);
        }
    }

    @Override
    public void setOnLongPressListener(onLongPressListener l) {
        mItemLongClickListener = l;
        mRootLayout.setOnLongClickListener(mLongClickListenerImp);
        if (mContentImageView != null) {
            mContentImageView.setOnLongClickListener(mLongClickListenerImp);
        }
    }

}
