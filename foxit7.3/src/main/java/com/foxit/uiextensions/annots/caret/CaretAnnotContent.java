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
package com.foxit.uiextensions.annots.caret;

import com.foxit.sdk.common.DateTime;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.utils.AppDmUtil;

public abstract class CaretAnnotContent implements AnnotContent {

    private boolean mIsInsert = false;
    public CaretAnnotContent(boolean isTnsert){
        mIsInsert = isTnsert;
    }
    abstract public TextSelector getTextSelector();

    abstract public DateTime getCreatedDate();

    abstract public int getRotate();

    @Override
    public int getPageIndex() {
        return 0;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public float getLineWidth() {
        return 0;
    }

    public String getAuthor() {
        return AppDmUtil.getAnnotAuthor();
    }

    @Override
    public String getNM() {
        return AppDmUtil.randomUUID(null);
    }

    @Override
    public String getIntent() {
        return mIsInsert ? "Insert Text" : "Replace";
    }

    @Override
    public String getSubject() {
        return mIsInsert ? "Insert Text" : "Replace";
    }

}

