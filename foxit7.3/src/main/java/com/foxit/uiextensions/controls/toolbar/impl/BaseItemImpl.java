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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.common.UIBtnImageView;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.utils.AppDisplay;

public class BaseItemImpl implements IBaseItem {

    protected ItemType mItemType;
    private int mTag;
    protected int mRelation;

    protected LinearLayout mRootLayout;//content view
    protected UIBtnImageView mImage;
    protected TextView mTextView;//title

    protected LinearLayout.LayoutParams mTextParams;
    protected LinearLayout.LayoutParams mImgParams;//image or customView

    protected int mSelectTextColor = Color.RED;
    protected int mNoSelectTextColor = Color.BLACK;
    protected boolean mUseTextColorRes = false;
    protected int mDefinedTextSize = 12;

    protected View.OnClickListener mClickListener;
    protected View.OnLongClickListener mLongClickListener;
    protected OnItemClickListener mItemClickListener;
    protected OnItemLongPressListener mItemLongClickListener;

    public BaseItemImpl(Context context) {
        this(context, null, 0, null, RELATION_LEFT, ItemType.Item_Text_Image);
    }

    public BaseItemImpl(Context context, String text) {
        this(context, text, 0, null, RELATION_LEFT, ItemType.Item_Text);
    }

    public BaseItemImpl(Context context, int imgRes) {
        this(context, null, imgRes, null, RELATION_LEFT, ItemType.Item_Image);
    }

    public BaseItemImpl(Context context, View customView) {
        this(context, null, 0, customView, RELATION_LEFT, ItemType.Item_custom);
    }

    protected BaseItemImpl(Context context, String text, int imgRes, int relation) {
        this(context, text, imgRes, null, relation, ItemType.Item_Text_Image);
    }

