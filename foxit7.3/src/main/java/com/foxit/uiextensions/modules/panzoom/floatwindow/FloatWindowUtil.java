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
package com.foxit.uiextensions.modules.panzoom.floatwindow;

import android.content.Context;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FloatWindowUtil {
    private static final FloatWindowUtil ourInstance = new FloatWindowUtil();

    public static FloatWindowUtil getInstance() {
        return ourInstance;
    }

    private FloatWindowUtil() {
    }

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    public void setContext(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public PDFViewCtrl getPdfViewCtrl() {
        return mPdfViewCtrl;
    }

    public void setParent(ViewGroup parent) {
        mParent = parent;
    }

    public ViewGroup getParent() {
        return mParent;
    }

    public static String getSystemProperty(String propertyName) {
        String property;
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("getprop " + propertyName);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")), 1024);
            property = reader.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return property;
    }
}
