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
package com.foxit.uiextensions.annots.line;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class LineEvent extends EditAnnotEvent {

    public LineEvent(int eventType, LineUndoItem undoItem, Line line, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = line;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Line)) {
            return false;
        }
        Line annot = (Line) mAnnot;
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

            if (mUndoItem.mIntent != null) {
                annot.setIntent(mUndoItem.mIntent);
            }

            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }

            LineUndoItem undoItem = (LineUndoItem) mUndoItem;
            annot.setStartPoint(AppUtil.toFxPointF(undoItem.mStartPt));
            annot.setEndPoint(AppUtil.toFxPointF(undoItem.mEndPt));
            annot.setLineStartStyle(undoItem.mStartingStyle);
            annot.setLineEndStyle(undoItem.mEndingStyle);

            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(mUndoItem.mLineWidth);
            annot.setBorderInfo(borderInfo);

            annot.setUniqueID(mUndoItem.mNM);
            if (LineConstants.INTENT_LINE_DIMENSION.equals(undoItem.mIntent)) {
                annot.setMeasureConversionFactor(0, undoItem.factor);
                annot.setMeasureRatio(undoItem.ratio);
                annot.setMeasureUnit(0, undoItem.unit);
//                float distance = AppDmUtil.distanceOfTwoPoints(undoItem.mStartPt,undoItem.mEndPt);
//                annot.setContent(""+distance*undoItem.factor+" "+undoItem.unit);
            }
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
        if (mAnnot == null || !(mAnnot instanceof Line)) {
            return false;
        }
        Line annot = (Line) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (!useOldValue) {
                annot.setBorderColor(mUndoItem.mColor);
                annot.setOpacity(mUndoItem.mOpacity);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mUndoItem.mLineWidth);
                annot.setBorderInfo(borderInfo);
                LineModifyUndoItem undoItem = (LineModifyUndoItem) mUndoItem;
                if (!(undoItem.mStartPt.equals(0, 0) && undoItem.mEndPt.equals(0, 0))) {
                    annot.setStartPoint(AppUtil.toFxPointF(undoItem.mStartPt));
                    annot.setEndPoint(AppUtil.toFxPointF(undoItem.mEndPt));
//                    if (LineConstants.INTENT_LINE_DIMENSION.equals(annot.getIntent())) {
//                        float distance = AppDmUtil.distanceOfTwoPoints(undoItem.mStartPt,undoItem.mEndPt);
//                        mUndoItem.mOldContents = mUndoItem.mContents;
//                        mUndoItem.mContents = ""+distance*annot.getMeasureConversionFactor(0)+" "+annot.getMeasureUnit(0);
//                    }
                }

                annot.setLineStartStyle(undoItem.mStartingStyle);
                annot.setLineEndStyle(undoItem.mEndingStyle);

                if (mUndoItem.mContents != null) {
                    annot.setContent(mUndoItem.mContents);
                } else {
                    annot.setContent("");
                }
            } else {
                annot.setBorderColor(mUndoItem.mOldColor);
                annot.setOpacity(mUndoItem.mOldOpacity);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mUndoItem.mOldLineWidth);
                annot.setBorderInfo(borderInfo);
                LineModifyUndoItem undoItem = (LineModifyUndoItem) mUndoItem;
                annot.setStartPoint(AppUtil.toFxPointF(undoItem.mOldStartPt));
                annot.setEndPoint(AppUtil.toFxPointF(undoItem.mOldEndPt));
                annot.setLineStartStyle(undoItem.mOldStartingStyle);
                annot.setLineEndStyle(undoItem.mOldEndingStyle);

                if (mUndoItem.mOldContents != null) {
                    annot.setContent(mUndoItem.mOldContents);
                } else {
                    annot.setContent("");
                }
            }

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
        if (mAnnot == null || !(mAnnot instanceof Line)) {
            return false;
        }

        try {
            ((Markup)mAnnot).removeAllReplies();
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
