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
package com.foxit.uiextensions.modules.compare;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;

public class CompareResultWindow extends PopupWindow {

    private Context mContext;
    private ViewGroup mParent;
    private AppDisplay display;
    private View mCompareResultView;
    private TextView mTitle;
    private TextView mContent;

    public CompareResultWindow(Context context, ViewGroup parent) {
        this(context, null, parent);
    }

    public CompareResultWindow(Context context, AttributeSet attrs, ViewGroup parent) {
        this(context, null, 0, parent);
    }

    public CompareResultWindow(Context context, AttributeSet attrs, int defStyleAttr, ViewGroup parent) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mParent = parent;

        display = AppDisplay.getInstance(context);
        mCompareResultView = LayoutInflater.from(mContext).inflate(R.layout.compare_result_comment_layout, null, false);

        mTitle = mCompareResultView.findViewById(R.id.compare_result_title);
        mContent = mCompareResultView.findViewById(R.id.compare_result_content);
        if (display.isPad()) {
            mContent.setMinLines(10);
            mContent.setMaxLines(15);
        } else {
            mContent.setMinLines(5);
            mContent.setMaxLines(10);
        }

        mContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(mCompareResultView);

        setTouchable(true);
        setOutsideTouchable(true);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }


    public void show() {
        showAtLocation(mParent, Gravity.CENTER, 0, 0);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setContent(String content) {
        mContent.scrollTo(0,0);
        mContent.setText(content);
    }
}
