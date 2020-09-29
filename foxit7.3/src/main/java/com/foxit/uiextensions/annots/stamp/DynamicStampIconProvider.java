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
package com.foxit.uiextensions.annots.stamp;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.IconProviderCallback;
import com.foxit.sdk.pdf.annots.ShadingColor;
import com.foxit.uiextensions.BuildConfig;
import com.foxit.uiextensions.utils.AppFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

public class DynamicStampIconProvider extends IconProviderCallback {

    private Context mContext;
    private HashMap<String, PDFDoc> mDocMap;
    private HashMap<PDFDoc, PDFPage> mDocPagePair;
    private String id = UUID.randomUUID().toString();
    private String version = BuildConfig.VERSION_NAME;//"Version 6.3";
    private int pageIndex = 0;

    private static DynamicStampIconProvider instance;

    public static DynamicStampIconProvider getInstance(Context context) {
        if (instance == null) {
            instance = new DynamicStampIconProvider(context);
        }
        return instance;
    }

    private DynamicStampIconProvider(Context context) {
        mContext = context;
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
        for (PDFDoc pdfDoc : mDocMap.values()) {
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
        if (mDocMap == null)
            return null;

        if (mDocMap.get(iconName + annotType) == null && !addIcon(iconName)) {
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

    public boolean addIcon(String iconName) {
        if (mDocMap.get(iconName + Annot.e_Stamp) == null) {
            FileOutputStream fos = null;
            try {
                int stampType = StampUntil.getStampTypeByName(iconName);

                String stampFileName = null;
                String assetsPath = null;
                if (stampType >= 0 && stampType <= 11) {
                    assetsPath = "StandardStamps/";
                    stampFileName = assetsPath + iconName + ".pdf";

                } else if (stampType >= 12 && stampType <= 16) {
                    assetsPath = "SignHere/";
                    stampFileName = assetsPath + iconName + ".pdf";
                } else if (stampType >= 17 && stampType <= 21) {
                    assetsPath = "DynamicStamps/";
                    stampFileName = assetsPath + iconName.substring(4) + ".pdf";
                }

                if (stampFileName == null) {
                    assetsPath = "StandardStamps/";
                    stampFileName = "StandardStamps/Approved.pdf";
                }

                InputStream is = mContext.getAssets().open(stampFileName);
                byte[] buffer = new byte[1 << 13];
                String path = AppFileUtil.getDiskCachePath(mContext);
                if (TextUtils.isEmpty(path)) {
                    path = Environment.getExternalStorageDirectory().getPath() + "/FoxitSDK/";
                } else if (!path.endsWith("/")) {
                    path += "/";
                }
                String dirPath = path + assetsPath;
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                path += stampFileName;
                File file = new File(path);
                fos = new FileOutputStream(file);
                int n = 0;
                while (-1 != (n = is.read(buffer))) {
                    fos.write(buffer, 0, n);
                }

                PDFDoc pdfDoc = new PDFDoc(path);
                pdfDoc.load(null);
                mDocMap.put(iconName + Annot.e_Stamp, pdfDoc);
                is.close();
                return true;
            } catch (PDFException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public boolean getShadingColor(int annotType, String iconName, int refColor,
                                   int shadingIndex, ShadingColor shadingColor) {
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
