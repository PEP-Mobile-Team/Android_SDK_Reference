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
package com.foxit.uiextensions.modules.compare;

import android.content.Context;
import android.os.Environment;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.addon.comparison.Comparison;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.util.concurrent.Callable;

import io.reactivex.Single;

public class ComparisonPDF {
    public static class ComparisonOption {
        public String filePath;
        public byte[] password;
        public int displayColor;
    }

    public static final int COMPARISON_MODE_SIDE_BY_SIDE = 0;
    // unsupported now.
//    public static final int COMPARISON_MODE_OVERLAY = 1;

    private int mComparisonMode = COMPARISON_MODE_SIDE_BY_SIDE;


    public static Single<String> doCompare(final Context context, final PDFDoc oldDoc, final PDFDoc newDoc, final int comparisonFlag) {
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return doCompareImpl(context, oldDoc, newDoc, comparisonFlag);
            }
        });
    }

    public static String doCompareImpl(Context context, PDFDoc oldDoc, PDFDoc newDoc, int comparisonFlag) {
        try {
            Comparison comparison = new Comparison(oldDoc, newDoc);
            PDFDoc pdfDoc = comparison.generateComparedDoc(comparisonFlag);
//            String resultPath = getDefaultComparisonFilePath(context);
            String resultPath = "/mnt/sdcard/FoxitSDK/diff3.pdf";

            pdfDoc.saveAs(resultPath, PDFDoc.e_SaveFlagNormal);
            pdfDoc.delete();
            return resultPath;
        } catch (PDFException e) {
            e.printStackTrace();
            String msg;
            if (e.getLastError() == Constants.e_ErrInvalidLicense) {
                msg = AppResource.getString(context.getApplicationContext(), R.string.rv_invalid_license);
            } else {
                msg = AppResource.getString(context.getApplicationContext(), R.string.rv_unknown_error);
            }
            UIToast.getInstance(context).show(msg);
        }

        return null;
    }

    private static String getDefaultComparisonFilePath(Context context) {
        String fileName = "The result of Comparison.pdf";
        File comparisonFile = new File(getDefaultCachePath(context) + File.separatorChar + fileName);
        if (!comparisonFile.exists()) {
            if (!comparisonFile.getParentFile().exists()) {
                if (!comparisonFile.getParentFile().mkdirs()) return null;
            }
        }
        return AppFileUtil.getFileDuplicateName(comparisonFile.getAbsolutePath());
    }

    private static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    private static String getDefaultCachePath(Context context) {
        return getDiskCachePath(context) + File.separatorChar + "comparison";
    }
}
