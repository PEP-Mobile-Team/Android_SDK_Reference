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
package com.foxit.uiextensions.utils;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class UIMarqueeTextView extends AppCompatTextView {

    public UIMarqueeTextView(Context context) {
        this(context, null, 0);
    }

    public UIMarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UIMarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.setSingleLine(true);
        this.setEllipsize(TruncateAt.MARQUEE);
        this.setMarqueeRepeatLimit(-1);
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}
