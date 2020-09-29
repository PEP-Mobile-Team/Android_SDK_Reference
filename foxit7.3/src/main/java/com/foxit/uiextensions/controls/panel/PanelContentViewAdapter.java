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
package com.foxit.uiextensions.controls.panel;

import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

public class PanelContentViewAdapter extends PagerAdapter {
    private List<View> mViewPagerList;

    public PanelContentViewAdapter(List<View> viewPagerList) {
        mViewPagerList = viewPagerList;
    }

    @Override
    public int getCount() {
        return mViewPagerList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // -1 if the view does not exist in the group
        if (container.indexOfChild(mViewPagerList.get(position)) == -1) {
            container.addView(mViewPagerList.get(position));
        }

        return mViewPagerList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object o){
        if (mViewPagerList.contains(o)){
            return mViewPagerList.indexOf(o);
        }
        return PagerAdapter.POSITION_NONE;
    }

}
