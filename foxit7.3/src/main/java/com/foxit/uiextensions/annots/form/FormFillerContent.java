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


import android.graphics.RectF;
import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class FormFillerContent implements IFormFillerContent {

    private Widget mWidget;
    private PDFViewCtrl mPDFViewCtrl;

    public String mFieldName;
    public String mFieldValue;
    public int mFieldFlags = -1;

    public int mFontId = -1;
    public int mFontColor = -1;
    public float mFontSize = -1;

    public RectF mRectF;
    public int mPageIndex = -1;

    public Boolean mIsChecked = null;
    public int mCheckedIndex = -1;

    public ArrayList<ChoiceItemInfo> mOptions;

    public FormFillerContent(PDFViewCtrl pdfViewCtrl, Widget widget) {
        mWidget = widget;
        mPDFViewCtrl = pdfViewCtrl;
    }

    @Override
    public String getFieldValue() {
        String value = null;
        try {
            if (mFieldValue != null)
                value = mFieldValue;
            else
                value = mWidget.getField().getValue();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public String getFieldName() {
        String name = null;
        try {
            if (mFieldName != null)
                name = mFieldName;
            else
                name = mWidget.getField().getName();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public int getFieldType() {
        int fieldType = Field.e_TypeUnknown;
        try {
            fieldType = mWidget.getField().getType();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return fieldType;
    }

    @Override
    public int getFieldFlags() {
        int flags = 0;
        try {
            if (mFieldFlags != -1)
                flags = mFieldFlags;
            else
                flags = mWidget.getField().getFlags();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return flags;
    }

    @Override
    public int getFontId() {
        int id = Font.e_StdIDCourier;
        try {
            if (mFontId != -1) {
                id = mFontId;
            } else {
                DefaultAppearance da;
                if (mWidget.getField().getType() == Field.e_TypeRadioButton)
                    da = mWidget.getControl().getDefaultAppearance();
                else
                    da = mWidget.getField().getDefaultAppearance();
                Font font = da != null ? da.getFont() : null;
                if (font != null && !font.isEmpty())
                    id = font.getStandard14Font(mPDFViewCtrl.getDoc());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public int getFontColor() {
        int fontColor = 0;
        try {
            if (mFontColor != -1) {
                fontColor = mFontColor;
            } else {
                DefaultAppearance da;
                if (mWidget.getField().getType() == Field.e_TypeRadioButton)
                    da = mWidget.getControl().getDefaultAppearance();
                else
                    da = mWidget.getField().getDefaultAppearance();
                fontColor = da.getText_color();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return fontColor;
    }

    @Override
    public float getFontSize() {
        float fontSize = 0;
        try {
            if (mFontSize != -1) {
                fontSize = mFontSize;
            } else {
                DefaultAppearance da;
                if (mWidget.getField().getType() == Field.e_TypeRadioButton)
                    da = mWidget.getControl().getDefaultAppearance();
                else
                    da = mWidget.getField().getDefaultAppearance();
                fontSize = da.getText_size();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return fontSize;
    }

    @Override
    public int getMKRotation() {
        int rotation = Constants.e_Rotation0;
        try {
            rotation = mWidget.getMKRotation();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rotation;
    }

    @Override
    public Boolean isChecked() {
        boolean isChecked = false;
        try {
            if (mIsChecked != null) {
                isChecked = mIsChecked;
            } else {
                isChecked = mWidget.getControl().isChecked();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return isChecked;
    }

    @Override
    public int getCheckedIndex() {
        int checkedIndex = -1;
        try {
            if (mCheckedIndex != -1) {
                checkedIndex = mCheckedIndex;
            } else {
                int fieldType = mWidget.getField().getType();
                if (fieldType == Field.e_TypeRadioButton) {
                    checkedIndex = FormFillerUtil.getCheckedIndex(mWidget.getField());
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return checkedIndex;
    }

    @Override
    public Annot getAnnot() {
        return mWidget;
    }

    @Override
    public ArrayList<ChoiceItemInfo> getOptions() {
        ArrayList<ChoiceItemInfo> infos = new ArrayList<>();
        try {
            if (mOptions != null) {
                infos = mOptions;
            } else {
                int fieldType = mWidget.getField().getType();
                if (fieldType == Field.e_TypeComboBox || fieldType == Field.e_TypeListBox) {
                    infos = FormFillerUtil.getOptions(mWidget.getField());
                    return infos;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return infos;
    }

    @Override
    public int getPageIndex() {
        int pageIndex = -1;
        try {
            if (mPageIndex != -1)
                pageIndex = mPageIndex;
            else
                pageIndex = mWidget.getPage().getIndex();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return pageIndex;
    }

    @Override
    public int getType() {
        int type = Annot.e_UnknownType;
        try {
            type = mWidget.getType();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return type;
    }

    @Override
    public String getNM() {
        return AppAnnotUtil.getAnnotUniqueID(mWidget);
    }

    @Override
    public RectF getBBox() {
        RectF rectF = new RectF();
        try {
            if (mRectF != null)
                rectF = mRectF;
            else
                rectF = AppUtil.toRectF(mWidget.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public float getLineWidth() {
        return 0;
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public DateTime getModifiedDate() {
        return AppDmUtil.currentDateToDocumentDate();
    }

    @Override
    public String getContents() {
        String content = null;
        try {
            content = mWidget.getContent();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return content;
    }

    @Override
    public String getIntent() {
        return null;
    }

}
