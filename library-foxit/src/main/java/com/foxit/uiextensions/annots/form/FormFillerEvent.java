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
package com.foxit.uiextensions.annots.form;


import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Form;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.form.undo.FormFillerAddUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerDeleteUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerModifyUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerUndoItem;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class FormFillerEvent extends EditAnnotEvent {

    private Widget mWidget;

    public FormFillerEvent(int eventType, FormFillerUndoItem undoItem, Widget widget, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mWidget = widget;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        FormFillerAddUndoItem addUndoItem = (FormFillerAddUndoItem) mUndoItem;

        if (mWidget == null || mWidget.isEmpty())
            return false;
        try {
            if (addUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(addUndoItem.mModifiedDate)) {
                mWidget.setModifiedDateTime(addUndoItem.mModifiedDate);
            }
            if (addUndoItem.mValue != null) {
                mWidget.getField().setValue(addUndoItem.mValue);
            }
            mWidget.setMKRotation(addUndoItem.mRotation);
            mWidget.setFlags(addUndoItem.mFlags);
            mWidget.setUniqueID(addUndoItem.mNM);
            mWidget.resetAppearanceStream();
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
        if (mWidget == null || mWidget.isEmpty()) {
            return false;
        }

        try {
            FormFillerModifyUndoItem undoItem = (FormFillerModifyUndoItem) mUndoItem;
            if (undoItem.mModifiedDate != null) {
                mWidget.setModifiedDateTime(undoItem.mModifiedDate);
            }
//            if (undoItem.mValue != null) {
//                mWidget.getField().setValue(undoItem.mValue);
//            }
            mWidget.move(AppUtil.toFxRectF(undoItem.mBBox));
            mWidget.resetAppearanceStream();
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
        if (mWidget == null || mWidget.isEmpty()) {
            return false;
        }
        try {
            FormFillerDeleteUndoItem deleteUndoItem = (FormFillerDeleteUndoItem) mUndoItem;
            if (Field.e_TypeSignature == deleteUndoItem.mFieldType){
                PDFDoc doc = mPdfViewCtrl.getDoc();
                Field field = mWidget.getField();
                Signature signature = new Signature(field);
                doc.removeSignature(signature);
            } else {
                Field field = mWidget.getField();
                Form form = new Form(mPdfViewCtrl.getDoc());
                if (field.getControlCount() <= 1){
                    form.removeField(field);
                } else {
                    form.removeControl(mWidget.getControl());
                }
                form.delete();
            }
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

}
