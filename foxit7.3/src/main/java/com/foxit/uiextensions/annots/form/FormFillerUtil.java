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

import com.foxit.sdk.PDFException;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.ChoiceOption;
import com.foxit.sdk.pdf.interform.ChoiceOptionArray;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Form;

import java.util.ArrayList;
import java.util.ListIterator;


public class FormFillerUtil {

    protected static int getAnnotFieldType(Annot annot) {
        int type = 0;
        try {
            if (annot == null || annot.isEmpty() || !(annot instanceof Widget)) return type;
            Control control = ((Widget) annot).getControl();
            if (control != null && !control.isEmpty())
                type = control.getField().getType();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return type;
    }

    public static boolean isReadOnly(Annot annot) {
        boolean bRet = false;

        try {
            long flags = annot.getFlags();
            bRet = ((flags & Annot.e_FlagReadOnly) != 0);
            Field field = ((Widget) annot).getField();
            int fieldType = field.getType();
            int fieldFlag = field.getFlags();
            switch (fieldType) {
                case Field.e_TypeUnknown:
                case Field.e_TypePushButton:
                    bRet = false;
                    break;
                case Field.e_TypeCheckBox:
                case Field.e_TypeRadioButton:
                case Field.e_TypeComboBox:
                case Field.e_TypeListBox:
                case Field.e_TypeSignature:
                case Field.e_TypeTextField:
                    bRet = (Field.e_FlagReadOnly & fieldFlag) != 0;
                    break;
                default:
                    break;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bRet;
    }

    protected static boolean isVisible(Annot annot) {
        boolean ret = false;
        long flags = 0;
        try {
            flags = annot.getFlags();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        ret = !((flags & Annot.e_FlagInvisible) != 0 || (flags & Annot.e_FlagHidden) != 0 || (flags & Annot.e_FlagNoView) != 0);
        return ret;
    }

    public static boolean isEmojiCharacter(int codePoint) {
        return (codePoint == 0x0) || (codePoint == 0x9)
                || (codePoint == 0xa9) || (codePoint == 0xae) || (codePoint == 0x303d)
                || (codePoint == 0x3030) || (codePoint == 0x2b55) || (codePoint == 0x2b1c)
                || (codePoint == 0x2b1b) || (codePoint == 0x2b50)
                || ((codePoint >= 0x1F0CF) && (codePoint <= 0x1F6B8))
                || (codePoint == 0xD) || (codePoint == 0xDE0D)
                || ((codePoint >= 0x2100) && (codePoint <= 0x27FF))
                || ((codePoint >= 0x2B05) && (codePoint <= 0x2B07))
                || ((codePoint >= 0x2934) && (codePoint <= 0x2935))
                || ((codePoint >= 0x203C) && (codePoint <= 0x2049))
                || ((codePoint >= 0x3297) && (codePoint <= 0x3299))
                || ((codePoint >= 0x1F600) && (codePoint <= 0x1F64F))
                || ((codePoint >= 0xDC00) && (codePoint <= 0xE678));

    }

    public static ArrayList<ChoiceItemInfo> getOptions(Field field) {
        ArrayList<ChoiceItemInfo> infos = new ArrayList<>();
        try {
            if (field.getType() == Field.e_TypeComboBox || field.getType() == Field.e_TypeListBox) {
                ChoiceOptionArray options = field.getOptions();
                long size = options.getSize();
                if (size > 0) {
                    for (int i = 0; i < size; i++) {
                        ChoiceOption option = options.getAt(i);
                        ChoiceItemInfo itemInfo = new ChoiceItemInfo();
                        itemInfo.optionValue = option.getOption_value() == null ? "" : option.getOption_value();
                        itemInfo.optionLabel = option.getOption_label() == null ? "" : option.getOption_label();
                        itemInfo.defaultSelected = option.getDefault_selected();
                        itemInfo.selected = option.getSelected();
                        infos.add(itemInfo);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return infos;
    }

    public static boolean optionsIsChanged(ArrayList<ChoiceItemInfo> src, ArrayList<ChoiceItemInfo> dest) {
        if (src == null && dest == null)
            return false;
        if (src == null || dest == null)
            return true;

        ListIterator<ChoiceItemInfo> e1 = src.listIterator();
        ListIterator<ChoiceItemInfo> e2 = dest.listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            ChoiceItemInfo o1 = e1.next();
            ChoiceItemInfo o2 = e2.next();
            if (!o1.equals(o2))
                return true;
        }
        return e1.hasNext() || e2.hasNext();
    }

    public static ChoiceOptionArray options2Native(ArrayList<ChoiceItemInfo> infos) {
        ChoiceOptionArray choiceOptionArray = new ChoiceOptionArray();
        for (ChoiceItemInfo itemInfo : infos) {
            ChoiceOption choiceOption = new ChoiceOption();
            choiceOption.setOption_value(itemInfo.optionValue);
            choiceOption.setOption_label(itemInfo.optionLabel);
            choiceOption.setSelected(itemInfo.selected);
            choiceOption.setDefault_selected(itemInfo.defaultSelected);
            choiceOptionArray.add(choiceOption);
        }
        return choiceOptionArray;
    }

    public static ArrayList<ChoiceItemInfo> cloneChoiceOptions(ArrayList<ChoiceItemInfo> srcList) {
        ArrayList<ChoiceItemInfo> infos = new ArrayList<>();
        if (srcList != null) {
            for (ChoiceItemInfo itemInfo : srcList) {
                infos.add(itemInfo.clone());
            }
        }
        return infos;
    }

    public static int getCheckedIndex(Field field) {
        int checkedIndex = -1;
        try {
            int controlCount = field.getControlCount();
            for (int i = 0; i < controlCount; i++) {
                if (field.getControl(i).isChecked()) {
                    checkedIndex = i;
                    break;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return checkedIndex;
    }

    public static Font getSupportFont(String name) {
        Font font = null;
        try {
            if (name == null) {
                font = new Font(Font.e_StdIDCourier);
            } else if (name.equals("Courier")) {
                font = new Font(Font.e_StdIDCourier);
            } else if (name.equals("Helvetica")) {
                font = new Font(Font.e_StdIDHelvetica);
            } else if (name.equals("Times")) {
                font = new Font(Font.e_StdIDTimes);
            } else {
                font = new Font(Font.e_StdIDCourier);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return font;
    }

    public static int getStandard14Font(DefaultAppearance da, PDFDoc doc) {
        int id = Font.e_StdIDCourier;
        try {
            Font font = da != null ? da.getFont() : null;
            if (font != null && !font.isEmpty())
                id = font.getStandard14Font(doc);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return id;
    }

    public static int getSupportFontId(String name) {
        int fontId;
        if (name == null) {
            fontId = Font.e_StdIDCourier;
        } else if (name.equals("Courier")) {
            fontId = Font.e_StdIDCourier;
        } else if (name.equals("Helvetica")) {
            fontId = Font.e_StdIDHelvetica;
        } else if (name.equals("Times")) {
            fontId = Font.e_StdIDTimes;
        } else {
            fontId = Font.e_StdIDCourier;
        }
        return fontId;
    }

    public static boolean renameField(Form form, Control control, String newName) {
        boolean bRet = false;
        try {
            Field field = control.getField();
            int controlCount = field.getControlCount();
            if (controlCount == 1) {
                bRet = form.renameField(field, newName);
            } else {
                bRet = form.moveControl(control, newName);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bRet;
    }
}
