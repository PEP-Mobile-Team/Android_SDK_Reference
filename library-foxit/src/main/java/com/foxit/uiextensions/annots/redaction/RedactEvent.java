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
package com.foxit.uiextensions.annots.redaction;


import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class RedactEvent extends EditAnnotEvent {

    public RedactEvent(int eventType, RedactUndoItem undoItem, Redact redact, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = redact;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Redact)) {
            return false;
        }
        Redact annot = (Redact) mAnnot;

        try {
            RedactAddUndoItem undoItem = (RedactAddUndoItem) mUndoItem;
            if (undoItem.mQuadPointsArray != null && undoItem.mQuadPointsArray.getSize() > 0) {
                annot.setQuadPoints(undoItem.mQuadPointsArray);
            }
            if (undoItem.mContents != null) {
                annot.setContent(undoItem.mContents);
            }
            if (undoItem.mCreationDate != null && AppDmUtil.isValidDateTime(undoItem.mCreationDate)) {
                annot.setCreationDateTime(undoItem.mCreationDate);
            }
            if (undoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(undoItem.mModifiedDate)) {
                annot.setModifiedDateTime(undoItem.mModifiedDate);
            }
            if (undoItem.mAuthor != null) {
                annot.setTitle(undoItem.mAuthor);
            }
            if (undoItem.mSubject != null) {
                annot.setSubject(undoItem.mSubject);
            }
            if (undoItem.mOverlayText != null) {
                DefaultAppearance da = new DefaultAppearance();
                da.set(undoItem.mDaFlags, undoItem.mFont, undoItem.mFontSize, undoItem.mTextColor);
                annot.setDefaultAppearance(da);
                annot.setOverlayText(undoItem.mOverlayText);
            }
            annot.setFlags(undoItem.mFlags);
            annot.setBorderColor(undoItem.mBorderColor);
            annot.setApplyFillColor(undoItem.mFillColor);
            annot.setUniqueID(undoItem.mNM);
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
        if (mAnnot == null || !(mAnnot instanceof Redact)) {
            return false;
        }

        try {
            Redact annot = (Redact) mAnnot;
            RedactModifyUndoItem undoItem = (RedactModifyUndoItem) mUndoItem;

            // overlaytext
            DefaultAppearance da = annot.getDefaultAppearance();
            Font font = da.getFont();
            String overlayText = annot.getOverlayText();
            if (!TextUtils.isEmpty(overlayText) || font != null && !font.isEmpty()) {
                da.setFlags(undoItem.mDaFlags);
                da.setText_color(undoItem.mTextColor);
                da.setFont(undoItem.mFont);
                da.setText_size(undoItem.mFontSize);
                annot.setDefaultAppearance(da);
                annot.setOverlayText(undoItem.mOverlayText);
            }
            annot.move(AppUtil.toFxRectF(mUndoItem.mBBox));
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            if (undoItem.mContents != null) {
                annot.setContent(undoItem.mContents);
            }
            annot.setBorderColor(undoItem.mBorderColor);
            annot.setApplyFillColor(undoItem.mFillColor);
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
        if (mAnnot == null || !(mAnnot instanceof Redact)) {
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
