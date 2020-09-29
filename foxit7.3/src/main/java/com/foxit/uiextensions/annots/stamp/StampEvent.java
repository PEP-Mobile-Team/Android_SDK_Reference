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
package com.foxit.uiextensions.annots.stamp;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class StampEvent extends EditAnnotEvent {

    public StampEvent(int eventType, StampUndoItem undoItem, Stamp stamp, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = stamp;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }
        Stamp annot = (Stamp) mAnnot;
        StampAddUndoItem undoItem = (StampAddUndoItem) mUndoItem;
        try {
            annot.setUniqueID(mUndoItem.mNM);
            annot.setFlags(mUndoItem.mFlags);
            if (mUndoItem.mCreationDate != null && AppDmUtil.isValidDateTime(mUndoItem.mCreationDate)) {
                annot.setCreationDateTime(mUndoItem.mCreationDate);
            }

            if (mUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(mUndoItem.mModifiedDate)) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (mUndoItem.mAuthor != null) {
                annot.setTitle(mUndoItem.mAuthor);
            }

            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }

            if (undoItem.mIconName != null) {
                annot.setIconName(undoItem.mIconName);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setContent(mUndoItem.mContents);

            if (!DynamicStampIconProvider.getInstance(mPdfViewCtrl.getContext()).addIcon(undoItem.mIconName)) {
                return false;
            }
            annot.setRotation(undoItem.mRotation);
            annot.resetAppearanceStream();
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }

        StampModifyUndoItem undoItem = (StampModifyUndoItem) mUndoItem;
        Stamp annot = (Stamp) mAnnot;
        try {
            if (undoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(undoItem.mModifiedDate);
            }
            if (undoItem.mContents == null) {
                undoItem.mContents = "";
            }
            if (undoItem.mRotation != annot.getRotation()) {
                annot.setRotation(undoItem.mRotation);
            }
            annot.setContent(undoItem.mContents);
            annot.move(AppUtil.toFxRectF(undoItem.mBBox));
            annot.resetAppearanceStream();
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }

        try {
            ((Markup) mAnnot).removeAllReplies();
            PDFPage page = mAnnot.getPage();
            page.removeAnnot(mAnnot);
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}
