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
package com.foxit.uiextensions.annots.form;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppDisplay;

public class FormNavigationModule implements Module {
    private Context mContext;
    private ViewGroup mParent;

    private RelativeLayout mFormNavigationLayout;
    private ImageView mPreView;
    private ImageView mNextView;
    private TextView mClearView;
    private TextView mFinishView;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public FormNavigationModule(Context context, ViewGroup parent, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        this.mContext = context;
        this.mParent = parent;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_FORM_NAVIGATION;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        if (AppDisplay.getInstance(mContext).isPad())
            mFormNavigationLayout = (RelativeLayout) View.inflate(mContext, R.layout.rv_form_navigation_pad, null);
        else
            mFormNavigationLayout = (RelativeLayout) View.inflate(mContext, R.layout.rv_form_navigation_phone, null);
        mPreView = (ImageView) mFormNavigationLayout.findViewById(R.id.rv_form_pre);
        mNextView = (ImageView) mFormNavigationLayout.findViewById(R.id.rv_form_next);
        mClearView = (TextView) mFormNavigationLayout.findViewById(R.id.rv_form_clear);
        mFinishView = (TextView) mFormNavigationLayout.findViewById(R.id.rv_form_finish);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mFormNavigationLayout.setPadding(0, 0, 0, 0);
        mFormNavigationLayout.setVisibility(View.INVISIBLE);
        mFormNavigationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                return;
            }
        });
        mParent.addView(mFormNavigationLayout, lp);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mParent.removeView(mFormNavigationLayout);
        return true;
    }

    public RelativeLayout getLayout() {
        return mFormNavigationLayout;
    }

    public void show() {
        if (getLayout().getVisibility() != View.VISIBLE){
            getLayout().startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_btot_show));
            getLayout().setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (getLayout().getVisibility() != View.INVISIBLE){
            getLayout().startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_btot_hide));
            getLayout().setVisibility(View.INVISIBLE);
        }
    }

    public Rect getPadding() {
        Rect padding = new Rect();
        padding.left = getLayout().getPaddingLeft();
        padding.top = getLayout().getPaddingTop();
        padding.right = getLayout().getPaddingRight();
        padding.bottom = getLayout().getPaddingBottom();
        return padding;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        getLayout().setPadding(left, top, right, bottom);
    }

    public ImageView getPreView() {
        return mPreView;
    }

    public ImageView getNextView() {
        return mNextView;
    }

    public TextView getClearView() {
        return mClearView;
    }

    public TextView getFinishView() {
        return mFinishView;
    }

    public void setClearEnable(boolean enable) {
        if (enable) {
            mClearView.setEnabled(true);
        }
        else {
            mClearView.setEnabled(false);
        }
    }

}
