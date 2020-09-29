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
package com.foxit.uiextensions.annots.freetext.callout;


import android.graphics.PointF;
import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.fxcrt.PointFArray;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.freetext.FtUtil;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class CalloutEvent extends EditAnnotEvent {

    public CalloutEvent(int eventType, CalloutUndoItem undoItem, FreeText callout, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = callout;
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

            CalloutAddUndoItem undoItem = (CalloutAddUndoItem) mUndoItem;
            DefaultAppearance da = new DefaultAppearance();
            Font font;
            if (undoItem.mFontId >= Font.e_StdIDCourier && undoItem.mFontId <= Font.e_StdIDZapfDingbats)
                font = new Font(undoItem.mFontId);
            else
                font = new Font(Font.e_StdIDCourier);
            da.set(undoItem.mDaFlags, font, undoItem.mFontSize, undoItem.mTextColor);
            annot.setDefaultAppearance(da);
            annot.setIntent(mUndoItem.mIntent);
            annot.setUniqueID(mUndoItem.mNM);
            annot.setInnerRect(AppUtil.toFxRectF(undoItem.mTextBBox));
            PointFArray _pArray = new PointFArray();
            _pArray.add(AppUtil.toFxPointF(undoItem.mStartingPt));
            _pArray.add(AppUtil.toFxPointF(undoItem.mKneePt));
            _pArray.add(AppUtil.toFxPointF(undoItem.mEndingPt));
            annot.setCalloutLinePoints(_pArray);
            annot.setCalloutLineEndingStyle(Markup.e_EndingStyleOpenArrow);
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
            if (((FreeText) mAnnot).getIntent() == null
                    || !((FreeText) mAnnot).getIntent().equals("FreeTextCallout")) {
                return false;
            }
            FreeText annot = (FreeText) mAnnot;
            CalloutModifyUndoItem undoItem = (CalloutModifyUndoItem) mUndoItem;
            DefaultAppearance da = annot.getDefaultAppearance();
            int flags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
            da.setFlags(flags);
            if (!AppUtil.toRectF(annot.getRect()).equals(undoItem.mBBox))
                annot.move(AppUtil.toFxRectF(undoItem.mBBox));

            boolean needReset = false;
            if (da.getText_color() != undoItem.mTextColor) {
                needReset = true;
                da.setText_color(undoItem.mTextColor);
            }
            if (undoItem.mFontSize != da.getText_size()) {
                needReset = true;
                da.setText_size(undoItem.mFontSize);
            }
            if (undoItem.mFontId != AppAnnotUtil.getStandard14Font(da, mPdfViewCtrl.getDoc())) {
                needReset = true;
                Font font;
                if (undoItem.mFontId >= Font.e_StdIDCourier && undoItem.mFontId <= Font.e_StdIDZapfDingbats)
                    font = new Font(undoItem.mFontId);
                else
                    font = new Font(Font.e_StdIDCourier);
                da.setFont(font);
            }
            annot.setDefaultAppearance(da);

            if ((int) (annot.getOpacity() * 255f + 0.5f) != (int) (undoItem.mOpacity * 255f + 0.5f) ) {
                needReset = true;
                annot.setOpacity(undoItem.mOpacity);
            }
            if (TextUtils.isEmpty(annot.getContent()) || !annot.getContent().equals(undoItem.mContents)) {
                needReset = true;
                annot.setContent(undoItem.mContents);
            }
            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(annot);
            PointF startingPt = points.get(0);
            PointF kneePt = points.get(1);
            PointF endingPt = points.get(2);
            if (!startingPt.equals(undoItem.mStartingPt) || !kneePt.equals(undoItem.mKneePt) || !endingPt.equals(undoItem.mEndingPt)) {
                needReset = true;
                PointFArray _pArray = new PointFArray();
                _pArray.add(AppUtil.toFxPointF(undoItem.mStartingPt));
                _pArray.add(AppUtil.toFxPointF(undoItem.mKneePt));
                _pArray.add(AppUtil.toFxPointF(undoItem.mEndingPt));
                annot.setCalloutLinePoints(_pArray);
            }
            if (!AppUtil.toRectF(annot.getInnerRect()).equals(undoItem.mTextBBox)) {
                needReset = true;
                annot.setInnerRect(AppUtil.toFxRectF(undoItem.mTextBBox));
            }
            if (needReset) {
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.resetAppearanceStream();
            }
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
            if (((FreeText) mAnnot).getIntent() == null
                    || !((FreeText) mAnnot).getIntent().equals("FreeTextCallout")) {
                return false;
            }
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

