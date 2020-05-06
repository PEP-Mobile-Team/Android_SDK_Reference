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
package com.foxit.uiextensions.annots.screen.image;


import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Image;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class PDFImageEvent extends EditAnnotEvent {

    public PDFImageEvent(int eventType, PDFImageUndoItem undoItem, Screen screen, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = screen;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }
        Screen annot = (Screen) mAnnot;
        try {
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

            annot.setFlags(mUndoItem.mFlags);
//            if (mUndoItem.mCreationDate != null && AppDmUtil.isValidDateTime(mUndoItem.mCreationDate)) {
//                annot.setCreationDateTime(mUndoItem.mCreationDate);
//            }

            if (mUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(mUndoItem.mModifiedDate)) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

//            if (mUndoItem.mAuthor != null) {
//                annot.setTitle(mUndoItem.mAuthor);
//            }
//
//            if (mUndoItem.mIntent != null) {
//                annot.setIntent(mUndoItem.mIntent);
//            }

            PDFImageAddUndoItem item = (PDFImageAddUndoItem) mUndoItem;
            if (item.mPDFDictionary != null) {
                annot.setMKDict(item.mPDFDictionary);
            } else {
                Image image = new Image(item.mImgPath);
                annot.setImage(image, 0, 0);
            }
            annot.setRotation(((PDFImageAddUndoItem) mUndoItem).mRotation);
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
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }
        Screen annot = (Screen) mAnnot;
        try {
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }

            PDFImageModifyUndoItem undoItem = (PDFImageModifyUndoItem) mUndoItem;
            annot.setRotation(undoItem.mRotation);
            annot.setOpacity(undoItem.mOpacity);
            annot.move(AppUtil.toFxRectF(mUndoItem.mBBox));
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setContent(undoItem.mContents);
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
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }

        try {
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
