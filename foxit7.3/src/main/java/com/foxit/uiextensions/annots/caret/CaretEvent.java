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
package com.foxit.uiextensions.annots.caret;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;

public class CaretEvent extends EditAnnotEvent {
    public CaretEvent(int eventType, CaretUndoItem undoItem, Caret caret, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = caret;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }

        try {
            Caret annot = (Caret) mAnnot;
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

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
            annot.setIntent(mUndoItem.mIntent);
            annot.setUniqueID(mUndoItem.mNM);
            int rotate = ((CaretAddUndoItem) mUndoItem).mRotate;
            if (rotate < Constants.e_Rotation0 || rotate > Constants.e_RotationUnknown) {
                rotate = 0;
            }
            annot.getDict().setAt("Rotate", PDFObject.createFromInteger(360 - rotate * 90));
            annot.resetAppearanceStream();

            if (((CaretAddUndoItem)mUndoItem).mIsReplace) {
                if (((CaretAddUndoItem)mUndoItem).strikeOutEvent != null && ((CaretAddUndoItem)mUndoItem).strikeOutEvent.add()) {
                    StrikeOut strikeOut = (StrikeOut) ((CaretAddUndoItem)mUndoItem).strikeOutEvent.mAnnot;
//                    strikeOut.setIntent("StrikeOutTextEdit");
                    MarkupArray groups = new MarkupArray();
                    groups.add(annot); // Replace Caret
                    groups.add(strikeOut);
                    annot.getPage().setAnnotGroup(groups, 0);
                    strikeOut.resetAppearanceStream();
                }
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
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
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }
        try {
            Caret annot = (Caret) mAnnot;
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            annot.resetAppearanceStream();
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }
        try {
            PDFPage page = mAnnot.getPage();
            //delete strikeout
            if (AppAnnotUtil.isReplaceCaret(mAnnot)) {
                Caret caret = (Caret) mAnnot;
                MarkupArray markupArray = caret.getGroupElements();
                int nCount = (int) markupArray.getSize();
                for (int i = nCount - 1; i >= 0; i --) {
                    Markup groupAnnot = markupArray.getAt(i);
                    if (groupAnnot.getType() == Annot.e_StrikeOut) {
                        page.removeAnnot(groupAnnot);
                        break;
                    }
                }
            }
            page.removeAnnot(mAnnot);// delete caret
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}