    private BaseItemImpl(Context context, String text, int imgRes, View customView, int relation, ItemType type) {
        initDimens(context);
        mRelation = relation;
        mItemType = type;
        mRootLayout = new LinearLayout(context) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                onItemLayout(l, t, r, b);
            }
        };
        mRootLayout.setGravity(Gravity.CENTER);
        if (ItemType.Item_Image.equals(type) || ItemType.Item_Text_Image.equals(type)) {
            mImage = new UIBtnImageView(context, mRootLayout);
            mImgParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mImgParams.gravity = Gravity.CENTER;
            if (imgRes != 0) {
                mImage.setImageResource(imgRes);
            }
        }

        if (ItemType.Item_Text.equals(type) || ItemType.Item_Text_Image.equals(type)) {
            mTextView = new TextView(context);
            mTextParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mTextParams.gravity = Gravity.CENTER;
            if (text != null) {
                mTextView.setText(text);
            }
        }

        if (customView != null) {
            mRootLayout.setOrientation(LinearLayout.VERTICAL);
            mRootLayout.addView(customView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            setTextImgRelation(relation);
        }
        if (mTextView != null) {//defined textSize and color
            setTextSize(mDefinedTextSize);
            setTextColorResource(R.color.tb_item_text_color_selector);
        }

    }

    @Override
    public View getContentView() {
        return mRootLayout;
    }

    @Override
    public void setText(CharSequence text) {
        if (mTextView != null) {
            mTextView.setText(text);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
        }
    }

    @Override
    public void setText(int res) {
        if (mTextView != null) {
            mTextView.setText(res);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
        }
    }

    @Override
    public String getText() {
        if (mTextView != null) {
            if (mTextView.getText() != null) {
                return mTextView.getText().toString();
            }
        }
        return null;
    }

    @Override
    public void setTextColor(int selectedTextColor, int disSelectedTextColor) {
        mUseTextColorRes = false;
        mSelectTextColor = selectedTextColor;
        mNoSelectTextColor = disSelectedTextColor;
        if (mTextView != null) {
            mTextView.setTextColor(mNoSelectTextColor);
        }
    }

    @Override
    public void setTextColor(int color) {
        setTextColor(color, color);
    }

    @Override
    public void setTypeface(Typeface typeface) {
        if (mTextView != null) {
            mTextView.setTypeface(typeface);
        }
    }

    @Override
    public void setTextSize(float size) {
        if (mTextView != null) {
            mTextView.setTextSize(size);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        if (mTextView != null) {
            mTextView.setTextSize(unit, size);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
        }
    }

    @Override
    public void setTextColorResource(int res) {
        Resources resource = mTextView.getContext().getResources();
        ColorStateList csl = resource.getColorStateList(res);
        mTextView.setTextColor(csl);
        mUseTextColorRes = true;
    }

    @Override
    public boolean setImageResource(int res) {
        if (mImage != null) {
            mImage.setImageResource(res);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean setImageDrawable(@Nullable Drawable drawable) {
        if (mImage != null) {
            mImage.setImageDrawable(drawable);
            if (mResetLayoutListener != null) {
                mResetLayoutListener.resetParentLayout(this);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setContentView(View view) {
        if (view != null && mRootLayout != null) {
            mRootLayout.removeAllViews();
            mRootLayout.addView(view);
        }
    }

    @Override
    public void setBackgroundResource(int res) {
        mRootLayout.setBackgroundResource(res);
    }

    @Override
    public void setImagePadding(int l, int t, int r, int b) {
        if (mImage != null) {
            mImage.setPadding(l, t, r, b);
        }
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        if (mTextView != null) {
            mTextView.setFilters(filters);
            mTextView.setText(mTextView.getText());
        }
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt where) {
        if (mTextView != null) {
            mTextView.setLines(1);
            mTextView.setEllipsize(where);
        }
    }

    @Override
    public void setRelation(int relation) {
        if (mRelation == relation) {
            return;
        }
        if (mItemType != ItemType.Item_Text_Image) {
            return;
        }
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
            if (mImage != null) {
                if (mImgParams != null) {
                    mRootLayout.addView(mImage, mImgParams);
                } else {
                    mRootLayout.addView(mImage);
                }
            }
        } else {
            if (mImage != null) {
                if (mImgParams != null) {
                    mRootLayout.addView(mImage, mImgParams);
                } else {
                    mRootLayout.addView(mImage);
                }
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
        mRootLayout.setEnabled(enable);
        if (mTextView != null) {
            mTextView.setEnabled(enable);
        }
        if (mImage != null) {
            mImage.setEnabled(enable);
        }
    }

    @Override
    public void setSelected(boolean selected) {
        mRootLayout.setSelected(selected);
        if (mTextView != null) {
            mTextView.setSelected(selected);
            if (!mUseTextColorRes) {
                if (selected) {
                    mTextView.setTextColor(mSelectTextColor);
                } else {
                    mTextView.setTextColor(mNoSelectTextColor);
                }
            }
        }
        if (mImage != null) {
            mImage.setSelected(selected);
        }
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        mClickListener = l;
        mRootLayout.setOnClickListener(mClickListenerImp);
        if (mImage != null) {
            mImage.setOnClickListener(mClickListenerImp);
        }
        mRootLayout.setOnClickListener(l);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        mLongClickListener = l;
        mRootLayout.setOnLongClickListener(l);
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener l) {
        mItemClickListener = l;
        mRootLayout.setOnClickListener(mClickListenerImp);
        if (mImage != null) {
            mImage.setOnClickListener(mClickListenerImp);
        }
    }

    @Override
    public void setOnItemLongPressListener(OnItemLongPressListener l) {
        mItemLongClickListener = l;
        mRootLayout.setOnLongClickListener(mLongClickListenerImp);
        if (mImage != null) {
            mImage.setOnLongClickListener(mLongClickListenerImp);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (mImage != null) {
            mImage.setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return mImage.isChecked();
    }

    protected View.OnClickListener mClickListenerImp = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mClickListener != null) {
                mClickListener.onClick(v);
            } else if (mItemClickListener != null) {
                mItemClickListener.onClick(BaseItemImpl.this, v);
            }
        }
    };

    protected View.OnLongClickListener mLongClickListenerImp = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mLongClickListener != null) {
                return mLongClickListener.onLongClick(v);
            } else if (mItemLongClickListener != null) {
                return mItemLongClickListener.onLongPress(BaseItemImpl.this, v);
            }
            return false;
        }
    };

    @Override
    public void setTag(int tag) {
        mTag = tag;
    }

    @Override
    public int getTag() {
        return mTag;
    }

    @Override
    public void setId(int id) {
        mRootLayout.setId(id);
    }

    @Override
    public int getId() {
        if (mRootLayout != null) {
            return mRootLayout.getId();
        } else {
            return 0;
        }

    }

    @Override
    public void setInterval(int interval) {
        if (mTextView == null || mTextView.getLayoutParams() == null) {
            return;
        }
        if (!(mTextView.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        ((LinearLayout.LayoutParams) mTextView.getLayoutParams()).setMargins(0, 0, 0, 0);
        if (mRelation == RELATION_LEFT) {
            ((LinearLayout.LayoutParams) mTextView.getLayoutParams()).rightMargin = interval;
        } else if (mRelation == RELATION_RIGNT) {
            ((LinearLayout.LayoutParams) mTextView.getLayoutParams()).leftMargin = interval;
        } else if (mRelation == RELATION_TOP) {
            ((LinearLayout.LayoutParams) mTextView.getLayoutParams()).bottomMargin = interval;
        } else {
            ((LinearLayout.LayoutParams) mTextView.getLayoutParams()).topMargin = interval;
        }
    }

    @Override
    public void setDisplayStyle(ItemType type) {
        if (ItemType.Item_Image.equals(type)) {
            if (mImage != null) {
                mImage.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        } else if (ItemType.Item_Text.equals(type)) {
            if (mImage != null) {
                mImage.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else if (ItemType.Item_Text_Image.equals(type)) {
            if (mImage != null) {
                mImage.setVisibility(View.VISIBLE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mImage != null) {
                mImage.setVisibility(View.GONE);
            }
            if (mTextView != null) {
                mTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemLayout(int l, int t, int r, int b) {

    }

    private void initDimens(Context context) {
        mDefinedTextSize = (int) context.getResources().getDimension(R.dimen.ux_text_height_toolbar);
        mDefinedTextSize = (int) AppDisplay.getInstance(context).px2dp(mDefinedTextSize);
    }

    public void setImageContentDescription(String des) {
        if (mImage != null) {
            mImage.setContentDescription(des);
        }
    }

    private IResetParentLayoutListener mResetLayoutListener;

    @Override
    public void setResetLayoutListener(IResetParentLayoutListener listener) {
        this.mResetLayoutListener = listener;
    }

}

