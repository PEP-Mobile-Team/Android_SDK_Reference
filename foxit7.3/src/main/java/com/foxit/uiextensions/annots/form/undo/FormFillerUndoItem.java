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
package com.foxit.uiextensions.annots.form.undo;


import com.foxit.sdk.common.Constants;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.form.ChoiceItemInfo;

import java.util.ArrayList;

public abstract class FormFillerUndoItem extends AnnotUndoItem {
    public String mValue;
    public String mFieldName;
    public int mFieldType;
    public int mFieldFlags;
    public int mRotation = Constants.e_Rotation0;

    public int mFontId = -1;
    public int mFontColor;
    public float mFontSize;

    public ArrayList<ChoiceItemInfo> mOptions;

    //for radiobutton
    public int mCheckedIndex;
    public boolean mIsChecked;

    //for radiobutton
    public boolean mNeedResetChecked;
}

