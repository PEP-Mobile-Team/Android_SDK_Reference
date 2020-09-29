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
package com.foxit.uiextensions.annots.form.undo;


import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Form;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.form.FormFillerEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public class FormFillerDeleteUndoItem extends FormFillerUndoItem {
    public FormFillerDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        FormFillerAddUndoItem undoItem = new FormFillerAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mValue = mValue;
        undoItem.mFieldName = mFieldName;
        undoItem.mFieldType = mFieldType;
        undoItem.mFieldFlags = mFieldFlags;
        undoItem.mRotation = mRotation;
        undoItem.mOptions = mOptions;
        undoItem.mIsChecked = mIsChecked;

        undoItem.mFontId = mFontId;
        undoItem.mFontColor = mFontColor;
        undoItem.mFontSize = mFontSize;

        try {
            PDFDoc doc = mPdfViewCtrl.getDoc();
            final PDFPage page = doc.getPage(mPageIndex);
            final Widget annot;

            if (Field.e_TypeSignature == mFieldType) {
                final Signature signature = page.addSignature(AppUtil.toFxRectF(mBBox), mFieldName);
                annot = signature.getControl(0).getWidget();
            } else {
                Form form = new Form(doc);
                Control control = form.addControl(page, mFieldName, mFieldType, AppUtil.toFxRectF(mBBox));
                annot = control.getWidget();
            }
            FormFillerEvent addEvent = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRectF));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            final Annot annot = documentManager.getAnnot(page, mNM);
            if (!(annot instanceof Widget)) {
                return false;
            }
            if (annot == documentManager.getCurrentAnnot()) {
                documentManager.setCurrentAnnot(null, false);
            }

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            FormFillerEvent deleteEvent = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Widget) annot, mPdfViewCtrl);
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

}
