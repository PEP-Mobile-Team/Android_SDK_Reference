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
package com.foxit.uiextensions.controls.panel.impl;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.panel.PanelContentViewAdapter;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.modules.ReadingBookmarkModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.util.ArrayList;
import java.util.List;

public class PanelHostImpl implements PanelHost {
    private Context mContext;
    private ViewGroup mRootView;
    private LinearLayout mTopLayout;
    private LinearLayout mTabLayout;
    private TextView mNoPanelView;
    private FrameLayout mNoPanelTopbarView;
    private ArrayList<PanelSpec> mSpecs;
    private PanelSpec mCurSpec;

    private ViewPager mContentViewPager;
    private List<View> mViewPagerList;
    private ICloseDefaultPanelCallback mColseDefaultPanelListener;
    private PanelContentViewAdapter mViewPagerAdapter;
    private AppDisplay mDisplay;

    public PanelHostImpl(Context context, ICloseDefaultPanelCallback closeDefaultPanelListener) {
        mContext = context;
        mColseDefaultPanelListener = closeDefaultPanelListener;
        mDisplay = new AppDisplay(mContext);
        mRootView = (ViewGroup) View.inflate(mContext, R.layout.root_panel, null);
        mTopLayout = (LinearLayout) mRootView.findViewById(R.id.panel_topbar_layout);
        mTabLayout = (LinearLayout) mRootView.findViewById(R.id.panel_tabbar_layout);
        mNoPanelView = (TextView) mRootView.findViewById(R.id.panel_content_noinfo);
        mContentViewPager = (ViewPager) mRootView.findViewById(R.id.panel_content_viewpager);

        mSpecs = new ArrayList<PanelSpec>();
        mViewPagerList = new ArrayList<View>();
        mViewPagerAdapter = new PanelContentViewAdapter(mViewPagerList);
        mContentViewPager.setAdapter(mViewPagerAdapter);
        mContentViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mCurSpec != null && mCurSpec.getPanelType() != mSpecs.get(position).getPanelType()) {
                    setCurrentSpec(mSpecs.get(position).getPanelType());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mTopLayout.setBackgroundResource(R.color.ux_text_color_subhead_colour);
        if (mDisplay.isPad()) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTopLayout.getLayoutParams();
            params.height = mDisplay.dp2px(64.0f);
            mTopLayout.setLayoutParams(params);
        }

        mTabLayout.setBackgroundResource(R.color.ux_text_color_subhead_colour);


