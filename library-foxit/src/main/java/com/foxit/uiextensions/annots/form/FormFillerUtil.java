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
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Form;



public class FormFillerUtil {

	protected static int getAnnotFieldType(Form form, Annot annot)
	{
		int type = 0;
		try {
			if (annot == null || annot.isEmpty() || !(annot instanceof Widget)) return type;
			Control control = ((Widget)annot).getControl();
			if(control != null)
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

	public static  boolean isEmojiCharacter(int codePoint) {
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
}
