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


import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.annots.AnnotContent;

import java.util.ArrayList;

public interface IFormFillerContent extends AnnotContent {
    public String getFieldValue();

    public String getFieldName();

    public int getFieldType();

    public int getFieldFlags();

    public int getFontId();

    public int getFontColor();

    public float getFontSize();

    public int getMKRotation();

    public Boolean isChecked();

    public int getCheckedIndex();

    public Annot getAnnot();

    public ArrayList<ChoiceItemInfo> getOptions();

}