        if (mDisplay.isPad()) {
            mTopLayout.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad);
        }
    }

    @Override
    public ViewGroup getContentView() {
        return mRootView;
    }

    @Override
    public PanelSpec getSpec(PanelSpec.PanelType panelType) {
        for (PanelSpec spec : mSpecs) {
            if (spec.getPanelType() == panelType) {
                return spec;
            }
        }
        return null;
    }

    @Override
    public void addSpec(PanelSpec spec) {
        if (getSpec(spec.getPanelType()) != null)
            return;
        int index = -1;
        for (int i = 0; i < mSpecs.size(); i++) {
            if (mSpecs.get(i).getPanelType().getTag() > spec.getPanelType().getTag()) {
                index = i;
                break;
            }
        }

        if (mNoPanelView.getVisibility() == View.VISIBLE) {
            mNoPanelView.setVisibility(View.GONE);
            mContentViewPager.setVisibility(View.VISIBLE);
        }

        if (index == -1) {
            mSpecs.add(spec);
            mViewPagerList.add(spec.getContentView());
        } else {
            mSpecs.add(index, spec);
            mViewPagerList.add(index, spec.getContentView());
        }
        mViewPagerAdapter.notifyDataSetChanged();

        int tabIndex = mSpecs.indexOf(spec);
        addTab(tabIndex, spec);
        if (mCurSpec == null || mSpecs.size() == 1) {
            setFocuses(0);
        } else {

            if (!(mCurSpec instanceof ReadingBookmarkModule)) {
                setFocuses(mSpecs.indexOf(mCurSpec));
            }
        }
    }

    @Override
    public void removeSpec(PanelSpec spec) {
        int index = mSpecs.indexOf(spec);
        if (index < 0) return;
        mSpecs.remove(index);
        mViewPagerList.remove(index);
        mViewPagerAdapter.notifyDataSetChanged();

        removeTab(spec);
        if (mSpecs.size() > index) {
            setFocuses(index);
        } else {
            setFocuses(mSpecs.size() - 1);
        }
    }

    @Override
    public void setCurrentSpec(PanelSpec.PanelType panelType) {
        if (mCurSpec != null) {
            if (mCurSpec.getPanelType() == panelType) {
                mCurSpec.onActivated();
                return;
            }
            mCurSpec.onDeactivated();
        }
        for (int i = 0; i < mSpecs.size(); i++) {
            if (mSpecs.get(i).getPanelType() == panelType) {
                setFocuses(i);
                mSpecs.get(i).onActivated();
            }
        }
    }

    @Override
    public PanelSpec getCurrentSpec() {
        return mCurSpec;
    }

    private void addTab(int index, final PanelSpec spec) {
        // icon view
        ImageView iconView = new ImageView(mContext);
        iconView.setId(R.id.rd_panel_tab_item);
        iconView.setImageResource(spec.getIcon());

        RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        iconLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        iconLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        // selected bright line
        ImageView focusView = new ImageView(mContext);
        focusView.setBackgroundColor(Color.WHITE);
        focusView.setImageResource(R.drawable.toolbar_shadow_top);
        focusView.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams focusLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, mDisplay.dp2px(4.0f));
        focusLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        int topMargin = 0;
        focusLayoutParams.setMargins(0, mDisplay.dp2px(topMargin), 0, 0);

        RelativeLayout tabItemView = new RelativeLayout(mContext);
        tabItemView.addView(iconView, iconLayoutParams);
        tabItemView.addView(focusView, focusLayoutParams);
        tabItemView.setTag(spec.getPanelType());

        tabItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentSpec(spec.getPanelType());
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        mTabLayout.addView(tabItemView, index, lp);
    }

    void removeTab(PanelSpec spec) {
        for (int i = 0, count = mTabLayout.getChildCount(); i < count; i++) {
            if (mTabLayout.getChildAt(i).getTag() == spec.getPanelType()) {
                mTabLayout.removeViewAt(i);
                break;
            }
        }
    }

    private void setFocuses(int index) {
        if (index < 0 || index > mSpecs.size() - 1) {
            index = 0;
        }
        if (mSpecs.size() == 0) {
            mNoPanelView.setVisibility(View.VISIBLE);
            mContentViewPager.setVisibility(View.GONE);
            mTabLayout.setVisibility(View.GONE);
            mTopLayout.removeAllViews();
            mTopLayout.addView(getNoPanelTopbar());
            return;
        }

        mCurSpec = mSpecs.get(index);
        mTopLayout.removeAllViews();
        mTopLayout.addView(mCurSpec.getTopToolbar());
        mTabLayout.setVisibility(View.VISIBLE);

        mContentViewPager.setCurrentItem(index);

        int iconCount = mSpecs.size();
        for (int i = 0; i < iconCount; i++) {
            RelativeLayout iconBox = (RelativeLayout) mTabLayout.getChildAt(i);
            if (i == index) {
                ((ImageView) iconBox.getChildAt(0)).setImageState(new int[]{android.R.attr.state_pressed}, true);
                iconBox.getChildAt(1).setVisibility(View.VISIBLE);
            } else {
                ((ImageView) iconBox.getChildAt(0)).setImageState(new int[]{}, true);
                iconBox.getChildAt(1).setVisibility(View.INVISIBLE);
            }
        }
    }


    private View getNoPanelTopbar() {
        if (mNoPanelTopbarView == null) {
            mNoPanelTopbarView = (FrameLayout) LayoutInflater.from(mContext).inflate(R.layout.panel_no_panel_topbar, null, false);

            ImageView noPanelClose = (ImageView) mNoPanelTopbarView.findViewById(R.id.panel_no_panel_topbar_close);

            // set top side offset
            if (mDisplay.isPad()) {
                noPanelClose.setVisibility(View.GONE);

                FrameLayout.LayoutParams rl_topLayoutParams = (FrameLayout.LayoutParams) noPanelClose.getLayoutParams();
                rl_topLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
                noPanelClose.setLayoutParams(rl_topLayoutParams);

                FrameLayout.LayoutParams topCloseLayoutParams = (FrameLayout.LayoutParams) noPanelClose.getLayoutParams();
                topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                noPanelClose.setLayoutParams(topCloseLayoutParams);
            } else {

                if (mColseDefaultPanelListener != null) {
                    noPanelClose.setVisibility(View.VISIBLE);
                    noPanelClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mColseDefaultPanelListener.closeDefaultPanel(v);
                        }
                    });
                } else {
                    noPanelClose.setVisibility(View.GONE);
                }
            }
        }
        return mNoPanelTopbarView;
    }

}
