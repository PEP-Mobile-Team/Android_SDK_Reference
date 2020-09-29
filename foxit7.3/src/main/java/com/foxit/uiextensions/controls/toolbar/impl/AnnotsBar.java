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

import com.foxit.uiextensions.utils.AppDisplay;

public class AnnotsBar extends BaseBarImpl {
    public AnnotsBar(Context context) {
        super(context);
        mDefaultSpace = dip2px(4);
        if (AppDisplay.getInstance(context).isPad()) {
            mDefaultSpace = dip2px(6);
        }
    }
}
