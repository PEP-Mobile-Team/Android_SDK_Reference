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
package com.foxit.uiextensions.annots;

import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;

/**
 * interface definition for a callback to be invoked when edit or change annotation.
 */
public interface AnnotEventListener {
    /**
     * Called when a annotation has added in a page
     * @param page The {@link PDFPage} which add a annotation.
     * @param annot The specified annotation has added.
     */
    void onAnnotAdded(PDFPage page, Annot annot);

    /**
     * Called when a annotation will be deleted.
     * @param page The {@link PDFPage} which delete a annotation.
     * @param annot The specified annotation will be deleted.
     */
    void onAnnotWillDelete(PDFPage page, Annot annot);

    /**
     * Called when a annotation has deleted.
     * @param page The {@link PDFPage} which delete a annotation.
     * @param annot The specified annotation has deleted.
     */
    void onAnnotDeleted(PDFPage page, Annot annot);

    /**
     * Called when a annotation has modified.
     * @param page The {@link PDFPage} which modify a annotation.
     * @param annot The specified annotation has modified.
     */
    void onAnnotModified(PDFPage page, Annot annot);

    /**
     * Called when the current annotation has changed.
     * @param lastAnnot the previous annotation.
     * @param currentAnnot the current annotation.
     */
    void onAnnotChanged(Annot lastAnnot, Annot currentAnnot);
}
