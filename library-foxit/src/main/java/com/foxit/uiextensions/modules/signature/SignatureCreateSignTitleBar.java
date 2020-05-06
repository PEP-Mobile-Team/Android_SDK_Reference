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
package com.foxit.uiextensions.modules.signature;


import android.content.Context;

import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;


public class SignatureCreateSignTitleBar extends TopBarImpl {
    public SignatureCreateSignTitleBar(Context context) {
        super(context);
        if (AppDisplay.getInstance(context).isPad()) {
            mRightSideInterval = mLeftSideInterval;
        } else {
            mLeftSideInterval = mRightSideInterval;
        }
    }
}
