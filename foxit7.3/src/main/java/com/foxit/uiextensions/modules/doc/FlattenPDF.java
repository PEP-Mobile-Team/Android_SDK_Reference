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
package com.foxit.uiextensions.modules.doc;


import com.foxit.sdk.PDFException;
import com.foxit.sdk.addon.xfa.XFADoc;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;

import java.util.concurrent.Callable;

import io.reactivex.Single;

public class FlattenPDF {

    public static Single<Boolean> doFlattenPDFDoc(final PDFDoc doc, final boolean forDisplay, final int options) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doFlattenPDFDocImpl(doc, forDisplay, options);
            }
        });
    }

    public static boolean doFlattenPDFDocImpl(PDFDoc doc, boolean forDisplay, int options) {
        try {
            if (doc == null || doc.isEmpty())
                return false;
            boolean bRet = false;
            int pageCount = doc.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PDFPage page = doc.getPage(i);
                if (page.isEmpty())
                    continue;

                bRet = page.flatten(forDisplay, options);
                if (!bRet)
                    return false;
            }
            return bRet;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Single<Boolean> doFlattenXFADoc(final XFADoc doc, final String outpuPath) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doFlattenXFADocImpl(doc, outpuPath);
            }
        });
    }

    public static boolean doFlattenXFADocImpl(XFADoc doc, String outpuPath) {
        try {
            if (doc == null || doc.isEmpty())
                return false;
            doc.flattenTo(outpuPath);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

}
