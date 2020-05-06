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
package com.foxit.uiextensions.annots.stamp;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.IconProviderCallback;
import com.foxit.sdk.pdf.annots.ShadingColor;
import com.foxit.uiextensions.BuildConfig;

import java.util.HashMap;
import java.util.UUID;

public class DynamicStampIconProvider extends IconProviderCallback {

    HashMap<String, PDFDoc> mDocMap;
    HashMap<PDFDoc, PDFPage>  mDocPagePair;
    private String id = UUID.randomUUID().toString();
    private String version = BuildConfig.VERSION_NAME;//"Version 6.3";
    private int pageIndex = 0;

    private static DynamicStampIconProvider instance;

    public static DynamicStampIconProvider getInstance() {
        if (instance == null) {
            instance = new DynamicStampIconProvider();
        }

        return instance;
    }

    private DynamicStampIconProvider() {
        mDocMap = new HashMap<String, PDFDoc>();
        mDocPagePair = new HashMap<PDFDoc, PDFPage>();
    }

    public void addDocMap(String key, PDFDoc pdfDoc) {
        if (key == null || key.trim().length() < 1) {
            return;
        }

        if (mDocMap.get(key) == null) {
            mDocMap.put(key, pdfDoc);
        }
    }

    public PDFDoc getDoc(String key) {
        if (key == null || key.trim().length() < 1) {
            return null;
        }
        return mDocMap.get(key);
    }

    @Override
    public void release() {
        for (PDFDoc pdfDoc: mDocMap.values()) {
             pdfDoc.delete();
        }

        mDocPagePair.clear();
        mDocMap.clear();
    }

    @Override
    public String getProviderID() {
        return id;
    }

    @Override
    public String getProviderVersion() {
        return version;
    }

    @Override
    public boolean hasIcon(int annotType, String iconName) {
        return Annot.e_Stamp == annotType;
    }

    @Override
    public boolean canChangeColor(int annotType, String iconName) {
        return true;
    }

    @Override
    public PDFPage getIcon(int annotType, String iconName, int color) {
        if (mDocMap == null || mDocMap.get(iconName + annotType) == null) {
            return null;
        }

        if (annotType == Annot.e_Stamp) {
            try {
                PDFDoc pdfDoc = mDocMap.get(iconName + annotType);
                if (pdfDoc == null || pdfDoc.isEmpty()) return null;
                if (mDocPagePair.get(pdfDoc) != null) {
                    return mDocPagePair.get(pdfDoc);
                }
                PDFPage page = pdfDoc.getPage(pageIndex);
                mDocPagePair.put(pdfDoc, page);
                return page;
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public boolean getShadingColor(int annotType, String iconName, int refColor, int shadingIndex, ShadingColor shadingColor) {
        return false;
    }

    @Override
    public float getDisplayWidth(int annotType, String iconName) {
        return 0;
    }

    @Override
    public float getDisplayHeight(int annotType, String iconName) {
        return 0;
    }

}
