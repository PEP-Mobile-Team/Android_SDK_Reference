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
package com.foxit.uiextensions.annots;


import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;

public interface IRedactionEventListener {

    void onAnnotWillApply(PDFPage page, Annot annot);

    void onAnnotApplied(PDFPage page, Annot annot);
}
