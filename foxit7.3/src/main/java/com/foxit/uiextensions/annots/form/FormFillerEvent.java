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
package com.foxit.uiextensions.annots.form;


import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.ChoiceOptionArray;
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

import java.util.ArrayList;

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
//            if (addUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(addUndoItem.mModifiedDate)) {
//                mWidget.setModifiedDateTime(addUndoItem.mModifiedDate);
//            }
            if (addUndoItem.mValue != null) {
                mWidget.getField().setValue(addUndoItem.mValue);
            }
            int fieldType = FormFillerUtil.getAnnotFieldType(mWidget);
            if (addUndoItem.mFontId != -1) {
                DefaultAppearance da = new DefaultAppearance();
                int flags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagFontSize | DefaultAppearance.e_FlagTextColor;
                Font font = new Font(addUndoItem.mFontId);
                da.set(flags, font, addUndoItem.mFontSize, addUndoItem.mFontColor);
                if (fieldType == Field.e_TypeRadioButton)
                    mWidget.getControl().setDefaultAppearance(da);
                else
                    mWidget.getField().setDefaultAppearance(da);
            }

            if (fieldType == Field.e_TypeComboBox || fieldType == Field.e_TypeListBox) {
                mWidget.getField().setFlags(addUndoItem.mFieldFlags);
                if (addUndoItem.mOptions != null) {
                    ChoiceOptionArray choiceOptionArray = FormFillerUtil.options2Native(addUndoItem.mOptions);
                    mWidget.getField().setOptions(choiceOptionArray);
                }
            } else if (fieldType == Field.e_TypeRadioButton) {
                if (mWidget.getControl().isChecked() != addUndoItem.mIsChecked)
                    mWidget.getControl().setChecked(addUndoItem.mIsChecked);
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
            boolean isModify = false;
            FormFillerModifyUndoItem undoItem = (FormFillerModifyUndoItem) mUndoItem;

            if (undoItem.mValue != null && !mWidget.getField().getValue().equals(undoItem.mValue)) {
                isModify = true;
                mWidget.getField().setValue(undoItem.mValue);
            }
            int fieldType = FormFillerUtil.getAnnotFieldType(mWidget);

            if (fieldType != Field.e_TypeSignature) {
                DefaultAppearance da;
                if (fieldType == Field.e_TypeRadioButton)
                    da = mWidget.getControl().getDefaultAppearance();
                else
                    da = mWidget.getField().getDefaultAppearance();

                Font font = da.getFont();
                if ((!font.isEmpty() && undoItem.mFontId != FormFillerUtil.getStandard14Font(da, mPdfViewCtrl.getDoc()))
                        || (da.getText_size() != undoItem.mFontSize)
                        || (da.getText_color() != undoItem.mFontColor)) {
                    isModify = true;
                    int flags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagFontSize | DefaultAppearance.e_FlagTextColor;
                    da.setFlags(flags);
                    da.setFont(new Font(undoItem.mFontId));
                    da.setText_color(undoItem.mFontColor);
                    da.setText_size(undoItem.mFontSize);

                    if (fieldType == Field.e_TypeRadioButton)
                        mWidget.getControl().setDefaultAppearance(da);
                    else
                        mWidget.getField().setDefaultAppearance(da);
                }

                if (fieldType == Field.e_TypeRadioButton) {
                    if (!TextUtils.isEmpty(undoItem.mFieldName) && !undoItem.mFieldName.equals(mWidget.getField().getName())) {
                        isModify = true;
                        Form form = new Form(mPdfViewCtrl.getDoc());
                        FormFillerUtil.renameField(form, mWidget.getControl(), undoItem.mFieldName);
                    }
                    if (undoItem.mNeedResetChecked
                            && !mWidget.getField().getControl(undoItem.mCheckedIndex).isEmpty()
                            && !mWidget.getField().getControl(undoItem.mCheckedIndex).isChecked()) {
                        isModify = true;
                        mWidget.getField().getControl(undoItem.mCheckedIndex).setChecked(true);
                    }
                } else if (fieldType == Field.e_TypeComboBox || fieldType == Field.e_TypeListBox) {
                    if (mWidget.getField().getFlags() != undoItem.mFieldFlags) {
                        isModify = true;
                        mWidget.getField().setFlags(undoItem.mFieldFlags);
                    }

                    ArrayList<ChoiceItemInfo> options = FormFillerUtil.getOptions(mWidget.getField());
                    if (undoItem.mOptions != null && FormFillerUtil.optionsIsChanged(undoItem.mOptions, options)) {
                        isModify = true;
                        ChoiceOptionArray choiceOptionArray = FormFillerUtil.options2Native(undoItem.mOptions);
                        mWidget.getField().setOptions(choiceOptionArray);
                    }
                }
            }

            if (isModify)
                mWidget.resetAppearanceStream();

            if (!undoItem.mBBox.equals(AppUtil.toRectF(mWidget.getRect()))) {
//                isModify = true;
                mWidget.move(AppUtil.toFxRectF(undoItem.mBBox));
            }

//            if (undoItem.mModifiedDate != null && isModify) {
//                mWidget.setModifiedDateTime(undoItem.mModifiedDate);
//            }
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
            if (Field.e_TypeSignature == deleteUndoItem.mFieldType) {
                PDFDoc doc = mPdfViewCtrl.getDoc();
                Field field = mWidget.getField();
                Signature signature = new Signature(field);
                doc.removeSignature(signature);
            } else {
                Field field = mWidget.getField();
                Form form = new Form(mPdfViewCtrl.getDoc());
                if (field.getControlCount() <= 1) {
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
