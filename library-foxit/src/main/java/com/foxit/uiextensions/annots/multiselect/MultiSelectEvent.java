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
package com.foxit.uiextensions.annots.multiselect;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class MultiSelectEvent extends EditAnnotEvent {
    private ArrayList<Annot> mAnnots;
    private ArrayList<EditAnnotEvent> mAnnotList;
    public MultiSelectEvent(int eventType, MultiSelectUndoItem undoItem, ArrayList<Annot> annots, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnots = annots;
        mPdfViewCtrl = pdfViewCtrl;
    }

    public MultiSelectEvent(int eventType, ArrayList<EditAnnotEvent> annotList, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mAnnotList = annotList;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event: mAnnotList) {
            event.add();
        }
        return true;
    }

    @Override
    public boolean modify() {
        if (mAnnots == null || mAnnots.size() == 0) return false;
        try {
            MultiSelectModifyUndoItem undoItem = (MultiSelectModifyUndoItem)mUndoItem;
            for (Annot annot : mAnnots) {
                if (mUndoItem.mModifiedDate != null) {
                    annot.setModifiedDateTime(mUndoItem.mModifiedDate);
                }
                String nm = annot.getUniqueID();
                if (undoItem.mCurrentAnnots.get(nm) != null) {
                    annot.move(AppUtil.toFxRectF(undoItem.mCurrentAnnots.get(nm)));
                    annot.resetAppearanceStream();
                }
            }
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
        }

        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event: mAnnotList) {
            event.delete();
        }
        return true;
    }

    @Override
    public boolean flatten() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event: mAnnotList) {
            if (!event.flatten()) return false;
        }
        return true;
    }
}
