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
package com.foxit.uiextensions.annots.freetext.textbox;


import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class TextBoxEvent extends EditAnnotEvent {

    public TextBoxEvent(int eventType, TextBoxUndoItem undoItem, FreeText textbox, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = textbox;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }
        FreeText annot = (FreeText) mAnnot;
        try {
            annot.setBorderColor(mUndoItem.mColor);

            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

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

            TextBoxAddUndoItem undoItem = (TextBoxAddUndoItem) mUndoItem;
            DefaultAppearance da = new DefaultAppearance();
            da.set(undoItem.mDaFlags, undoItem.mFont, undoItem.mFontSize, undoItem.mTextColor);
            annot.setDefaultAppearance(da);
            annot.setIntent(mUndoItem.mIntent);
            annot.setUniqueID(mUndoItem.mNM);
            annot.setInnerRect(AppUtil.toFxRectF(undoItem.mTextRectF));
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
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }

        try {
            if (((FreeText) mAnnot).getIntent() != null) {
                return false;
            }
            FreeText annot = (FreeText) mAnnot;
            TextBoxModifyUndoItem undoItem = (TextBoxModifyUndoItem) mUndoItem;
            DefaultAppearance da = annot.getDefaultAppearance();
            da.setText_color(undoItem.mTextColor);
            da.setFont(undoItem.mFont);
            da.setText_size(undoItem.mFontSize);
            annot.setDefaultAppearance(da);
            annot.setOpacity(undoItem.mOpacity);
            annot.move(AppUtil.toFxRectF(mUndoItem.mBBox));
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setContent(undoItem.mContents);
            annot.setInnerRect(AppUtil.toFxRectF(undoItem.mTextRectF));
            annot.resetAppearanceStream();
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }

        try {
            if (((FreeText) mAnnot).getIntent() != null) {
                return false;
            }
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
