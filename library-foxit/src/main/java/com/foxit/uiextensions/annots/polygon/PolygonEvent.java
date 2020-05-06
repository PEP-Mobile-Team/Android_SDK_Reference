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
package com.foxit.uiextensions.annots.polygon;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Polygon;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class PolygonEvent extends EditAnnotEvent {
    public PolygonEvent(int eventType, PolygonUndoItem undoItem, Polygon polygon, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = polygon;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Polygon)) {
            return false;
        }
        Polygon annot = (Polygon) mAnnot;
        try {
            annot.setVertexes(((PolygonAddUndoItem)mUndoItem).mVertexes);
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

            if (mUndoItem.mIntent != null) {
                annot.setIntent(mUndoItem.mIntent);
            }

            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setStyle(mUndoItem.mBorderStyle);
            borderInfo.setWidth(mUndoItem.mLineWidth);
            if (mUndoItem.mBorderStyle == BorderInfo.e_Cloudy) {
                borderInfo.setCloud_intensity(((PolygonAddUndoItem) mUndoItem).mIntensity);
            }
            annot.setBorderInfo(borderInfo);

            annot.setUniqueID(mUndoItem.mNM);
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
        if (mAnnot == null || !(mAnnot instanceof Polygon)) {
            return false;
        }
        Polygon annot = (Polygon) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            if (mUndoItem.mIntent != null) {
                annot.setIntent(mUndoItem.mIntent);
            }
             if (((PolygonModifyUndoItem)mUndoItem).mVertexes != null) {
                annot.setVertexes(((PolygonModifyUndoItem)mUndoItem).mVertexes);
            } else {
                annot.move(AppUtil.toFxRectF(mUndoItem.mBBox));
            }
            annot.setContent(mUndoItem.mContents);
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setStyle(mUndoItem.mBorderStyle);
            borderInfo.setWidth(mUndoItem.mLineWidth);
            if (mUndoItem.mBorderStyle == BorderInfo.e_Cloudy) {
                borderInfo.setCloud_intensity(((PolygonModifyUndoItem) mUndoItem).mIntensity);
            }
            annot.setBorderInfo(borderInfo);

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
        if (mAnnot == null || !(mAnnot instanceof Polygon)) {
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
