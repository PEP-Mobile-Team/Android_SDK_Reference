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
package com.foxit.uiextensions.home.local;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;

class LocalView extends RelativeLayout {
    private RelativeLayout mTopLayout;
    private RelativeLayout mBottomLayout;
    private View mTopLayoutDivider;
    private View mFileView;

    public LocalView(Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        initTopLayout();
        initBottomLayout();
        mTopLayoutDivider = new View(getContext());
        mTopLayoutDivider.setBackgroundColor(getResources().getColor(R.color.ux_color_seperator_gray));
        LayoutParams dividerParams = new LayoutParams(LayoutParams.MATCH_PARENT, AppDisplay.getInstance(context).dp2px(1));
        dividerParams.addRule(ALIGN_PARENT_BOTTOM);
        mTopLayoutDivider.setLayoutParams(dividerParams);
    }

    private void initTopLayout() {
        mTopLayout = new RelativeLayout(getContext());
        mTopLayout.setId(R.id.fb_local_view_top);
        mTopLayout.setBackgroundColor(getResources().getColor(R.color.ux_color_white));
        LayoutParams params = null;
        if (AppDisplay.getInstance(getContext()).isPad())
            params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.ux_list_item_height_1l_pad));
        else
            params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.ux_list_item_height_1l_phone));
        addView(mTopLayout, params);
        mTopLayout.setGravity(CENTER_VERTICAL);
    }

    private void initBottomLayout() {
        mBottomLayout = new RelativeLayout(getContext());
        mBottomLayout.setId(R.id.fb_local_view_bottom);
        mBottomLayout.setVisibility(VISIBLE);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(ALIGN_PARENT_BOTTOM);
        addView(mBottomLayout, params);
        mBottomLayout.setGravity(CENTER_VERTICAL);
    }

    void setTopLayoutVisible(boolean visible) {
        mTopLayout.setVisibility(visible ? VISIBLE : GONE);
    }

    void addPathView(View view) {
        LayoutParams params;

        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        params.addRule(ALIGN_PARENT_LEFT);
        params.addRule(CENTER_VERTICAL);
        mTopLayout.addView(view, params);
    }

    void removeAllTopView() {
        mTopLayout.removeAllViews();
        mTopLayout.addView(mTopLayoutDivider);
    }

    void setBottomLayoutVisible(boolean visible) {
        mBottomLayout.setVisibility(visible ? VISIBLE : GONE);
    }

    void addFileView(View view) {
        if (mFileView != null && mFileView.getParent() != null) {
            removeView(mFileView);
        }

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.addRule(BELOW, mTopLayout.getId());
        params.addRule(ABOVE, mBottomLayout.getId());
        addView(view, params);
        mFileView = view;
    }
}