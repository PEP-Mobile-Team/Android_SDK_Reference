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
package com.foxit.uiextensions.modules.signature;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

class SignatureViewGroup extends ViewGroup {
	private Context				mContext;
	private Scroller			mScroller;
	private int 				mCurIndex;
	private IMoveCallBack		mCallback;
	
	public interface IMoveCallBack {
        void onStop();
        void onStart();
	}
	
	public SignatureViewGroup(Context context) {
		this(context, null);
	}
	
	public SignatureViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	private void init() {
		this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mScroller = new Scroller(mContext);
	}
	
	public void init(int width, int height) {
		snapToScreen(0);
		this.invalidate();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			view.measure(widthMeasureSpec, heightMeasureSpec);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			invalidate();
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			view.layout(l, i * getHeight(), r, getHeight() + i * getHeight());
		}
	}
	
	private void snapToScreen(int index) {
		if (mCurIndex == index) return;
		if (!mScroller.isFinished()) {
			mScroller.forceFinished(true);
		}
		int delta = index * getHeight() - getScrollY();
		mScroller.startScroll(0, getScrollY(), 0, delta, Math.abs(delta));
		invalidate();
		mCurIndex = index;
	}
	
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(0, mScroller.getCurrY());
			postInvalidate();
		}
	}
	
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (mCallback != null && (getScrollY() == 0 || getScrollY() == getHeight())) {
			mCallback.onStop();
			mCallback = null;
		}
	}

	
	public void moveToTop(final IMoveCallBack callback) {
		mCallback= callback;
		callback.onStart();
		snapToScreen(0);
	}
}
