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
package com.foxit.uiextensions.annots.common;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.utils.Event;


public abstract class EditAnnotEvent extends Event {
    public static final int EVENTTYPE_ADD = 1;
    public static final int EVENTTYPE_MODIFY = 2;
    public static final int EVENTTYPE_DELETE = 3;
    public static final int EVENTTYPE_FLATTEN = 4;

    public abstract boolean add();
    public abstract boolean modify();
    public abstract boolean delete();
    public boolean flatten() {
        return false;
    }

    protected boolean execute() {
        switch (mType) {
            case EVENTTYPE_ADD:
                return add();
            case EVENTTYPE_MODIFY:
                return modify();
            case EVENTTYPE_DELETE:
                return delete();
            case EVENTTYPE_FLATTEN:
                return flatten();
            default:
                return false;
        }
    }


    public AnnotUndoItem mUndoItem;
    public Annot mAnnot;
    public PDFViewCtrl mPdfViewCtrl;
    public boolean useOldValue;

    public boolean isModifyDocument() {
        return true;
    }
}
